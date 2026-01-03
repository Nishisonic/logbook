package logbook.server.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import logbook.config.AppConfig;
import logbook.constants.AppConstants;
import logbook.data.Data;
import logbook.data.context.GlobalContext;
import logbook.dto.BattleExDto;
import logbook.dto.BattlePhaseKind;
import logbook.dto.DockDto;
import logbook.dto.ItemDto;
import logbook.dto.MapCellDto;
import logbook.dto.MapHpInfoDto;
import logbook.dto.ShipDto;
import logbook.gui.ApplicationMain;
import logbook.gui.logic.SakutekiString;
import logbook.internal.LoggerHolder;

import org.eclipse.swt.widgets.Display;

/**
 * TsunDB Client
 * 
 * 対応状況
 * ⭕:対応済み
 * 🔺:中途半端
 * ❌:未対応(やる気があれば対応できるかも)
 * ➖:対応予定なし
 * 
 * aaci:❌(対空CI用の手間がきつい)
 * abnormaldamage:❌(異常ダメ用の手間がきつい)
 * celldata:⭕
 * development:⭕
 * enemycomp:🔺(全部は送ってない)
 * equips:➖
 * eventreward:⭕(多分)
 * eventworld:⭕
 * expedition:❌(装備ボーナス取得が無理)
 * fits:➖
 * friendlyfleet:⭕
 * gimmick:❌(うーん)
 * lolimodfod:➖
 * maelstrom:➖
 * normalworld:⭕
 * sanma:❌(秋刀魚の取得面倒)
 * shipdrop:⭕
 * shipdroplocations:⭕
 * spattack:➖
 * 
 * @author Nishikuma
 */
public class TsunDBClient extends Thread {
    private static final LoggerHolder LOG = new LoggerHolder(TsunDBClient.class);
    private static TsunDBClient instance = null;

    private static class QueueItem {
        public String target;
        public String data;

        public QueueItem(String target, String data) {
            this.target = target;
            this.data = data;
        }
    }

    private static synchronized TsunDBClient getInstance() {
        if (instance == null) {
            instance = new TsunDBClient();
            instance.start();
        }
        return instance;
    }

    public static void send(String serverName, Data data) {
        if (!AppConfig.get().isSendTsunDB())
            return;
        // 私設サーバー対策
        if (!Arrays.asList(AppConstants.KANCOLLE_DOMAIN_LIST).contains(serverName))
            return;
        switch (data.getDataType()) {
        case START:
            // 空にする
            edgeID = new ArrayList<Integer>();
            processCellData(data);
        case NEXT:
            processNext(data);
            break;
        case BATTLE_MIDNIGHT:
        case COMBINED_BATTLE_MIDNIGHT:
        case COMBINED_EC_BATTLE_MIDNIGHT:
            processFriendlyFleet(data);
            break;
        case BATTLE_NIGHT_TO_DAY:
        case COMBINED_BATTLE_SP_MIDNIGHT:
        case COMBINED_EC_NIGHT_TO_DAY:
        case BATTLE_SP_MIDNIGHT:
            // each_sp_midnight
            processFriendlyFleet(data);
        case BATTLE:
        case AIR_BATTLE:
        case LD_AIRBATTLE:
        case LD_SHOOTING:
        case COMBINED_AIR_BATTLE:
        case COMBINED_BATTLE:
        case COMBINED_BATTLE_WATER:
        case COMBINED_LD_AIRBATTLE:
        case COMBINED_LD_SHOOTING:
        case COMBINED_EC_BATTLE:
        case COMBINED_EACH_BATTLE:
            // each_airbattle
        case COMBINED_EACH_BATTLE_WATER:
            // each_ld_airbattle
            // each_ld_shooting
            processEnemy(data);
            break;
        case BATTLE_RESULT:
        case COMBINED_BATTLE_RESULT:
            int shipCount = GlobalContext.getShipMap().size();
            int itemCount = GlobalContext.slotItemSize();
            if (GlobalContext.maxChara() > shipCount &&
                    GlobalContext.maxSlotitem() > itemCount) {
                processDrop(data);
            }
            break;
        case CREATE_ITEM:
            processDevelopment(data);
            break;
        case PORT:
            break;
        default:
            break;
        }
        return;
    }

