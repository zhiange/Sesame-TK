package tkaxv7s.xposed.sesame.data;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 运行状态枚举类
 */
@Getter
public enum RunType {

    DISABLE(0, "已关闭"),
    MODEL(1, "已激活"),
    PACKAGE(2, "已加载");

    private final Integer code;
    private final String name;

    // 不需要额外的静态块，直接在枚举中初始化MAP
    private static final Map<Integer, RunType> CODE_MAP = new HashMap<>();

    // 静态初始化块，用于填充CODE_MAP
    static {
        for (RunType runType : RunType.values()) {
            CODE_MAP.put(runType.code, runType);
        }
    }

    RunType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static RunType getByCode(Integer code) {
        return CODE_MAP.get(code);
    }
}
