package logbook.dto;

import javax.json.JsonObject;

import com.dyuproject.protostuff.Tag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AirbaseDto {

    @Tag(1)
    private int areaId;

    @Tag(2)
    private int rid;

    @Tag(3)
    private String name;

    @Tag(4)
    private int distanceBase;

    @Tag(5)
    private int distanceBonus;

    @Tag(6)
    private int actionKind;

    @Tag(7)
    private List<SquadronDto> planeInfos;

    public AirbaseDto(JsonObject json, Map<Integer, ItemDto> itemMap) {
        this.areaId = json.getInt("api_area_id");
        this.rid = json.getInt("api_rid");
        this.name = json.getString("api_name");
        this.distanceBase = json.getJsonObject("api_distance").getInt("api_base");
        this.distanceBonus = json.getJsonObject("api_distance").getInt("api_bonus");
        this.actionKind = json.getInt("api_action_kind");
        this.planeInfos = json.getJsonArray("api_plane_info").stream()
            .map(JsonObject.class::cast)
            .map(planeInfo -> new SquadronDto(itemMap.get(planeInfo.getInt("api_slotid")), planeInfo))
            .collect(Collectors.toList());
    }

    public AirbaseDto(int areaId, int rid, String name, int distanceBase, int distanceBonus, int actionKind, List<SquadronDto> planeInfos) {
        this.areaId = areaId;
        this.rid = rid;
        this.name = name;
        this.distanceBase = distanceBase;
        this.distanceBonus = distanceBonus;
        this.actionKind = actionKind;
        this.planeInfos = planeInfos;
    }

    public String toActionKindString() {
        switch (this.actionKind) {
        case 0:
            return "待機";
        case 1:
            return "出撃";
        case 2:
            return "防空";
        case 3:
            return "退避";
        case 4:
            return "休息";
        default:
            return "不明";
        }
    }

    public int getAreaId() {
        return this.areaId;
    }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    public int getRid() {
        return this.rid;
    }

    public void setRid(int rid) {
        this.rid = rid;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDistanceBase() {
        return this.distanceBase;
    }

    public void setDistanceBase(int distanceBase) {
        this.distanceBase = distanceBase;
    }

    public int getDistanceBonus() {
        return this.distanceBonus;
    }

    public void setDistanceBonus(int distanceBonus) {
        this.distanceBonus = distanceBonus;
    }

    public int getDistance() {
        return this.distanceBase + this.distanceBonus;
    }

    public int getActionKind() {
        return this.actionKind;
    }

    public void setActionKind(int actionKind) {
        this.actionKind = actionKind;
    }

    public List<SquadronDto> getPlaneInfos() {
        return this.planeInfos;
    }

    public void setPlaneInfos(List<SquadronDto> planeInfos) {
        this.planeInfos = planeInfos;
    }
}
