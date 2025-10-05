package com.yupi.yuaicodemother.request;

import dev.langchain4j.http.client.HttpClientBuilder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * 可中断的JDK HTTP客户端构建器
 * 用于构建支持中断功能的HTTP客户端
 * 
 * @author leikooo
 */
@Getter
@Accessors(fluent = true)
public class InterruptibleJdkHttpClientBuilder implements HttpClientBuilder {

    /**
     * HTTP客户端构建器的提供者
     */
    private Supplier<HttpClient.Builder> httpClientBuilder = HttpClient::newBuilder;

    /**
     * 连接超时时间
     */
    private Duration connectTimeout;

    /**
     * 读取超时时间
     */
    private Duration readTimeout;

    /**
     * 创建构建器实例
     */
    public static InterruptibleJdkHttpClientBuilder builder() {
        return new InterruptibleJdkHttpClientBuilder();
    }

    /**
     * 设置HTTP客户端构建器
     * 
     * @param httpClientBuilder HTTP客户端构建器
     * @return 当前构建器实例
     */
    public InterruptibleJdkHttpClientBuilder httpClientBuilder(HttpClient.Builder httpClientBuilder) {
        this.httpClientBuilder = () -> httpClientBuilder;
        return this;
    }

    /**
     * 设置HTTP客户端构建器提供者
     * 
     * @param httpClientBuilderSupplier HTTP客户端构建器提供者
     * @return 当前构建器实例
     */
    public InterruptibleJdkHttpClientBuilder httpClientBuilder(Supplier<HttpClient.Builder> httpClientBuilderSupplier) {
        this.httpClientBuilder = httpClientBuilderSupplier;
        return this;
    }

    /**
     * 设置连接超时时间
     * 
     * @param connectTimeout 连接超时时间
     * @return 当前构建器实例
     */
    @Override
    public InterruptibleJdkHttpClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 设置连接超时时间（秒）
     * 
     * @param seconds 超时秒数
     * @return 当前构建器实例
     */
    public InterruptibleJdkHttpClientBuilder connectTimeout(long seconds) {
        this.connectTimeout = Duration.ofSeconds(seconds);
        return this;
    }

    /**
     * 设置读取超时时间
     * 
     * @param readTimeout 读取超时时间
     * @return 当前构建器实例
     */
    @Override
    public InterruptibleJdkHttpClientBuilder readTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * 设置读取超时时间（秒）
     * 
     * @param seconds 超时秒数
     * @return 当前构建器实例
     */
    public InterruptibleJdkHttpClientBuilder readTimeout(long seconds) {
        this.readTimeout = Duration.ofSeconds(seconds);
        return this;
    }

    /**
     * 构建可中断的HTTP客户端
     *
     * @return 可中断的HTTP客户端实例
     */
    @Override
    public dev.langchain4j.http.client.HttpClient build() {
        return new InterruptibleJdkHttpClient(this);
    }

    /**
     * 获取HTTP客户端（别名方法，与build()等效）
     *
     * @return 可中断的HTTP客户端实例
     */
    public dev.langchain4j.http.client.HttpClient getClient() {
        return build();
    }
}