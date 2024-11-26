package fansirsqi.xposed.sesame.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fansirsqi.xposed.sesame.model.ModelConfig;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.model.ModelFields;
import lombok.Data;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.entity.UserEntity;
import fansirsqi.xposed.sesame.util.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置类，负责加载、保存、管理应用的配置数据。
 */
@Data
public class Config {

    private static final String TAG = Config.class.getSimpleName();

    // 单例实例
    public static final Config INSTANCE = new Config();

    // 是否初始化标志
    @JsonIgnore
    private boolean init;

    // 存储模型字段的映射
    private final Map<String, ModelFields> modelFieldsMap = new ConcurrentHashMap<>();

    /**
     * 设置新的模型字段配置
     *
     * @param newModels 新的模型字段映射
     */
    public void setModelFieldsMap(Map<String, ModelFields> newModels) {
        modelFieldsMap.clear();
        Map<String, ModelConfig> modelConfigMap = ModelTask.getModelConfigMap();

        // 如果传入的 newModels 为 null，初始化为空
        if (newModels == null) {
            newModels = new HashMap<>();
        }

        // 遍历所有模型配置，合并字段配置
        for (ModelConfig modelConfig : modelConfigMap.values()) {
            String modelCode = modelConfig.getCode();
            ModelFields newModelFields = new ModelFields();
            ModelFields configModelFields = modelConfig.getFields();
            ModelFields modelFields = newModels.get(modelCode);

            if (modelFields != null) {
                // 如果已有模型字段，则按值覆盖配置
                for (ModelField<?> configModelField : configModelFields.values()) {
                    ModelField<?> modelField = modelFields.get(configModelField.getCode());
                    try {
                        if (modelField != null) {
                            Object value = modelField.getValue();
                            if (value != null) {
                                configModelField.setObjectValue(value);
                            }
                        }
                    } catch (Exception e) {
                        LogUtil.printStackTrace(e);
                    }
                    newModelFields.addField(configModelField);
                }
            } else {
                // 如果没有找到对应的模型字段，则直接添加配置字段
                for (ModelField<?> configModelField : configModelFields.values()) {
                    newModelFields.addField(configModelField);
                }
            }
            modelFieldsMap.put(modelCode, newModelFields);
        }
    }

    /**
     * 检查是否存在指定的模型字段
     *
     * @param modelCode 模型代码
     * @return 是否存在该模型字段
     */
    public Boolean hasModelFields(String modelCode) {
        return modelFieldsMap.containsKey(modelCode);
    }

    /**
     * 检查指定模型字段是否存在
     *
     * @param modelCode 模型代码
     * @param fieldCode 字段代码
     * @return 是否存在该字段
     */
    public Boolean hasModelField(String modelCode, String fieldCode) {
        ModelFields modelFields = modelFieldsMap.get(modelCode);
        if (modelFields == null) {
            return false;
        }
        return modelFields.containsKey(fieldCode);
    }

    /**
     * 判断配置文件是否已修改
     *
     * @param userId 用户 ID
     * @return 是否已修改
     */
    public static Boolean isModify(String userId) {
        String json = null;
        File configV2File;

        if (StringUtil.isEmpty(userId)) {
            configV2File = FileUtil.getDefaultConfigV2File();
        } else {
            configV2File = FileUtil.getConfigV2File(userId);
        }

        if (configV2File.exists()) {
            json = FileUtil.readFromFile(configV2File);
        }

        if (json != null) {
            String formatted = toSaveStr();
            return formatted == null || !formatted.equals(json);
        }
        return true;
    }

    /**
     * 保存配置文件
     *
     * @param userId 用户 ID
     * @param force 是否强制保存
     * @return 保存是否成功
     */
    public static Boolean save(String userId, Boolean force) {
        if (!force) {
            if (!isModify(userId)) {
                return true;
            }
        }
        String json = toSaveStr();
        boolean success;

        if (StringUtil.isEmpty(userId)) {
            userId = "默认";
            success = FileUtil.setDefaultConfigV2File(json);
        } else {
            success = FileUtil.setConfigV2File(userId, json);
        }

        LogUtil.record("保存配置: " + userId);
        return success;
    }

    /**
     * 加载配置文件
     *
     * @param userId 用户 ID
     * @return 加载后的 Config 实例
     */
    public static synchronized Config load(String userId) {
        LogUtil.runtime(TAG, "开始加载配置");
        String userName = "";
        File configV2File = null;
        try {
            if (StringUtil.isEmpty(userId)) {
                configV2File = FileUtil.getDefaultConfigV2File();
                userName = "默认";
            } else {
                configV2File = FileUtil.getConfigV2File(userId);
                UserEntity userEntity = UserIdMapUtil.get(userId);
                if (userEntity == null) {
                    userName = userId;
                } else {
                    userName = userEntity.getShowName();
                }
            }
            LogUtil.record("加载配置: " + userName);

            // 如果配置文件存在，加载内容
            if (configV2File.exists()) {
                String json = FileUtil.readFromFile(configV2File);
                JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json);
                String formatted = toSaveStr();

                if (formatted != null && !formatted.equals(json)) {
                    LogUtil.runtime(TAG, "格式化配置: " + userName);
                    LogUtil.system(TAG, "格式化配置: " + userName);
                    FileUtil.write2File(formatted, configV2File);
                }
            } else {
                // 如果配置文件不存在，复制默认配置或初始化
                File defaultConfigV2File = FileUtil.getDefaultConfigV2File();
                if (defaultConfigV2File.exists()) {
                    String json = FileUtil.readFromFile(defaultConfigV2File);
                    JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json);
                    LogUtil.runtime(TAG, "复制新配置: " + userName);
                    LogUtil.system(TAG, "复制新配置: " + userName);
                    FileUtil.write2File(json, configV2File);
                } else {
                    unload();
                    LogUtil.runtime(TAG, "初始新配置: " + userName);
                    LogUtil.system(TAG, "初始新配置: " + userName);
                    FileUtil.write2File(toSaveStr(), configV2File);
                }
            }
        } catch (Throwable t) {
            LogUtil.printStackTrace(TAG, t);
            LogUtil.runtime(TAG, "重置配置: " + userName);
            LogUtil.system(TAG, "重置配置: " + userName);
            try {
                unload();
                if (configV2File != null) {
                    FileUtil.write2File(toSaveStr(), configV2File);
                }
            } catch (Exception e) {
                LogUtil.printStackTrace(TAG, t);
            }
        }
        INSTANCE.setInit(true);
        LogUtil.runtime(TAG, "加载配置结束");
        return INSTANCE;
    }

    /**
     * 卸载当前配置
     */
    public static synchronized void unload() {
        for (ModelFields modelFields : INSTANCE.modelFieldsMap.values()) {
            for (ModelField<?> modelField : modelFields.values()) {
                if (modelField != null) {
                    modelField.reset();
                }
            }
        }
    }

    /**
     * 将配置对象转换为 JSON 格式字符串
     *
     * @return 配置的 JSON 字符串
     */
    public static String toSaveStr() {
        return JsonUtil.toFormatJsonString(INSTANCE);
    }
}
