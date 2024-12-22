/**
 *
 */
package logbook.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.dyuproject.protostuff.Tag;

import logbook.internal.Item;
import logbook.util.JsonUtils;

/**
 * @author Nekopanda
 */
public class AirBattleDto {

    /**
     * 攻撃シーケンス
     */
    @Tag(1)
    public List<BattleAtackDto> atacks;
    /**
     * 触接 [味方, 敵]
     */
    @Tag(2)
    public int[] touchPlane;
    /**
     * 制空状態
     */
    @Tag(3)
    public String seiku;
    /**
     * stage1 艦載機数 [味方ロスト, 味方全, 敵ロスト, 敵全]
     */
    @Tag(4)
    public int[] stage1;
    /**
     * stage2 艦載機数 [味方ロスト, 味方全, 敵ロスト, 敵全]
     */
    @Tag(5)
    public int[] stage2;
    /**
     * 対空カットイン [発動艦0-, 種別]
     */
    @Tag(8)
    public int[] airFire;
    @Tag(9)
    public int[] airFireItems;
    /**
     * 基地航空隊ID
     */
    @Tag(21)
    public int airBaseId;

    private static int[] readPlaneCount(JsonObject stage) {
        return new int[] {
                stage.getInt("api_f_lostcount"),
                stage.getInt("api_f_count"),
                stage.getInt("api_e_lostcount"),
                stage.getInt("api_e_count")
        };
    }

    public AirBattleDto(int baseidx, int friendSecondBase, JsonObject kouku, boolean isCombined, boolean isBase) {
        JsonObject jsonStage1 = JsonUtils.getJsonObject(kouku, "api_stage1");
        if (jsonStage1 != null) {
            this.stage1 = readPlaneCount(jsonStage1);
            JsonArray jsonTouchPlane = JsonUtils.getJsonArray(jsonStage1, "api_touch_plane");
            if (jsonTouchPlane != null) {
                this.touchPlane = new int[] {
                        jsonTouchPlane.getInt(0),
                        jsonTouchPlane.getInt(1)
                };
                this.seiku = toSeiku(jsonStage1.getInt("api_disp_seiku"));
            }
        }

        JsonObject jsonStage2 = JsonUtils.getJsonObject(kouku, "api_stage2");
        if (jsonStage2 != null) {
            this.stage2 = readPlaneCount(jsonStage2);

            JsonObject jsonAirFire = JsonUtils.getJsonObject(jsonStage2, "api_air_fire");
            if (jsonAirFire != null) {
                this.airFire = new int[] {
                        jsonAirFire.getInt("api_idx"),
                        jsonAirFire.getInt("api_kind")
                };
                this.airFireItems = JsonUtils.getIntArray(jsonAirFire, "api_use_items");
            }
        }

        this.atacks = BattleAtackDto.makeAir(
                baseidx, friendSecondBase,
                JsonUtils.getJsonArray(kouku, "api_plane_from"),
                JsonUtils.getJsonObject(kouku, "api_stage3"),
                isCombined ? JsonUtils.getJsonObject(kouku, "api_stage3_combined") : null,
                isBase);
    }

    private static String toSeiku(int id) {
        switch (id) {
        case 1:
            return "制空権確保";
        case 2:
            return "航空優勢";
        case 0:
            return "航空互角";
        case 3:
            return "航空劣勢";
        case 4:
            return "制空権喪失";
        default:
            return "不明(" + id + ")";
        }
    }