    private static void processDrop(Data data) {
        BattleExDto lastBattleDto = GlobalContext.getLastBattleDto();
        if (Objects.isNull(lastBattleDto))
            return;
        JsonObject json = data.getJsonObject().getJsonObject("api_data");
        MapCellDto mapCellDto = lastBattleDto.getMapCellDto();
        if (Objects.isNull(mapCellDto))
            return;
        int[] maps = mapCellDto.getMap();
        int mapId = maps[0] * 10 + maps[1];
        String map = maps[0] + "-" + maps[1];
        int node = maps[2];
        String rank = lastBattleDto.getRank().rank();
        JsonObject apiData = lastBattleDto.getPhase1().getJson();
        JsonObjectBuilder enemyComp = Json.createObjectBuilder()
                .add("ship", apiData.get("api_ship_ke"))
                .add("lvl", apiData.get("api_ship_lv"))
                .add("hp", apiData.get("api_e_maxhps"))
                .add("stats", apiData.getOrDefault("api_eParam", Json.createArrayBuilder().build()))
                .add("equip", apiData.get("api_eSlot"))
                .add("formation", apiData.getJsonArray("api_formation").get(1))
                .add("mapName", lastBattleDto.getQuestName())
                .add("compName", lastBattleDto.getEnemyName())
                .add("baseExp", json.getInt("api_get_base_exp"));
        if (Objects.isNull(GlobalContext.getMapHpInfo()))
            return;
        Optional<MapHpInfoDto> mapHpInfoOpt = GlobalContext.getMapHpInfo().stream()
                .filter(maphpinfo -> maphpinfo.getMapId() == mapId).findFirst();
        if (!mapHpInfoOpt.isPresent())
            return;
        MapHpInfoDto mapHpInfo = mapHpInfoOpt.get();
        boolean cleared = mapHpInfo.getCleared() == 1;
        if (maps[0] > 10) {
            enemyComp.add("mapStats", Json.createObjectBuilder()
                    .add("gaugeNum", mapHpInfo.getGaugeIndex())
                    .add("currentHP", mapHpInfo.getNowMapHp())
                    .add("maxHP", mapHpInfo.getMaxMapHp()));
        }
        if (apiData.containsKey("api_ship_ke_combined")) {
            enemyComp.add("shipEscort", apiData.get("api_ship_ke_combined"))
                    .add("lvlEscort", apiData.get("api_ship_lv_combined"))
                    .add("hpEscort", apiData.get("api_e_maxhps_combined"))
                    .add("statsEscort", apiData.get("api_eParam_combined"))
                    .add("equipEscort", apiData.get("api_eSlot_combined"));
        }
        BattlePhaseKind kind = lastBattleDto.getPhase1().getKind();
        if (kind == BattlePhaseKind.LD_AIRBATTLE || kind == BattlePhaseKind.COMBINED_LD_AIR) {
            enemyComp.add("isAirRaid", true);
        }
        int hqLvl = lastBattleDto.getHqLv();
        int difficulty = mapHpInfo.getDifficulty();
        if (json.containsKey("api_get_eventitem")) {
            processEventReward(map, difficulty, json.get("api_get_eventitem"), json.get("api_select_reward_dict"));
        }
        int ship = lastBattleDto.isDropShip() ? lastBattleDto.getDropShipId() : -1;
        JsonObjectBuilder counts = Json.createObjectBuilder();
        int shipCount = GlobalContext.getShipMap().keySet().size();
        GlobalContext.getShipMap().entrySet().stream()
                .sorted(Comparator.comparing(Entry::getKey))
                .limit(shipCount - (ship > 0 ? 1 : 0))
                .map(Entry::getValue)
                .collect(Collectors.groupingBy(ShipDto::getCharId, Collectors.counting()))
                .forEach((charId, count) -> counts.add(Integer.toString(charId), count));
        String result = Json.createObjectBuilder()
                .add("map", map)
                .add("node", node)
                .add("rank", rank)
                .add("cleared", cleared)
                .add("enemyComp", enemyComp.build().toString())
                .add("hqLvl", hqLvl)
                .add("difficulty", difficulty)
                .add("ship", ship)
                .add("counts", counts.build().toString())
                .build()
                .toString();
        getInstance().dataQueue.offer(new QueueItem("drops", result));
        processDropLoc(ship, map, node, rank, difficulty);
    }

