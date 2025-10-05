package com.yupi.yuaicodemother.request;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yuaicodemother.constant.RequestConstant;
import com.yupi.yuaicodemother.utils.ThreadLocalUtil;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEvent;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static dev.langchain4j.http.client.sse.ServerSentEventListenerUtils.ignoringExceptions;
import static java.util.stream.Collectors.joining;

@Slf4j
public class InterruptibleJdkHttpClient implements HttpClient {

    private final java.net.http.HttpClient delegate;
    private final Duration readTimeout;

    /**
     * 存储活跃的HTTP请求，key是 appId ，value是CompletableFuture
     */
    private static final Map<String, CompletableFuture<?>> ACTIVE_REQUESTS = new ConcurrentHashMap<>();

    public static Cache<String, Byte> INTERCEPTOR_IDS = Caffeine.newBuilder()
            .maximumSize(40)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    public InterruptibleJdkHttpClient(InterruptibleJdkHttpClientBuilder builder) {
        java.net.http.HttpClient.Builder httpClientBuilder =
                getOrDefault(builder.httpClientBuilder(), java.net.http.HttpClient::newBuilder);
        if (builder.connectTimeout() != null) {
            httpClientBuilder.connectTimeout(builder.connectTimeout());
        }
        this.delegate = httpClientBuilder.build();
        this.readTimeout = builder.readTimeout();
    }

    private java.net.http.HttpClient.Builder getOrDefault(Supplier<java.net.http.HttpClient.Builder> builderSupplier,
                                                          Supplier<java.net.http.HttpClient.Builder> defaultBuilder) {
        if (builderSupplier == null) {
            return defaultBuilder.get();
        }
        return builderSupplier.get();
    }

    public static InterruptibleJdkHttpClientBuilder builder() {
        return new InterruptibleJdkHttpClientBuilder();
    }

