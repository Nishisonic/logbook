package logbook.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アイテム種別(カテゴリ)
 *
 */
public class ItemCategoryType {

    /**
     * アイテム種別プリセット値
     */
    private static final Map<Integer, String> ITEMTYPECATEGORY = new ConcurrentHashMap<Integer, String>() {
        {
            this.put(1, "小口径主砲");
            this.put(2, "中口径主砲");
            this.put(3, "大口径主砲");
            this.put(4, "副砲");
            this.put(5, "魚雷");
            this.put(6, "艦上戦闘機");
            this.put(7, "艦上爆撃機");
            this.put(8, "艦上攻撃機");
            this.put(9, "艦上偵察機");
            this.put(10, "水上偵察機");
            this.put(11, "水上爆撃機");
            this.put(12, "小型電探");
            this.put(13, "大型電探");
            this.put(14, "ソナー");
            this.put(15, "爆雷");
            this.put(16, "追加装甲");
            this.put(17, "機関部強化");
            this.put(18, "対空強化弾");
            this.put(19, "対艦強化弾");
            this.put(20, "VT信管");
            this.put(21, "対空機銃");
            this.put(22, "特殊潜航艇");
            this.put(23, "応急修理要員");
            this.put(24, "上陸用舟艇");
            this.put(25, "オートジャイロ");
            this.put(26, "対潜哨戒機");
            this.put(27, "追加装甲(中型)");
            this.put(28, "追加装甲(大型)");
            this.put(29, "探照灯");
            this.put(30, "簡易輸送部材");
            this.put(31, "艦艇修理施設");
            this.put(32, "潜水艦魚雷");
            this.put(33, "照明弾");
            this.put(34, "司令部施設");
            this.put(35, "航空要員");
            this.put(36, "高射装置");
            this.put(37, "対地装備");
            //this.put(38, "大口径主砲(II)");
            this.put(39, "水上艦要員");
            this.put(40, "大型ソナー");
            this.put(41, "大型飛行艇");
            this.put(42, "大型探照灯");
            this.put(43, "戦闘糧食");
            this.put(44, "補給物資");
            this.put(45, "水上戦闘機");
            this.put(46, "特型内火艇");
            this.put(47, "陸上攻撃機");
            this.put(48, "局地戦闘機");
            this.put(49, "陸上偵察機");
            this.put(50, "輸送機材");
            this.put(51, "潜水艦装備");
            this.put(52, "陸戦部隊");
            this.put(53, "大型陸上機");
            this.put(54, "水上艦装備");
            this.put(56, "噴式戦闘機");
            this.put(57, "噴式戦闘爆撃機");
            this.put(58, "噴式攻撃機");
            this.put(59, "噴式偵察機");
            //this.put(93, "大型電探(II)");
            //this.put(94, "艦上偵察機(II)");
            //this.put(95, "副砲(II)");

        }
    };

    public static Map<Integer, String> getAll() {
        return ITEMTYPECATEGORY;
    }

    /**
     * アイテム種別を取得します
     *
     * @param type ID
     * @return アイテム種別
     */
    public static String get(Integer type) {
        return ITEMTYPECATEGORY.get(type);
    }
}
