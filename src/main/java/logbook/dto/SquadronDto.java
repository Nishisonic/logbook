package logbook.dto;

import java.util.Objects;

import javax.json.JsonObject;

import com.dyuproject.protostuff.Tag;

public class SquadronDto extends ItemDto {

    @Tag(101)
    private int squadronId;
    
    @Tag(102)
    private int state;
    
    @Tag(103)
    private int count;
    
    @Tag(104)
    private int maxCount;
    
    @Tag(105)
    private int cond;

    public SquadronDto(ItemDto item, JsonObject json) {
        super(Objects.nonNull(item) ? item.getInfo() : new ItemInfoDto(), json.getInt("api_slotid"));
        if (Objects.nonNull(item)) {
            super.setLocked(item.isLocked());
            super.setLevel(item.getLevel());
            super.setAlv(item.getAlv());
            this.count = json.getInt("api_count");
            this.maxCount = json.getInt("api_max_count");
            this.cond = json.getInt("api_cond");
        }
        this.squadronId = json.getInt("api_squadron_id");
        this.state = json.getInt("api_state");
    }

    public SquadronDto(ItemDto item, int squadronId, int state, int count, int maxCount, int cond) {
        super(Objects.nonNull(item) ? item.getInfo() : new ItemInfoDto(), squadronId);
        if (Objects.nonNull(item)) {
            super.setLocked(item.isLocked());
            super.setLevel(item.getLevel());
            super.setAlv(item.getAlv());
        }
        this.squadronId = squadronId;
        this.state = state;
        this.count = count;
        this.maxCount = maxCount;
        this.cond = cond;
    }

    public int getSquadronId() {
        return squadronId;
    }

    public void setSquadronId(int squadronId) {
        this.squadronId = squadronId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public int getCond() {
        return cond;
    }

    public void setCond(int cond) {
        this.cond = cond;
    }

    public boolean isNotEmptySlot() {
        return super.getSlotitemId() > 0;
    }
}