    @Override
    public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException {
        try {
            java.net.http.HttpRequest jdkRequest = toJdkRequest(request);

            java.net.http.HttpResponse<String> jdkResponse = delegate.send(jdkRequest, HttpResponse.BodyHandlers.ofString());

            if (!isSuccessful(jdkResponse)) {
                throw new HttpException(jdkResponse.statusCode(), jdkResponse.body());
            }

            return fromJdkResponse(jdkResponse, jdkResponse.body());
        } catch (HttpTimeoutException e) {
            throw new TimeoutException(e);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
        String appId = ThreadLocalUtil.getAndRemove(RequestConstant.LOCAL_THREAD_APP_ID);
        java.net.http.HttpRequest jdkRequest = toJdkRequest(request);
        CompletableFuture<Void> future = delegate.sendAsync(jdkRequest, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(jdkResponse -> {
                    // 在处理响应前检查是否被中断
                    if (isRequestInterrupted(appId)) {
                        log.info("HTTP响应处理前检测到中断: appId={}", appId);
                        return;
                    }
                    if (!isSuccessful(jdkResponse)) {
                        HttpException exception = new HttpException(jdkResponse.statusCode(), readBody(jdkResponse));
                        ignoringExceptions(() -> listener.onError(exception));
                        return;
                    }
                    SuccessfulHttpResponse response = fromJdkResponse(jdkResponse, null);
                    ignoringExceptions(() -> listener.onOpen(response));
                    try (InputStream inputStream = jdkResponse.body()) {
                        // 使用包装的listener进行流解析，支持中断检查
                        parseWithInterruptCheck(inputStream, parser, listener, appId);
                        listener.onClose();
                    } catch (Exception e) {
                        if (isRequestInterrupted(appId)) {
                            log.info("HTTP流解析被中断报错信息 {}", ExceptionUtils.getRootCauseMessage(e));
                        }
                        log.info("HTTP流解析被中断: appId={}", appId);
                    }
                })
                .exceptionally(throwable -> {
                    if (throwable instanceof CancellationException) {
                        log.info("HTTP请求被取消: appId={}", appId);
                    } else if (throwable.getCause() instanceof HttpTimeoutException) {
                        ignoringExceptions(() -> listener.onError(new TimeoutException(throwable)));
                    } else {
                        ignoringExceptions(() -> listener.onError(throwable));
                    }
                    return null;
                })
                .whenComplete((result, throwable) -> {
                    cleanupRequest(appId);
                });
        ACTIVE_REQUESTS.put(appId, future);
    }


    /**
     * 清理请求相关的所有缓存数据
     * 这是一个原子操作，确保多线程安全
     */
    private void cleanupRequest(String appId) {
        if (StrUtil.isBlank(appId)) {
            return;
        }
        ACTIVE_REQUESTS.remove(appId);
        log.debug("HTTP请求完成，清理所有缓存: appId={}", appId);
    }

    private java.net.http.HttpRequest toJdkRequest(HttpRequest request) {
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(request.url()));

        request.headers().forEach((name, values) -> {
            if (values != null) {
                values.forEach(value -> builder.header(name, value));
            }
        });

        java.net.http.HttpRequest.BodyPublisher bodyPublisher;
        if (request.body() != null) {
            bodyPublisher = java.net.http.HttpRequest.BodyPublishers.ofString(request.body());
        } else {
            bodyPublisher = java.net.http.HttpRequest.BodyPublishers.noBody();
        }
        builder.method(request.method().name(), bodyPublisher);

        if (readTimeout != null) {
            builder.timeout(readTimeout);
        }

        return builder.build();
    }

    private static SuccessfulHttpResponse fromJdkResponse(java.net.http.HttpResponse<?> response, String body) {
        return SuccessfulHttpResponse.builder()
                .statusCode(response.statusCode())
                .headers(response.headers().map())
                .body(body)
                .build();
    }

    private static boolean isSuccessful(java.net.http.HttpResponse<?> response) {
        int statusCode = response.statusCode();
        return statusCode >= 200 && statusCode < 300;
    }

    private static String readBody(java.net.http.HttpResponse<InputStream> response) {
        try (InputStream inputStream = response.body();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(joining(System.lineSeparator()));
        } catch (IOException e) {
            return "Cannot read error response body: " + e.getMessage();
        }
    }

    /**
     * 带中断检查的流解析
     */
    private void parseWithInterruptCheck(InputStream inputStream, ServerSentEventParser parser,
                                         ServerSentEventListener listener, String appId) {
        // 包装listener，在每次事件处理前检查中断状态
        ServerSentEventListener wrappedListener = new ServerSentEventListener() {
            @Override
            public void onOpen(SuccessfulHttpResponse response) {
                listener.onOpen(response);
            }

            @Override
            public void onEvent(ServerSentEvent event) {
                if (!isRequestInterrupted(appId)) {
                    listener.onEvent(event);
                } else {
                    log.debug("检测到中断，直接取消HTTP请求: appId={}", appId);
                    cancelCurrentRequest(appId);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isRequestInterrupted(appId)) {
                    listener.onError(throwable);
                }
            }

            @Override
            public void onClose() {
                if (!isRequestInterrupted(appId)) {
                    listener.onClose();
                }
            }
        };

        parser.parse(inputStream, wrappedListener);
    }

    /**
     * 检查请求是否被中断
     *
     * @param appId 会话ID
     * @return true表示应该中断，false表示继续处理
     */
    private boolean isRequestInterrupted(String appId) {
        if (StrUtil.isBlank(appId)) {
            return false;
        }
        return INTERCEPTOR_IDS.asMap().containsKey(appId);
    }

    /**
     * 取消当前HTTP请求
     * 这会同时清理ONGOING_GENERATES，标记会话已结束
     *
     * @param appId 会话ID
     */
    private void cancelCurrentRequest(String appId) {
        if (StrUtil.isBlank(appId)) {
            return;
        }

        CompletableFuture<?> future = ACTIVE_REQUESTS.get(appId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                cleanupRequest(appId);
                log.info("成功取消HTTP请求: appId={}", appId);
            } else {
                log.debug("请求已完成，无法取消: appId={}", appId);
            }
        } else {
            // 即使没有active的future，也要清理ONGOING_GENERATES
            cleanupRequest(appId);
            log.debug("未找到活跃的HTTP请求但已清理缓存: appId={}", appId);
        }
    }
}