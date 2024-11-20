package fansirsqi.xposed.sesame.entity;

import fansirsqi.xposed.sesame.util.ReserveIdMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 表示支付宝保留项的实体类，包含 ID 和名称。
 */
public class AlipayReserve extends IdAndName {
    // 使用 volatile 关键字确保多线程环境下的可见性
    private static volatile List<AlipayReserve> list;

    /**
     * 构造方法，根据给定的 ID 和名称初始化对象。
     * @param i 保留项的 ID
     * @param n 保留项的名称
     */
    public AlipayReserve(String i, String n) {
        id = i;
        name = n;
    }

    /**
     * 获取包含所有保留项的列表，首次调用时从 ReserveIdMap 初始化。
     * 使用双重检查锁定机制实现懒加载以提高性能。
     * @return 包含所有 AlipayReserve 对象的不可变列表
     */
    public static List<AlipayReserve> getList() {
        if (list == null) {
            synchronized (AlipayReserve.class) {
                if (list == null) {
                    List<AlipayReserve> tempList = new ArrayList<>();
                    Set<Map.Entry<String, String>> idSet = ReserveIdMap.getMap().entrySet();
                    for (Map.Entry<String, String> entry : idSet) {
                        tempList.add(new AlipayReserve(entry.getKey(), entry.getValue()));
                    }
                    list = Collections.unmodifiableList(tempList);
                }
            }
        }
        return list;
    }

    /**
     * 根据给定的 ID 删除相应的 AlipayReserve 对象。
     * 首次调用 getList 方法以确保列表已初始化。
     * @param id 要删除的保留项 ID
     */
    public static void remove(String id) {
        getList();
        synchronized (AlipayReserve.class) {
            list = new ArrayList<>(list); // 创建可变列表的副本
            list.removeIf(reserve -> reserve.id.equals(id)); // 使用流简化移除操作
            list = Collections.unmodifiableList(list); // 确保返回不可变列表
        }
    }
}
