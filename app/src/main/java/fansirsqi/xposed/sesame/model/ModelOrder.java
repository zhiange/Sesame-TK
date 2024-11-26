package fansirsqi.xposed.sesame.model;

import fansirsqi.xposed.sesame.task.AnswerAI.AnswerAI;
import fansirsqi.xposed.sesame.task.ancientTree.AncientTree;
import fansirsqi.xposed.sesame.task.antCooperate.AntCooperate;
import fansirsqi.xposed.sesame.task.antFarm.AntFarm;
import fansirsqi.xposed.sesame.task.antForest.AntForest;
import fansirsqi.xposed.sesame.task.antMember.AntMember;
import fansirsqi.xposed.sesame.task.antOcean.AntOcean;
import fansirsqi.xposed.sesame.task.antOrchard.AntOrchard;
import fansirsqi.xposed.sesame.task.antSports.AntSports;
import fansirsqi.xposed.sesame.task.antStall.AntStall;
import fansirsqi.xposed.sesame.task.greenFinance.GreenFinance;
import fansirsqi.xposed.sesame.task.reserve.Reserve;
import fansirsqi.xposed.sesame.task.antDodo.AntDodo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModelOrder {
    @SuppressWarnings("unchecked")
    private static final Class<Model>[] array = new Class[]{
            BaseModel.class
            , AntForest.class
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