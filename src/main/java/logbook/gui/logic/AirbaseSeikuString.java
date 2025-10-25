package logbook.gui.logic;

import java.util.Arrays;
import java.util.List;

import logbook.config.AppConfig;
import logbook.dto.AirPower;
import logbook.dto.SquadronDto;

/**
 * @author Nishikuma
 */
public class AirbaseSeikuString {

    private static int[][] alevelBonusTable = new int[][] {
            { 0, 0, 2, 5, 9, 14, 14, 22 }, // 艦上戦闘機、水上戦闘機、夜間戦闘機
            { 0, 0, 0, 0, 0, 0, 0, 0 }, // 艦上爆撃機、艦上攻撃機、噴式戦闘爆撃機、陸上偵察機
            { 0, 0, 1, 1, 1, 3, 3, 6 }, // 水上爆撃機
            { 0, 0, 2, 5, 9, 14, 14, 22 }, // 一式戦 隼II型改(20戦隊)、一式戦 隼III型改(熟練/20戦隊)
    };

    private static int[] internalAlevelTable = new int[] {
            0, 10, 25, 40, 55, 70, 85, 100, 121
    };

    public static AirPower getSortieAirPower(List<SquadronDto> squadrons) {
        int method = AppConfig.get().getSeikuMethod();
        AirPower power = squadrons.stream().filter(squadron -> squadron.getCount() > 0).map(squadron -> {
            int constSkilledBonus;
            int[] skilledBonus;
            double base;
            switch (squadron.getType2()) {
            case 6: // 艦上戦闘機、夜間戦闘機
            case 45: // 水上戦闘機
                // case 56: // 噴式戦闘機
                base = (squadron.getParam().getTaiku() + 0.2 * squadron.getLevel()) * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[0][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 48: // 局地戦闘機
                base = (squadron.getParam().getTaiku() + 1.5 * squadron.getParam().getKaihi()
                        + 0.2 * squadron.getLevel())
                        * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[0][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 7: // 艦上爆撃機、夜間爆撃機?
                base = (squadron.getParam().getTaiku()
                        + (squadron.getParam().getTaiku() > 3 ? 0.25 * squadron.getLevel() : 0))
                        * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 8: // 艦上攻撃機、夜間攻撃機
            case 57: // 噴式戦闘爆撃機
                // case 58: // 噴式攻撃機
                base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 47: // 陸上攻撃機
                base = (squadron.getParam().getTaiku() + 0.5 * Math.sqrt(squadron.getLevel()))
                        * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 11: // 水上爆撃機
                base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[2][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 25: // オートジャイロ
                return new AirPower(0, 0);
            case 26: // 対潜哨戒機
                if (squadron.getSlotitemId() == 489 || squadron.getSlotitemId() == 491) {
                    // 一式戦 隼II型改(20戦隊)、一式戦 隼III型改(熟練/20戦隊)
                    base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount());
                    constSkilledBonus = alevelBonusTable[3][squadron.getAlv()];
                    skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                            internalAlevelTable[squadron.getAlv() + 1] - 1 };
                    break;
                }
                return new AirPower(0, 0);
            case 49: // 陸上偵察機
                // 無理やり対応(ちゃんと判明したら対応)
                base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount())
                        + (squadron.getLevel() >= 2 ? 1 : 0);
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            default:
                base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            }
            int min = (int) Math.floor(base + constSkilledBonus + Math.sqrt(skilledBonus[0] / 10));
            int max = (int) Math.floor(base + constSkilledBonus + Math.sqrt(skilledBonus[1] / 10));
            if (method == 1 || method == 2 || method == 3 || method == 4) {
                // 熟練度付き制空値
                return new AirPower(min, max);
            }
            else {
                // 素の制空値
                return new AirPower((int) base);
            }
        }).reduce(new AirPower(0), (p, v) -> {
            p.add(v);
            return p;
        });

        int reconBonus = squadrons.stream().filter(squadron -> squadron.getCount() > 0).mapToInt(squadron -> {
            int search = squadron.getParam().getSakuteki();
            switch (squadron.getType2()) {
            case 49: // 陸上偵察機
                if (search >= 9)
                    return 118;
                return 115; // search = 8
            }
            return 100;
        }).max().orElse(100);
        power.setMin(power.getMin() * reconBonus / 100);
        power.setMax(power.getMax() * reconBonus / 100);
        return power;
    }

