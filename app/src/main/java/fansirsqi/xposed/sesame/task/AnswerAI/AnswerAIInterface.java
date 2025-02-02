package fansirsqi.xposed.sesame.task.AnswerAI;
import java.util.List;
public interface AnswerAIInterface {
    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果
     */
    String getAnswerStr(String text);

    /**
     * 获取AI答案
     */
    Integer getAnswer(String title, List<String> answerList);

    // 提供一个静态方法用于获取单例实例
    static AnswerAIInterface getInstance() {
        return SingletonHolder.INSTANCE;
    }

    static class SingletonHolder {
        private static final AnswerAIInterface INSTANCE = new AnswerAIInterface() {
            @Override
            public String getAnswerStr(String text) {
                return "";
            }

            @Override
            public Integer getAnswer(String title, List<String> answerList) {
                return -1;
            }
        };
    }
}