    private static String toTaikuCutin(int id) {
        switch (id) {
        case 1:
            return "1:高角砲x2/電探";
        case 2:
            return "2:高角砲/電探";
        case 3:
            return "3:高角砲x2";
        case 4:
            return "4:大口径主砲/三式弾/高射装置/電探";
        case 5:
            return "5:高角砲+高射装置x2/電探";
        case 6:
            return "6:大口径主砲/三式弾/高射装置";
        case 7:
            return "7:高角砲/高射装置/電探";
        case 8:
            return "8:高角砲+高射装置/電探";
        case 9:
            return "9:高角砲/高射装置";
        case 10:
            return "10:高角砲/集中機銃/電探";
        case 11:
            return "11:高角砲/集中機銃";
        case 12:
            return "12:集中機銃/機銃/電探";
        case 13:
            return "13:高角砲/集中機銃/電探";
        case 14:
            return "14:高角砲/機銃/電探";
        case 15:
            return "15:高角砲/機銃";
        case 16:
            return "16:高角砲/機銃/電探";
        case 17:
            return "17:高角砲/機銃";
        case 18:
            return "18:集中機銃";
        case 19:
            return "19:高角砲(非高射装置)/集中機銃";
        case 20:
            return "20:集中機銃";
        case 21:
            return "21:高角砲/電探";
        case 22:
            return "22:集中機銃";
        case 23:
            return "23:機銃(非集中)";
        case 24:
            return "24:高角砲/機銃(非集中)";
        case 25:
            return "25:噴進砲改二/電探/三式弾";
        case 26:
            return "26:高角砲+増設機銃/電探";
        case 27:
            return "27:高角砲+増設機銃/噴進砲改二/電探";
        case 28:
            return "28:新噴進砲/電探";
        case 29:
            return "29:高角砲/電探";
        case 30:
            return "30:高角砲x3";
        case 31:
            return "31:高角砲x2";
        case 32:
            return "32:ロケランx2 or FCR/ポンポン砲 or ロケラン/ポンポン砲";
        case 33:
            return "33:高角砲/機銃(非集中)";
        case 34:
            return "34:Fletcher砲改+GFCSx2";
        case 35:
            return "35:Fletcher砲改+GFCS/Fletcher砲改";
        case 36:
            return "36:Fletcher砲改x2/GFCS";
        case 37:
            return "37:Fletcher砲改x2";
        case 38:
            return "38:Atlanta砲+GFCSx2";
        case 39:
            return "39:Atlanta砲+GFCS/Atlanta砲";
        case 40:
            return "40:Atlanta砲x2/GFCS";
        case 41:
            return "41:Atlanta砲x2";
        case 42:
            return "42:高角砲群集中配備x2/機銃/大和型電探";
        case 43:
            return "43:高角砲群集中配備x2/大和型電探";
        case 44:
            return "44:高角砲群集中配備/機銃/大和型電探";
        case 45:
            return "45:高角砲群集中配備/大和型電探";
        case 46:
            return "46:35.6改三ダズルor35.6改四/集中機銃/電探";
        case 47:
            return "47:12.7cm連装砲C型改三Hx2 or 12.7cm連装砲C型改三H/25mm対空機銃増備/電探";
        case 48:
            return "48:10cm連装高角砲改+高射装置改x2/対空電探";
        case 49:
            return "49:特殊高角砲/対空電探";
        }
        return "不明(" + id + ")";
    }

    public String[] getStage1ShortString() {
        return getNumPlaneString(this.stage1, false);
    }

    public String[] getStage2ShortString() {
        return getNumPlaneString(this.stage2, false);
    }

    public String[] getStage1DetailedString() {
        return getNumPlaneString(this.stage1, true);
    }

    public String[] getStage2DetailedString() {
        return getNumPlaneString(this.stage2, true);
    }

    public String getTaikuCutinString() {
        return toTaikuCutin(this.airFire[1]);
    }

    public String getTaikuCutinItemsString() {
        return String.join("/", Arrays.stream(this.airFireItems)
                .boxed()
                .map(Item::get)
                .map(item -> Objects.nonNull(item) ? item.getName() : "装備不明")
                .toArray(String[]::new));
    }

    /**
     * 艦載機ロスト表示を生成 [味方・敵]
     *
     * @param stage
     * @return
     */
    private static String[] getNumPlaneString(int[] stage, boolean detail) {
        if (stage == null) {
            return new String[] { "", "" };
        }
        int flost = stage[0];
        int fall = stage[1];
        int elost = stage[2];
        int eall = stage[3];
        int fremain = fall - flost;
        int eremain = eall - elost;
        if (detail) {
            return new String[] {
                    String.valueOf(fall) + "→" + fremain + " (-" + flost + ")",
                    String.valueOf(eall) + "→" + eremain + " (-" + elost + ")"
            };
        }
        else {
            return new String[] {
                    String.valueOf(fall) + "→" + fremain,
                    String.valueOf(eall) + "→" + eremain
            };
        }
    }

    public static String[] toTouchPlaneString(int[] touchPlane) {
        if (touchPlane == null) {
            return new String[] { "", "" };
        }
        String[] ret = new String[2];
        for (int i = 0; i < 2; ++i) {
            if (touchPlane[i] == -1) {
                ret[i] = "なし";
            }
            else {
                ItemInfoDto item = Item.get(touchPlane[i]);
                if (item != null) {
                    ret[i] = item.getName();
                }
                else {
                    ret[i] = "あり（機体不明）";
                }
            }
        }
        return ret;
    }

    /**
     * 触接表示を生成 [味方・敵]
     *
     * @param touchPlane
     * @return
     */
    public String[] getTouchPlane() {
        return toTouchPlaneString(this.touchPlane);
    }
}
