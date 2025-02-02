package fansirsqi.xposed.sesame.task.AnswerAI;

import java.util.List;

import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.util.Log;

public class AnswerAI extends Model {
    private static final String TAG = AnswerAI.class.getSimpleName();
    private static final String AI_LOG_PREFIX = "AI🧠答题，问题：[";
    private static final String NORMAL_LOG_PREFIX = "开始答题，问题：[";
    private static final String QUESTION_LOG_FORMAT = "题目[%s]#选项:\n%s";
    private static final String AI_ANSWER_LOG_FORMAT = "AI回答🧠[%s]";
    private static final String NORMAL_ANSWER_LOG_FORMAT = "普通回答🤖[%s]";

    private static Boolean enable = false;

    @Override
    public String getName() {
        return "AI答题";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.OTHER;
    }

    @Override
    public String getIcon() {
        return "AnswerAI.svg";
    }

    private static AnswerAIInterface answerAIInterface = AnswerAIInterface.getInstance();

    public interface AIType {
        int TONGYI = 0;
        int GEMINI = 1;
        int DEEPSEEK = 2;

        String[] nickNames = {
                "通义千问",
                "Gemini",
                "DeepSeek"
        };
    }

    private final ChoiceModelField aiType = new ChoiceModelField("useGeminiAI", "AI类型", AIType.TONGYI, AIType.nickNames);
    private final StringModelField tongYiToken = new StringModelField("tongYiToken", "qwen-turbo | 设置令牌", "");
    private final StringModelField GeminiToken = new StringModelField("GeminiAIToken", "gemini-1.5-flash | 设置令牌", "");
    private final StringModelField DeepSeekToken = new StringModelField("DeepSeekToken", "DeepSeek-R1 | 设置令牌", "");

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(aiType);
        modelFields.addField(tongYiToken);
        modelFields.addField(GeminiToken);
        modelFields.addField(DeepSeekToken);
        return modelFields;
    }

    @Override
    public void boot(ClassLoader classLoader) {
        enable = getEnableField().getValue();
        switch (aiType.getValue()) {
            case AIType.TONGYI:
                answerAIInterface = new TongyiAI(tongYiToken.getValue());
                break;
            case AIType.GEMINI:
                answerAIInterface = new GeminiAI(GeminiToken.getValue());
                break;
            case AIType.DEEPSEEK:
                answerAIInterface = new DeepSeek(DeepSeekToken.getValue());
                break;
            default:
                answerAIInterface = AnswerAIInterface.getInstance();
                break;
        }
    }

    // 封装日志记录方法
    private static void logQuestion(String text) {
        String logPrefix = enable ? AI_LOG_PREFIX : NORMAL_LOG_PREFIX;
        Log.record(logPrefix + text + "]");
    }

    // 封装AI回答日志记录方法
    private static void logAIAnswer(String answer) {
        Log.record(String.format(AI_ANSWER_LOG_FORMAT, answer));
    }

    // 封装普通回答日志记录方法
    private static void logNormalAnswer(String answer) {
        Log.record(String.format(NORMAL_ANSWER_LOG_FORMAT, answer));
    }

    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果
     */
    public static String getAnswer(String text) {
        try {
            logQuestion(text);
            if (enable) {
                return answerAIInterface.getAnswerStr(text);
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, t);
        }
        return "";
    }

    public static String getAnswer(String text, List<String> answerList) {
        String answerStr = "";
        try {
            Log.record(String.format(QUESTION_LOG_FORMAT, text, answerList));
            if (enable) {
                Integer answer = answerAIInterface.getAnswer(text, answerList);
                if (answer != null && answer >= 0 && answer < answerList.size()) {
                    answerStr = answerList.get(answer);
                    logAIAnswer(answerStr);
                }
            } else {
                if (!answerList.isEmpty()) {
                    answerStr = answerList.get(0);
                    logNormalAnswer(answerStr);
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, t);
        }
        return answerStr;
    }
}