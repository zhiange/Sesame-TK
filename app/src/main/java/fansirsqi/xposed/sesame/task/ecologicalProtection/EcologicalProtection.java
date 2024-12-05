package fansirsqi.xposed.sesame.task.ecologicalProtection;

import java.util.HashSet;
import java.util.LinkedHashMap;

import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.entity.AlipayAnimal;
import fansirsqi.xposed.sesame.entity.AlipayBeach;
import fansirsqi.xposed.sesame.entity.AlipayMarathon;
import fansirsqi.xposed.sesame.entity.AlipayNewAncientTree;
import fansirsqi.xposed.sesame.entity.AlipayReserve;
import fansirsqi.xposed.sesame.entity.AlipayTree;
import fansirsqi.xposed.sesame.entity.CooperateUser;
import fansirsqi.xposed.sesame.model.base.TaskCommon;
import fansirsqi.xposed.sesame.util.idMap.*;

public class EcologicalProtection extends ModelTask {
    private static final String TAG = EcologicalProtection.class.getSimpleName();

    @Override
    public String getName() {
        return "生态保护";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    private static BooleanModelField cooperateWater;
    private static SelectAndCountModelField cooperateWaterList;
    private static SelectAndCountModelField cooperateWaterTotalLimitList;
    private static ChoiceModelField protectMarathonType;
    private static SelectAndCountModelField protectMarathonList;
    private static ChoiceModelField protectNewAncientTreeType;
    private static SelectAndCountModelField protectNewAncientTreeList;
    private static BooleanModelField protectTree;
    private static SelectAndCountModelField protectTreeList;
    private static BooleanModelField protectReserve;
    private static SelectAndCountModelField protectReserveList;
    private static BooleanModelField protectBeach;
    private static SelectAndCountModelField protectBeachList;
    private static BooleanModelField protectAnimal;
    private static SelectModelField protectAnimalList;

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(cooperateWater = new BooleanModelField("cooperateWater", "合种 | 浇水", false));
        modelFields.addField(cooperateWaterList = new SelectAndCountModelField("cooperateWaterList", "合种 | 日浇水量列表", new LinkedHashMap<>(), CooperateUser::getList));
        modelFields.addField(cooperateWaterTotalLimitList = new SelectAndCountModelField("cooperateWaterTotalLimitList", "合种 | 总浇水量列表", new LinkedHashMap<>(), CooperateUser::getList));


        modelFields.addField(protectMarathonType = new ChoiceModelField("protectMarathonType", "碳中和 | 马拉松", ProtectType.NONE, ProtectType.nickNames));
        modelFields.addField(protectMarathonList = new SelectAndCountModelField("protectMarathonList", "碳中和 | 马拉松列表", new LinkedHashMap<>(), AlipayMarathon::getList));
        modelFields.addField(protectNewAncientTreeType = new ChoiceModelField("protectNewAncientTreeType", "碳中和 | 古树医生", ProtectType.NONE, ProtectType.nickNames));
        modelFields.addField(protectNewAncientTreeList = new SelectAndCountModelField("protectNewAncientTreeList", "碳中和 | 古树医生列表", new LinkedHashMap<>(), AlipayNewAncientTree::getList));
        modelFields.addField(protectTree = new BooleanModelField("protectTree", "保护森林 | 植树(总数)", false));
        modelFields.addField(protectTreeList = new SelectAndCountModelField("protectTreeList", "保护森林 | 植树列表", new LinkedHashMap<>(), AlipayTree::getList));
        modelFields.addField(protectReserve = new BooleanModelField("protectReserve", "保护动物 | 保护地(每天)", false));
        modelFields.addField(protectReserveList = new SelectAndCountModelField("reserveList", "保护动物 | 保护地列表", new LinkedHashMap<>(), AlipayReserve::getList));
        modelFields.addField(protectAnimal = new BooleanModelField("protectAnimal", "保护动物 | 护林员", false));
        modelFields.addField(protectAnimalList = new SelectModelField("protectAnimalList", "保护动物 | 护林员列表", new HashSet<>(), AlipayAnimal::getList));
        modelFields.addField(protectBeach = new BooleanModelField("protectBeach", "保护海洋 | 海滩(总数)", false));
        modelFields.addField(protectBeachList = new SelectAndCountModelField("protectOceanList", "保护海洋 | 海滩列表", new LinkedHashMap<>(), AlipayBeach::getList));
        return modelFields;
    }

    @Override
    public Boolean check() {
        return !TaskCommon.IS_ENERGY_TIME;
    }

    @Override
    public void run() {
        if (cooperateWater.getValue()) {
            cooperateWater();
        }
        if (protectMarathonType.getValue() != ProtectType.NONE
                || protectNewAncientTreeType.getValue() != ProtectType.NONE) {
            protectCarbon();
        }
        if (protectTree.getValue()) {
            protectTree();
        }
        if (protectReserve.getValue()) {
            protectReserve();
        }
        if (protectAnimal.getValue()) {
            protectAnimal();
        }
        if (protectBeach.getValue()) {
            protectBeach();
        }
    }

    FunctionFactory
}

