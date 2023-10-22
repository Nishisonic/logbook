package logbook.gui.logic;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import logbook.data.context.GlobalContext;
import logbook.dto.ItemDto;
import logbook.dto.ShipDto;

/**
 * @author Nishikuma
 *
 */
public class FleetAnalysis {
    /**
     * 艦隊分析の艦娘フォーマットを取得します
     *
     * @return フォーマット
     */
    public String getShipsFormat() {
        List<ShipDto> ships = GlobalContext.getShipMap().values().stream().collect(Collectors.toList());

        return "svdata={\"api_result\":1,\"api_result_msg\":\"成功\",\"api_data\":{\"api_ship\":["
                + ships.stream().map(ShipDto::getJson)
                        .filter(Objects::nonNull)
                        .map(json -> {
                            JsonObjectBuilder result = Json.createObjectBuilder();
                            if (json.containsKey("api_id")) {
                                result = result.add("api_id", json.getInt("api_id"));
                            }
                            if (json.containsKey("api_ship_id")) {
                                result = result.add("api_ship_id", json.getInt("api_ship_id"));
                            }
                            if (json.containsKey("api_lv")) {
                                result = result.add("api_lv", json.getInt("api_lv"));
                            }
                            if (json.containsKey("api_kyouka")) {
                                result = result.add("api_kyouka", json.getJsonArray("api_kyouka"));
                            }
                            if (json.containsKey("api_exp")) {
                                result = result.add("api_exp", json.getJsonArray("api_exp"));
                            }
                            if (json.containsKey("api_slot_ex")) {
                                result = result.add("api_slot_ex", json.getInt("api_slot_ex"));
                            }
                            if (json.containsKey("api_sally_area")) {
                                result = result.add("api_sally_area", json.getInt("api_sally_area"));
                            }
                            if (json.containsKey("api_locked")) {
                                result = result.add("api_locked", json.getInt("api_locked"));
                            }

                            return result.build().toString();
                        })
                        .collect(Collectors.joining(","))
                + "]}}";
    }

    /**
     * 艦隊分析の装備フォーマットを取得します
     *
     * @return フォーマット
     */
    public String getItemsFormat() {
        List<ItemDto> items = GlobalContext.getItemMap().values().stream().collect(Collectors.toList());

        return "svdata={\"api_result\":1,\"api_result_msg\":\"成功\",\"api_data\":["
                + items.stream().map(item -> Json.createObjectBuilder()
                        .add("api_id", item.getId())
                        .add("api_slotitem_id", item.getSlotitemId())
                        .add("api_level", item.getLevel())
                        .add("api_locked", item.isLocked() ? 1 : 0).build().toString()).collect(Collectors.joining(","))
                + "]}";
    }
}
