package logbook.gui.logic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import logbook.data.context.GlobalContext;
import logbook.dto.AirbaseDto;
import logbook.dto.ItemDto;
import logbook.dto.ShipDto;
import logbook.dto.SquadronDto;
import logbook.internal.LoggerHolder;

/**
 * デッキビルダーのフォーマットを作成するクラス
 *
 * @author Nishisonic
 */
public class DeckBuilder {
    /** ロガー */
    private static final LoggerHolder LOG = new LoggerHolder(DeckBuilder.class);

    private static final int DECKBUILDER_FORMAT_VERSION = 4;
    private static final double DECKBUILDER_V2_FORMAT_VERSION = 4.2;
    private static final String KC_TOOLS_URL = "https://noro6.github.io/kc-web/";
    private static final String FLEET_HUB_URL = "https://jervis.vercel.app/";

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのフォーマットを作成します
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @param needsUsedDock どの艦隊のデータを用いるか[第一艦隊,第二艦隊,第三艦隊,第四艦隊]
     * @return url URL
     */

    public static String toDeckBuilderURL(List<List<ShipDto>> fleets, List<AirbaseDto> airbases) {
        Optional<String> formatOpt = Optional.ofNullable(toDeckBuilderFormatV4_2(fleets, airbases));
        if (formatOpt.isPresent()) {
            return formatOpt.get();
        }
        else {
            return null;
        }
    }

    public static String toDeckBuilderURL(boolean[] needsUsedDock) {
        Optional<String> formatOpt = Optional.ofNullable(toDeckBuilderFormat(needsUsedDock));
        if (formatOpt.isPresent()) {
            return formatOpt.get();
        }
        else {
            return null;
        }
    }

    public static String toDeckBuilderAAURL(boolean[] needsUsedDock, int areaId) {
        Optional<String> formatOpt = Optional.ofNullable(toDeckBuilderFormatV4_2(needsUsedDock, areaId));
        if (formatOpt.isPresent()) {
            return formatOpt.get();
        }
        else {
            return null;
        }
    }

    public static String toKcToolsURL(List<List<ShipDto>> fleets, List<AirbaseDto> airbases) {
        Optional<String> formatOpt = Optional.ofNullable(toDeckBuilderFormatV4_2(fleets, airbases));
        if (formatOpt.isPresent()) {
            return KC_TOOLS_URL + "?predeck=" + formatOpt.get();
        }
        else {
            return null;
        }
    }

    public static String toKcToolsURL(boolean[] needsUsedDock, int areaId) {
        Optional<String> formatOpt = Optional.ofNullable(toDeckBuilderFormatV4_2(needsUsedDock, areaId));
        if (formatOpt.isPresent()) {
            return KC_TOOLS_URL + "?predeck=" + formatOpt.get();
        }
        else {
            return null;
        }
    }

    public static String toFleetHubURL(List<List<ShipDto>> fleets, List<AirbaseDto> airbases) {
        Optional<String> formatOpt = Optional.ofNullable(toDeckBuilderFormatV4_2(fleets, airbases));
        if (formatOpt.isPresent()) {
            return FLEET_HUB_URL + "?predeck=" + formatOpt.get();
        }
        else {
            return null;
        }
    }

    public static String toFleetHubURL(boolean[] needsUsedDock, int areaId) {
        Optional<String> formatOpt = Optional.ofNullable(toDeckBuilderFormatV4_2(needsUsedDock, areaId));
        if (formatOpt.isPresent()) {
            return FLEET_HUB_URL + "?predeck=" + formatOpt.get();
        }
        else {
            return null;
        }
    }

    public static String toDeckBuilderFormat(boolean[] needsUsedDock) {
        List<List<ShipDto>> fleets = IntStream.rangeClosed(1, 4)
                .boxed()
                .map(dockId -> needsUsedDock[dockId - 1] ? GlobalContext.getDock(dockId.toString()).getShips() : null)
                .collect(Collectors.toList());

        return toDeckBuilderFormat(fleets);
    }

