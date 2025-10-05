package com.yupi.yuaicodemother.request;

import com.yupi.yuaicodemother.constant.RequestConstant;
import com.yupi.yuaicodemother.utils.ThreadLocalUtil;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.springframework.stereotype.Component;

import static com.yupi.yuaicodemother.constant.RequestConstant.APP_ID;

/**
 * AI 模型监听器
 */
@Component
public class RequestMonitorListener implements ChatModelListener {

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        String conversationId = ThreadLocalUtil.get(RequestConstant.LOCAL_THREAD_APP_ID);
        requestContext.attributes().put(APP_ID, conversationId);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        String conversationId = (String) responseContext.attributes().get(APP_ID);
        ThreadLocalUtil.set(RequestConstant.LOCAL_THREAD_APP_ID, conversationId);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        String conversationId = (String) errorContext.attributes().get(APP_ID);
        ThreadLocalUtil.set(RequestConstant.LOCAL_THREAD_APP_ID, conversationId);
    }

}
