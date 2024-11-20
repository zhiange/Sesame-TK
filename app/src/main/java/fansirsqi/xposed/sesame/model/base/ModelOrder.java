package fansirsqi.xposed.sesame.model.base;

import fansirsqi.xposed.sesame.data.Model;
import fansirsqi.xposed.sesame.model.normal.answerAI.AnswerAI;
import fansirsqi.xposed.sesame.model.normal.base.BaseModel;
import fansirsqi.xposed.sesame.model.task.ancientTree.AncientTree;
import fansirsqi.xposed.sesame.model.task.antCooperate.AntCooperate;
import fansirsqi.xposed.sesame.model.task.antFarm.AntFarm;
import fansirsqi.xposed.sesame.model.task.antForest.AntForestV2;
import fansirsqi.xposed.sesame.model.task.antMember.AntMember;
import fansirsqi.xposed.sesame.model.task.antOcean.AntOcean;
import fansirsqi.xposed.sesame.model.task.antOrchard.AntOrchard;
import fansirsqi.xposed.sesame.model.task.antSports.AntSports;
import fansirsqi.xposed.sesame.model.task.antStall.AntStall;
import fansirsqi.xposed.sesame.model.task.greenFinance.GreenFinance;
import fansirsqi.xposed.sesame.model.task.reserve.Reserve;
import fansirsqi.xposed.sesame.model.task.antDodo.AntDodo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModelOrder {
    @SuppressWarnings("unchecked")
    private static final Class<Model>[] array = new Class[]{
            BaseModel.class
            , AntForestV2.class
            , AntFarm.class
            , AntStall.class
            , AntOrchard.class
            , Reserve.class
            , AntDodo.class
            , AntOcean.class
            , AntCooperate.class
            , AncientTree.class
            , AntSports.class
            , AntMember.class
            , GreenFinance.class
            , AnswerAI.class
    };

    private static final List<Class<Model>> readOnlyClazzList = Collections.unmodifiableList(Arrays.asList(array));

    public static List<Class<Model>> getClazzList() {
        return readOnlyClazzList;
    }

}