    public static String toDeckBuilderFormat(List<List<ShipDto>> fleets) {
        JsonObjectBuilder deck = Json.createObjectBuilder();
        deck.add("version", DECKBUILDER_FORMAT_VERSION);
        try {
            for (int dockId = 1; dockId <= fleets.size(); dockId++) {
                List<ShipDto> ships = fleets.get(dockId - 1);
                if (Objects.isNull(ships)) {
                    continue;
                }

                JsonObjectBuilder fleet = Json.createObjectBuilder();
                for (int shipIdx = 0; shipIdx < ships.size(); shipIdx++) {
                    JsonObjectBuilder ship = Json.createObjectBuilder();
                    ship.add("id", Integer.toString(ships.get(shipIdx).getShipInfo().getShipId()));
                    ship.add("lv", ships.get(shipIdx).getLv());
                    ship.add("luck", ships.get(shipIdx).getLucky());
                    JsonObjectBuilder items = Json.createObjectBuilder();
                    List<ItemDto> item2 = ships.get(shipIdx).getItem2();

                    for (int itemIdx = 0; itemIdx < item2.size(); itemIdx++) {
                        JsonObjectBuilder item = Json.createObjectBuilder();
                        ItemDto itemDto = item2.get(itemIdx);
                        if (Objects.isNull(itemDto)) {
                            break;
                        }

                        item.add("id", itemDto.getSlotitemId());
                        if (itemDto.getLevel() > 0) {
                            item.add("rf", Integer.toString(itemDto.getLevel()));
                        }
                        else {
                            item.add("rf", 0);
                        }
                        item.add("mas", Integer.toString(itemDto.getAlv()));
                        items.add("i" + (itemIdx + 1), item);
                    }

                    ItemDto slotExItem = ships.get(shipIdx).getSlotExItem();
                    if (Objects.nonNull(slotExItem)) {
                        JsonObjectBuilder item = Json.createObjectBuilder();
                        item.add("id", slotExItem.getSlotitemId());
                        if (slotExItem.getLevel() > 0) {
                            item.add("rf", Integer.toString(slotExItem.getLevel()));
                        }
                        else {
                            item.add("rf", 0);
                        }
                        item.add("mas", Integer.toString(slotExItem.getAlv()));
                        int slotNum = ships.get(shipIdx).getSlotNum();
                        if (slotNum < 5) {
                            items.add("i" + (slotNum + 1), item);
                        }
                        else {
                            items.add("ix", item);
                        }
                    }
                    ship.add("items", items);

                    fleet.add("s" + (shipIdx + 1), ship);
                }
                deck.add("f" + dockId, fleet);
            }
            return deck.build().toString();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public static String toDeckBuilderFormatV4_2(List<List<ShipDto>> fleets, List<AirbaseDto> airbases) {
        JsonObjectBuilder deck = Json.createObjectBuilder();
        deck.add("version", DECKBUILDER_V2_FORMAT_VERSION);
        try {
            for (int dockId = 1; dockId <= fleets.size(); dockId++) {
                List<ShipDto> ships = fleets.get(dockId - 1);
                if (Objects.isNull(ships)) {
                    continue;
                }

                JsonObjectBuilder fleet = Json.createObjectBuilder();
                for (int shipIdx = 0; shipIdx < ships.size(); shipIdx++) {
                    JsonObjectBuilder ship = Json.createObjectBuilder();
                    ship.add("id", Integer.toString(ships.get(shipIdx).getShipInfo().getShipId()));
                    ship.add("lv", ships.get(shipIdx).getLv());
                    ship.add("hp", ships.get(shipIdx).getMaxhp());
                    ship.add("luck", ships.get(shipIdx).getLucky());
                    JsonObjectBuilder items = Json.createObjectBuilder();
                    List<ItemDto> item2 = ships.get(shipIdx).getItem2();

                    for (int itemIdx = 0; itemIdx < item2.size(); itemIdx++) {
                        JsonObjectBuilder item = Json.createObjectBuilder();
                        ItemDto itemDto = item2.get(itemIdx);
                        if (Objects.isNull(itemDto)) {
                            break;
                        }

                        item.add("id", itemDto.getSlotitemId());
                        if (itemDto.getLevel() > 0) {
                            item.add("rf", Integer.toString(itemDto.getLevel()));
                        }
                        else {
                            item.add("rf", 0);
                        }
                        item.add("mas", Integer.toString(itemDto.getAlv()));
                        items.add("i" + (itemIdx + 1), item);
                    }

                    ItemDto slotExItem = ships.get(shipIdx).getSlotExItem();
                    if (Objects.nonNull(slotExItem)) {
                        JsonObjectBuilder item = Json.createObjectBuilder();
                        item.add("id", slotExItem.getSlotitemId());
                        if (slotExItem.getLevel() > 0) {
                            item.add("rf", Integer.toString(slotExItem.getLevel()));
                        }
                        else {
                            item.add("rf", 0);
                        }
                        item.add("mas", Integer.toString(slotExItem.getAlv()));
                        int slotNum = ships.get(shipIdx).getSlotNum();
                        if (slotNum < 5) {
                            items.add("i" + (slotNum + 1), item);
                        }
                        else {
                            items.add("ix", item);
                        }
                    }
                    ship.add("items", items);

                    fleet.add("s" + (shipIdx + 1), ship);
                }
                deck.add("f" + dockId, fleet);
            }

            if (Objects.nonNull(airbases)) {
                airbases.forEach(airbase -> {
                    List<SquadronDto> squadrons = airbase.getPlaneInfos();
                    JsonObjectBuilder squadronJson = Json.createObjectBuilder();
                    squadrons.forEach((squadron) -> {
                        if (squadron.getSlotitemId() > 0) {
                            squadronJson.add("i" + squadron.getSquadronId(), Json.createObjectBuilder()
                                    .add("id", squadron.getSlotitemId())
                                    .add("rf", squadron.getLevel())
                                    .add("mas", squadron.getAlv()));
                        }
                    });
                    deck.add("a" + airbase.getRid(), Json.createObjectBuilder()
                            .add("mode", airbase.getActionKind())
                            .add("items", squadronJson));
                });
            }
            return deck.build().toString();
        } catch (NullPointerException e) {
            LOG.get().warn("デッキビルダー出力に失敗しました", e);
            return null;
        }
    }

    public static String toDeckBuilderFormatV4_2(boolean[] needsUsedDock, int areaId) {
        List<List<ShipDto>> fleets = IntStream.rangeClosed(1, 4)
                .boxed()
                .map(dockId -> needsUsedDock[dockId - 1] ? GlobalContext.getDock(dockId.toString()).getShips() : null)
                .collect(Collectors.toList());

        Map<Integer, List<AirbaseDto>> airbaseMap = GlobalContext.getAirbases().stream()
                .collect(Collectors.groupingBy(airbase -> airbase.getAreaId()));
        int area = areaId == 0 ? airbaseMap.keySet().stream().filter(a -> a >= 22).findFirst().orElse(-1)
                : airbaseMap.containsKey(areaId) ? areaId : -1;

        return toDeckBuilderFormatV4_2(fleets, airbaseMap.get(area));
    }
}
