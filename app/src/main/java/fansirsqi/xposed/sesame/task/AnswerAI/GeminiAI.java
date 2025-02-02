package fansirsqi.xposed.sesame.task.AnswerAI;

import static fansirsqi.xposed.sesame.util.JsonUtil.getValueByPath;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import fansirsqi.xposed.sesame.util.Log;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GenAI帮助类，用于与GenAI接口交互以获取AI回答
 * 支持单条文本问题及带有候选答案列表的问题请求
 */
public class GeminiAI implements AnswerAIInterface {
    private final String TAG = GeminiAI.class.getSimpleName();

    private static final String BASE_URL = "https://api.genai.gd.edu.kg/google";
    private static final String MODEL_ENDPOINT = "/v1beta/models/gemini-1.5-flash:generateContent";
    private static final String CONTENT_TYPE = "application/json";
    private static final String JSON_PATH = "candidates.[0].content.parts.[0].text";
    private static final String PREFIX = "只回答答案 ";

    private final String token;

    // 私有构造函数，防止外部实例化
    public GeminiAI(String token) {
        this.token = token != null && !token.isEmpty() ? token : "";
    }

    /**
     * 构建请求体
     *
     * @param text 问题内容
     * @return 请求体的JSON字符串
     */
    private String buildRequestBody(String text) {
        return "{\n" +
                "    \"contents\": [\n" +
                "        {\n" +
                "            \"parts\": [\n" +
                "                {\n" +
                "                    \"text\": \"" + PREFIX + text + "\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }

    /**
     * 构建请求URL
     *
     * @return 完整的请求URL
     */
    private String buildRequestUrl() {
        return BASE_URL + MODEL_ENDPOINT + "?key=" + token;
    }

    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果
     */
    @Override
    public String getAnswerStr(String text) {
        String result = "";
        OkHttpClient client = new OkHttpClient();
        String content = buildRequestBody(text);
        MediaType mediaType = MediaType.parse(CONTENT_TYPE);
        RequestBody body = RequestBody.create(content, mediaType);
        String url = buildRequestUrl();
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", CONTENT_TYPE)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return result;
            }
            String json = response.body().string();
            if (!response.isSuccessful()) {
                Log.other("Gemini请求失败");
                Log.runtime("Gemini接口异常：" + json);
                // 可能key出错了
                return result;
            }
            JSONObject jsonObject = new JSONObject(json);
            result = getValueByPath(jsonObject, JSON_PATH);
        } catch (IOException | org.json.JSONException e) {
            Log.printStackTrace(TAG, e);
        }
        return result;
    }

    /**
     * 获取答案
     *
     * @param title      问题
     * @param answerList 答案集合
     * @return 空没有获取到
     */
    @Override
    public Integer getAnswer(String title, List<String> answerList) {
        StringBuilder answerStr = new StringBuilder();
        for (String answer : answerList) {
            answerStr.append("[").append(answer).append("]");
        }
        String answerResult = getAnswerStr(title + "\n" + answerStr);
        if (answerResult != null && !answerResult.isEmpty()) {
            for (int i = 0; i < answerList.size(); i++) {
                if (answerResult.contains(answerList.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }
}