package com.yupi.yuaicodemother.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.yuaicodemother.langgraph4j.model.ImageResource;
import com.yupi.yuaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片收集工具（插画图片）
 * todo https://pixabay.com/api/docs/#api_search_images 使用这个代替
 */
@Slf4j
@Component
public class UndrawIllustrationTool {

    private static final String PIXABAY_API_URL = "https://pixabay.com/api/";
    private static final String API_KEY = "你的密钥";

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> searchIllustrations(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        // 构建 Pixabay API请求URL
        String apiUrl = String.format("%s?key=%s&q=%s&image_type=illustration&per_page=%d",
                PIXABAY_API_URL, API_KEY, query, searchCount);
        // 使用 try-with-resources 自动释放 HTTP 资源
        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                log.error("Pixabay API请求失败，状态码: {}", response.getStatus());
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray hits = result.getJSONArray("hits");
            if (hits == null || hits.isEmpty()) {
                log.info("未找到与'{}'相关的插画", query);
                return imageList;
            }

            int actualCount = Math.min(searchCount, hits.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject imageData = hits.getJSONObject(i);
                String tags = imageData.getStr("tags", "插画");
                String webformatURL = imageData.getStr("webformatURL", "");
                String largeImageURL = imageData.getStr("largeImageURL", "");
                // 优先使用webformatURL，如果没有则使用largeImageURL
                String imageUrl = StrUtil.isNotBlank(webformatURL) ? webformatURL : largeImageURL;
                if (StrUtil.isNotBlank(imageUrl)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(tags)
                            .url(imageUrl)
                            .build());
                }
            }
            log.info("成功搜索到 {} 张插画图片", imageList.size());
        } catch (Exception e) {
            log.error("搜索插画失败：{}", e.getMessage(), e);
        }
        return imageList;
    }
}