    private static void processDropLoc(int ship, String map, int node, String rank, int difficulty) {
        String result = Json.createObjectBuilder()
                .add("ship", ship)
                .add("map", map)
                .add("node", node)
                .add("rank", rank)
                .add("difficulty", difficulty)
                .build()
                .toString();
        getInstance().dataQueue.offer(new QueueItem("droplocs", result));
    }

    private static void processDevelopment(Data data) {
        JsonObject json = data.getJsonObject().getJsonObject("api_data");
        int hqLvl = GlobalContext.hqLevel();
        ShipDto secretary = GlobalContext.getSecretary();
        if (!(hqLvl > 0 && Objects.nonNull(secretary)))
            return;
        json.getJsonArray("api_get_items").forEach(e -> {
            int slotitemId = Json.createObjectBuilder().add("e", e).build().getJsonObject("e")
                    .getInt("api_slotitem_id");
            String result = Json.createObjectBuilder()
                    .add("hqLvl", hqLvl)
                    .add("flagship",
                            Json.createObjectBuilder().add("id", secretary.getShipId())
                                    .add("type", secretary.getStype()).add("lvl", secretary.getLv()).build().toString())
                    .add("resources", Json.createObjectBuilder()
                            .add("fuel", Integer.parseInt(data.getField("api_item1")))
                            .add("ammo", Integer.parseInt(data.getField("api_item2")))
                            .add("steel", Integer.parseInt(data.getField("api_item3")))
                            .add("bauxite", Integer.parseInt(data.getField("api_item4"))).build().toString())
                    .add("result", slotitemId)
                    .add("success", slotitemId != -1)
                    .build()
                    .toString();
            getInstance().dataQueue.offer(new QueueItem("development", result));
        });
    }

    private static void processEventReward(String map, int difficulty, JsonValue rewards, JsonValue selectreward) {
        String result = Json.createObjectBuilder()
                .add("map", map)
                .add("difficulty", difficulty)
                .add("rewards", rewards)
                .add("selectreward", selectreward)
                .build()
                .toString();
        getInstance().dataQueue.offer(new QueueItem("eventreward", result));
    }

    private static void processCellData(Data data) {
        JsonObject json = data.getJsonObject().getJsonObject("api_data");
        if (Objects.isNull(GlobalContext.getMapHpInfo()) || Objects.isNull(GlobalContext.getSortieMap()))
            return;
        Optional<MapHpInfoDto> mapHpInfoOpt = GlobalContext.getMapHpInfo().stream()
                .filter(maphpinfo -> maphpinfo.getMapId() == GlobalContext.getSortieMap().getAreaId()).findFirst();
        if (!mapHpInfoOpt.isPresent())
            return;
        MapHpInfoDto mapHpInfo = mapHpInfoOpt.get();
        int[] map = GlobalContext.getSortieMap().getMap();
        amountOfNodes = json.getJsonArray("api_cell_data").size();
        String result = Json.createObjectBuilder()
                .add("amountOfNodes", amountOfNodes)
                .add("celldata", json.get("api_cell_data"))
                .add("map", map[0] + "-" + map[1])
                .add("cleared", mapHpInfo.getCleared() == 1)
                .add("difficulty", mapHpInfo.getDifficulty())
                .build()
                .toString();
        getInstance().dataQueue.offer(new QueueItem("celldata", result));
    }

    private static int amountOfNodes = 0;
    private static List<Integer> edgeID = new ArrayList<Integer>();