    public static AirPower getDefenseAirPower(List<SquadronDto> squadrons) {
        int method = AppConfig.get().getSeikuMethod();
        AirPower power = squadrons.stream().filter(squadron -> squadron.getCount() > 0).map(squadron -> {
            int constSkilledBonus;
            int[] skilledBonus;
            double base;
            switch (squadron.getType2()) {
            case 6: // 艦上戦闘機、夜間戦闘機
            case 45: // 水上戦闘機
                // case 56: // 噴式戦闘機
                base = (squadron.getParam().getTaiku() + 0.2 * squadron.getLevel()) * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[0][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 48: // 局地戦闘機
                base = (squadron.getParam().getTaiku() + squadron.getParam().getKaihi()
                        + 2 * squadron.getParam().getHoum()
                        + 0.2 * squadron.getLevel()) * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[0][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 7: // 艦上爆撃機、夜間爆撃機?
                base = (squadron.getParam().getTaiku()
                        + (squadron.getParam().getTaiku() > 3 ? 0.25 * squadron.getLevel() : 0))
                        * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 8: // 艦上攻撃機、夜間攻撃機
            case 57: // 噴式戦闘爆撃機
                // case 58: // 噴式攻撃機
                base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 47: // 陸上攻撃機
                base = (squadron.getParam().getTaiku() + 0.5 * Math.sqrt(squadron.getLevel()))
                        * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 11: // 水上爆撃機
                base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[2][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            case 25: // オートジャイロ
                return new AirPower(0, 0);
            case 26: // 対潜哨戒機
                if (squadron.getSlotitemId() == 489 || squadron.getSlotitemId() == 491) {
                    // 一式戦 隼II型改(20戦隊)、一式戦 隼III型改(熟練/20戦隊)
                    base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount());
                    constSkilledBonus = alevelBonusTable[3][squadron.getAlv()];
                    skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                            internalAlevelTable[squadron.getAlv() + 1] - 1 };
                    break;
                }
                return new AirPower(0, 0);
            case 49: // 陸上偵察機
                // 無理やり対応(ちゃんと判明したら対応)
                base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount())
                        + (squadron.getLevel() >= 2 ? 1 : 0);
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            default:
                base = squadron.getParam().getTaiku() * Math.sqrt(squadron.getCount());
                constSkilledBonus = alevelBonusTable[1][squadron.getAlv()];
                skilledBonus = new int[] { internalAlevelTable[squadron.getAlv()],
                        internalAlevelTable[squadron.getAlv() + 1] - 1 };
                break;
            }
            int min = (int) Math.floor(base + constSkilledBonus + Math.sqrt(skilledBonus[0] / 10));
            int max = (int) Math.floor(base + constSkilledBonus + Math.sqrt(skilledBonus[1] / 10));
            if (method == 1 || method == 2 || method == 3 || method == 4) {
                // 熟練度付き制空値
                return new AirPower(min, max);
            }
            else {
                // 素の制空値
                return new AirPower((int) base);
            }
        }).reduce(new AirPower(0), (p, v) -> {
            p.add(v);
            return p;
        });

        int reconBonus = squadrons.stream().filter(squadron -> squadron.getCount() > 0).mapToInt(squadron -> {
            int search = squadron.getParam().getSakuteki();
            switch (squadron.getType2()) {
            case 9: // 艦上偵察機
            case 94: // 艦上偵察機(II)
                // case 59: // 噴式偵察機
                if (search >= 9)
                    return 130;
                return 120; // search = 7
            case 10: // 水上偵察機
            case 41: // 大型飛行艇
                if (search >= 9)
                    return 116;
                if (search == 8)
                    return 113;
                return 110;
            case 49: // 陸上偵察機
                if (search >= 9)
                    return 123;
                return 118; // search = 8
            }
            return 100;
        }).max().orElse(100);
        power.setMin(power.getMin() * reconBonus / 100);
        power.setMax(power.getMax() * reconBonus / 100);

        return power;
    }

    public static AirPower getHighDefenseAirPower(List<SquadronDto> squadrons) {
        AirPower power = getDefenseAirPower(squadrons);
        List<Integer> rocketIds = Arrays.asList(350, 351, 352);
        int rocket = (int) squadrons.stream()
                .filter(squadron -> squadron.getCount() > 0 && rocketIds.contains(squadron.getSlotitemId())).count();
        double bonus = 0.5;
        if (rocket >= 3) {
            bonus = 1.2;
        }
        else if (rocket == 2) {
            bonus = 1.1;
        }
        else if (rocket == 1) {
            bonus = 0.8;
        }

        return new AirPower((int) (power.getMin() * bonus), (int) (power.getMax() * bonus));
    }
}