    private static void processNext(Data data) {
        JsonObject json = data.getJsonObject().getJsonObject("api_data");
        MapCellDto sortieMap = GlobalContext.getSortieMap();
        if (Objects.isNull(GlobalContext.getMapHpInfo()) || Objects.isNull(sortieMap))
            return;
        Optional<MapHpInfoDto> mapHpInfoOpt = GlobalContext.getMapHpInfo().stream()
                .filter(maphpinfo -> maphpinfo.getMapId() == sortieMap.getAreaId()).findFirst();
        if (!mapHpInfoOpt.isPresent())
            return;
        MapHpInfoDto mapHpInfo = mapHpInfoOpt.get();
        edgeID.add(json.getInt("api_no"));
        JsonObjectBuilder nodeinfo = Json.createObjectBuilder()
                .add("nodeType", sortieMap.getColorNo())
                .add("eventId", sortieMap.getEventId())
                .add("eventKind", sortieMap.getEventKind())
                .add("nodeColor", sortieMap.getColorNo())
                .add("amountOfNodes", amountOfNodes)
                .add("itemGet",
                        json.getOrDefault("api_itemget", Json.createArrayBuilder().build()));
        if (json.containsKey("api_cell_flavor") && json.getJsonObject("api_cell_flavor").containsKey("api_message")) {
            nodeinfo.add("flavorType", json.getJsonObject("api_cell_flavor").getInt("api_type"))
                    .add("flavorMessage", json.getJsonObject("api_cell_flavor").getString("api_message"));
        }
        boolean[] sortie = GlobalContext.getIsSortie();
        int sortiedFleet;
        for (sortiedFleet = 0; sortiedFleet < sortie.length && !sortie[sortiedFleet]; sortiedFleet++)
            ;
        int[] map = sortieMap.getMap();
        JsonObjectBuilder job = Json.createObjectBuilder()
                .add("map", map[0] + "-" + map[1])
                .add("hqLvl", GlobalContext.hqLevel())
                .add("cleared", mapHpInfo.getCleared() == 1)
                .add("edgeID", toJsonArray(edgeID.stream().mapToInt(i -> i).toArray()))
                .add("nodeInfo", nodeinfo)
                .add("nextRoute", json.getInt("api_next"))
                .add("sortiedFleet", sortiedFleet + 1)
                .add("fleetType", sortiedFleet == 0 ? GlobalContext.getCombined() : 0)
                .add("fleet1", handleFleet(sortiedFleet + 1));
        DockDto dock = GlobalContext.getDock(String.valueOf(sortiedFleet + 1));
        List<ShipDto> s = dock.getShips();
        boolean[] escaped = dock.getEscaped();
        List<ShipDto> ships = new ArrayList<ShipDto>();
        for (int i = 0; i < s.size(); i++) {
            if (Objects.isNull(escaped) || !escaped[i]) {
                ships.add(s.get(i));
            }
        }
        int fleetlevel = 0;
        int fleetSpeed = 9999;
        List<Integer> fleetids = new ArrayList<Integer>();
        double[] los = { 0, 0, 0, 0 };
        SakutekiString fleetlos = new SakutekiString(ships, GlobalContext.hqLevel());
        for (int i = 0; i < los.length; i++) {
            los[i] += fleetlos.getValue(i + 1);
        }
        List<Integer> fleetoneequips = new ArrayList<Integer>();
        List<Integer> fleetoneexslots = new ArrayList<Integer>();
        List<Integer> fleetonetypes = new ArrayList<Integer>();
        for (ShipDto ship : ships) {
            // 0で送信する事象が何故かあるので、そのようなデータがあったら弾いておく
            if (ship.getShipId() == 0) {
                ApplicationMain.main.printMessage("ShipIDが見つからなかったため、TsunDBへの送信をスキップしました");
                return;
            }
            fleetlevel += ship.getLv();
            fleetSpeed = Math.min(fleetSpeed, ship.getParam().getSoku());
            fleetids.add(ship.getShipId());
            for (int i = 0; i < ship.getSlotNum(); i++) {
                ItemDto item = ship.getItem2().get(i);
                fleetoneequips.add(Objects.nonNull(item) ? item.getSlotitemId() : -1);
            }
            fleetoneexslots
                    .add(Objects.nonNull(ship.getSlotExItem()) ? ship.getSlotExItem().getSlotitemId() : -1);
            fleetonetypes.add(ship.getStype());
        }
        job.add("fleetoneequips", toJsonArray(fleetoneequips.stream().mapToInt(i -> i).toArray()))
                .add("fleetoneexslots", toJsonArray(fleetoneexslots.stream().mapToInt(i -> i).toArray()))
                .add("fleetonetypes", toJsonArray(fleetonetypes.stream().mapToInt(i -> i).toArray()));
        if (GlobalContext.isCombined() && sortiedFleet == 0) {
            DockDto dock2 = GlobalContext.getDock(String.valueOf(2));
            List<ShipDto> s2 = dock2.getShips();
            boolean[] escaped2 = dock2.getEscaped();
            List<ShipDto> ships2 = new ArrayList<ShipDto>();
            for (int i = 0; i < s2.size(); i++) {
                if (Objects.isNull(escaped2) || !escaped2[i]) {
                    ships2.add(s2.get(i));
                }
            }
            SakutekiString fleettwolos = new SakutekiString(ships2, GlobalContext.hqLevel());
            for (int i = 0; i < los.length; i++) {
                los[i] += fleettwolos.getValue(i + 1);
            }
            List<Integer> fleettwoequips = new ArrayList<Integer>();
            List<Integer> fleettwoexslots = new ArrayList<Integer>();
            List<Integer> fleettwotypes = new ArrayList<Integer>();
            for (ShipDto ship : ships2) {
                // 0で送信する事象が何故かあるので、そのようなデータがあったら弾いておく
                if (ship.getShipId() == 0) {
                    ApplicationMain.main.printMessage("ShipIDが見つからなかったため、TsunDBへの送信をスキップしました");
                    return;
                }
                fleetlevel += ship.getLv();
                fleetSpeed = Math.min(fleetSpeed, ship.getParam().getSoku());
                fleetids.add(ship.getShipId());
                for (int j = 0; j < ship.getSlotNum(); j++) {
                    ItemDto item = ship.getItem2().get(j);
                    fleettwoequips.add(Objects.nonNull(item) ? item.getSlotitemId() : -1);
                }
                fleettwoexslots
                        .add(Objects.nonNull(ship.getSlotExItem()) ? ship.getSlotExItem().getSlotitemId() : -1);
                fleettwotypes.add(ship.getStype());
            }
            job.add("fleet2", handleFleet(2))
                    .add("fleettwoequips", toJsonArray(fleettwoequips.stream().mapToInt(i -> i).toArray()))
                    .add("fleettwoexslots", toJsonArray(fleettwoexslots.stream().mapToInt(i -> i).toArray()))
                    .add("fleettwotypes", toJsonArray(fleettwotypes.stream().mapToInt(i -> i).toArray()));
        }
        else {
            job.add("fleet2", Json.createArrayBuilder())
                    .add("fleettwoequips", Json.createArrayBuilder())
                    .add("fleettwoexslots", Json.createArrayBuilder())
                    .add("fleettwotypes", Json.createArrayBuilder());
        }
        job.add("fleetlevel", fleetlevel)
                .add("fleetSpeed", fleetSpeed)
                .add("fleetids", toJsonArray(fleetids.stream().mapToInt(i -> i).toArray()))
                .add("los", toJsonArray(los));
        if (json.containsKey("api_eventmap")) {
            JsonObject eventMap = json.getJsonObject("api_eventmap");
            job.add("currentMapHP", eventMap.getInt("api_now_maphp"))
                    .add("maxMapHP", eventMap.getInt("api_max_maphp"))
                    .add("difficulty", mapHpInfo.getDifficulty())
                    .add("gaugeNum", mapHpInfo.getGaugeIndex())
                    .add("gaugeType", mapHpInfo.getGaugeType())
                    // 取るのが面倒くさい
                    .add("debuffSound", -1);
            getInstance().dataQueue.offer(new QueueItem("eventrouting", job.build().toString()));
        }
        else {
            job.addNull("currentMapHp")
                    .addNull("maxMapHP")
                    .addNull("difficulty")
                    .addNull("gaugeNum")
                    .addNull("gaugeType")
                    .addNull("debuffSound");
            getInstance().dataQueue.offer(new QueueItem("routing", job.build().toString()));
        }
    }

    private static void processEnemy(Data data) {
        MapCellDto sortieMap = GlobalContext.getSortieMap();
        if (Objects.isNull(GlobalContext.getMapHpInfo()) || Objects.isNull(sortieMap))
            return;
        Optional<MapHpInfoDto> mapHpInfoOpt = GlobalContext.getMapHpInfo().stream()
                .filter(maphpinfo -> maphpinfo.getMapId() == sortieMap.getAreaId()).findFirst();
        if (!mapHpInfoOpt.isPresent())
            return;
        MapHpInfoDto mapHpInfo = mapHpInfoOpt.get();
        JsonObject json = data.getJsonObject().getJsonObject("api_data");
        int[] map = sortieMap.getMap();
        JsonObjectBuilder enemyComp = Json.createObjectBuilder()
                .add("ship", json.get("api_ship_ke"))
                .add("lvl", json.get("api_ship_lv"))
                .add("hp", json.get("api_e_maxhps"))
                .add("stats",
                        json.containsKey("api_eParam") ? json.get("api_eParam")
                                : Json.createArrayBuilder().build())
                .add("equip", json.get("api_eSlot"))
                .add("formation", json.getJsonArray("api_formation").get(1));
        if (map[0] >= 22) {
            enemyComp.add("mapStats", Json.createObjectBuilder()
                    .add("gaugeNum", mapHpInfo.getGaugeIndex())
                    .add("currentHP", mapHpInfo.getNowMapHp())
                    .add("maxHP", mapHpInfo.getMaxMapHp()));
        }
        if (json.containsKey("api_ship_ke_combined")) {
            enemyComp.add("shipEscort", json.get("api_ship_ke_combined"))
                    .add("lvlEscort", json.get("api_ship_lv_combined"))
                    .add("hpEscort", json.get("api_e_maxhps_combined"))
                    .add("statsEscort", json.get("api_eParam_combined"))
                    .add("equipEscort", json.get("api_eSlot_combined"));
        }
        String result = Json.createObjectBuilder()
                .add("map", map[0] + "-" + map[1])
                .add("node", map[2])
                .add("hqLvl", GlobalContext.hqLevel())
                .add("difficulty", mapHpInfo.getDifficulty())
                .add("enemyComp", enemyComp)
                // やるの面倒
                .addNull("airBattle")
                .build()
                .toString();
        getInstance().dataQueue.offer(new QueueItem("enemy-comp", result));
    }

    private static void processFriendlyFleet(Data data) {
        MapCellDto sortieMap = GlobalContext.getSortieMap();
        if (Objects.isNull(GlobalContext.getMapHpInfo()) || Objects.isNull(sortieMap))
            return;
        Optional<MapHpInfoDto> mapHpInfoOpt = GlobalContext.getMapHpInfo().stream()
                .filter(maphpinfo -> maphpinfo.getMapId() == sortieMap.getAreaId()).findFirst();
        if (!mapHpInfoOpt.isPresent())
            return;
        MapHpInfoDto mapHpInfo = mapHpInfoOpt.get();
        JsonObject json = data.getJsonObject().getJsonObject("api_data");
        if (!json.containsKey("api_friendly_info"))
            return;
        JsonObject friendlyInfo = json.getJsonObject("api_friendly_info");
        int[] map = sortieMap.getMap();
        JsonArray voice = Json.createArrayBuilder()
                .add(friendlyInfo.get("api_voice_id"))
                .add(friendlyInfo.get("api_voice_p_no"))
                .build();
        boolean[] sortie = GlobalContext.getIsSortie();
        int sortiedFleet;
        for (sortiedFleet = 0; sortiedFleet < sortie.length && !sortie[sortiedFleet]; sortiedFleet++)
            ;
        DockDto dock = GlobalContext.getDock(String.valueOf(sortiedFleet + 1));
        List<ShipDto> ships = dock.getShips();
        boolean[] escaped = dock.getEscaped();
        List<Integer> fleet1 = new ArrayList<Integer>();
        List<Integer> fleet2 = new ArrayList<Integer>();
        for (int i = 0; i < ships.size(); i++) {
            if (Objects.isNull(escaped) || !escaped[i]) {
                fleet1.add(ships.get(i).getCharId());
            }
        }
        if (GlobalContext.isCombined() && sortiedFleet == 0) {
            DockDto dock2 = GlobalContext.getDock(String.valueOf(2));
            List<ShipDto> ships2 = dock2.getShips();
            boolean[] escaped2 = dock2.getEscaped();
            for (int i = 0; i < ships2.size(); i++) {
                if (Objects.isNull(escaped2) || !escaped2[i]) {
                    fleet2.add(ships2.get(i).getCharId());
                }
            }
        }
        JsonObjectBuilder fleet = Json.createObjectBuilder()
                .add("ship", friendlyInfo.get("api_ship_id"))
                .add("lvl", friendlyInfo.get("api_ship_lv"))
                .add("hp", friendlyInfo.get("api_maxhps"))
                .add("nowhp", friendlyInfo.get("api_nowhps"))
                .add("stats", friendlyInfo.get("api_Param"))
                .add("equip", friendlyInfo.get("api_Slot"))
                .add("requestType", GlobalContext.getRequestFriendlyFleetType())
                .add("voice", voice)
                .add("fleet1", toJsonArray(fleet1.stream().mapToInt(i -> i).toArray()))
                .add("fleet2", toJsonArray(fleet2.stream().mapToInt(i -> i).toArray()));
        JsonObjectBuilder tmp = Json.createObjectBuilder()
                .add("map", map[0] + "-" + map[1])
                .add("node", map[2])
                .add("difficulty", mapHpInfo.getDifficulty())
                .add("gaugeNum", mapHpInfo.getGaugeIndex())
                .add("variation", friendlyInfo.getInt("api_production_type"))
                .add("fleet", fleet);
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(getInstance().getClass().getResourceAsStream("crc32c.js")))) {
            engine.eval(br.lines().collect(Collectors.joining("")));
            long uniquekey = new BigDecimal(String.valueOf(
                    engine.eval("crc32c(JSON.stringify(" + tmp.build().getJsonObject("fleet").toString() + "))")))
                    .longValue();
            String result = tmp.add("uniquekey", uniquekey).build().toString();
            getInstance().dataQueue.offer(new QueueItem("friendlyfleet", result));
        } catch (ScriptException | IOException e) {
            LOG.get().fatal("スクリプト読込エラー", e);
        }
    }

    private static JsonArray toJsonArray(int[] array) {
        JsonArrayBuilder json = Json.createArrayBuilder();
        for (int v : array) {
            json.add(v);
        }
        return json.build();
    }

    private static JsonArray toJsonArray(double[] array) {
        JsonArrayBuilder json = Json.createArrayBuilder();
        for (double v : array) {
            json.add(v);
        }
        return json.build();
    }

    private static JsonArrayBuilder handleFleet(int id) {
        DockDto dock = GlobalContext.getDock(String.valueOf(id));
        List<ShipDto> ships = dock.getShips();
        boolean[] escaped = dock.getEscaped();
        int i = 0;
        JsonArrayBuilder result = Json.createArrayBuilder();
        for (ShipDto ship : ships) {
            List<ItemDto> item2 = new ArrayList<>(ship.getItem2().subList(0, Math.max(ship.getSlotNum(), 4)));
            item2.add(ship.getSlotExItem());
            result.add(Json.createObjectBuilder()
                    .add("id", ship.getShipId())
                    .add("name", ship.getName())
                    .add("shiplock", ship.getSallyArea())
                    .add("level", ship.getLv())
                    .add("type", ship.getStype())
                    .add("speed", ship.getParam().getSoku())
                    .add("flee", Objects.nonNull(escaped) ? escaped[i++] : false)
                    .add("equip", toJsonArray(
                            ship.getItem2().subList(0, Math.max(ship.getSlotNum(), 4)).stream().mapToInt(item -> {
                                return Objects.nonNull(item) ? item.getSlotitemId() : -1;
                            }).toArray()))
                    .add("stars", toJsonArray(item2.stream().mapToInt(item -> {
                        return Objects.nonNull(item) ? item.getLevel() : 0;
                    }).toArray()))
                    .add("ace", toJsonArray(item2.stream().mapToInt(item -> {
                        return Objects.nonNull(item) && item.isPlane() ? item.getAlv() : -1;
                    }).toArray()))
                    .add("exslot",
                            Objects.nonNull(ship.getSlotExItem()) ? ship.getSlotExItem().getSlotitemId() : -1));
        }
        return result;
    }

    public static synchronized void end() {
        if (Objects.nonNull(instance)) {
            instance.endRequested = true;
            instance.dataQueue.offer(new QueueItem(null, null));
            try {
                instance.join();
                instance = null;
            } catch (InterruptedException e) {
                LOG.get().fatal("TsunDBClientスレッド終了時に何かのエラー", e);
            }
        }
    }

    private final BlockingQueue<QueueItem> dataQueue = new ArrayBlockingQueue<>(32);

    private boolean endRequested = false;

    private int post(String target, String data) {
        try {
            URL url = URI.create("https://tsundb.kc3.moe/api/" + target).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("tsun-ver", "Kasumi Kai");
            con.setRequestProperty("dataorigin", "LogbookEx");
            con.setRequestProperty("version", AppConstants.VERSION);
            con.setConnectTimeout(5000);
            con.connect();
            OutputStream os = con.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(data);
            writer.flush();
            writer.close();
            os.close();
            return con.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public void run() {
        try {
            int skipCount = 0;
            int errorCount = 0;

            while (true) {
                final QueueItem queue = this.dataQueue.take();
                final String target = queue.target;
                final String data = queue.data;
                if (this.endRequested)
                    return;
                if (skipCount > 0) {
                    --skipCount;
                    continue;
                }
                for (int retly = 0;; ++retly) {
                    boolean error = false;
                    try {
                        int code = post(target, data);
                        if (this.endRequested)
                            return;
                        if (code == HttpURLConnection.HTTP_OK) {
                            // 成功したらエラーカウンタをリセット
                            skipCount = errorCount = 0;
                            // ログに出す
                            if (AppConfig.get().isTsunDBSendLog()) {
                                Display.getDefault().asyncExec(() -> {
                                    try {
                                        if (!ApplicationMain.main.getShell().isDisposed()) {
                                            ApplicationMain.main.printMessage("TsunDBへ送信(" + target + ")");
                                        }
                                    } catch (Exception e) {
                                        LOG.get().warn("TsunDB送信でエラー", e);
                                    }
                                });
                            }
                            break;
                        }
                        else if (code == HttpURLConnection.HTTP_BAD_REQUEST)
                            break;
                        error = true;
                    } catch (Exception e) {
                        error = true;
                    }
                    if (error) {
                        // 少し時間をおいてリトライ
                        Thread.sleep(1000);
                        if (this.endRequested)
                            return;
                        if (retly >= 1) {
                            // リトライが多すぎたらエラーにする
                            skipCount = (errorCount++) * 4;
                            LOG.get().warn("TsunDBへ送信失敗");
                            if (skipCount > 0) {
                                LOG.get().warn("以降 " + skipCount + " 個の送信をスキップします.");
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (!this.endRequested) {
                LOG.get().fatal("スレッドが異常終了しました", e);
            }
        }
    }
}
