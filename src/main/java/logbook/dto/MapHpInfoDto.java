package logbook.dto;

public class MapHpInfoDto {

    private int mapId;

    private int difficulty;

    private int cleared;

    private int defeatCount;

    private int requiredDefeatCount;

    private int nowMapHp;

    private int maxMapHp;

    private int gaugeIndex;

    private int gaugeType;

    public MapHpInfoDto(int mapId, int difficulty, int cleared, int defeatCount, int requiredDefeatCount, int nowMapHp, int maxMapHp, int gaugeIndex, int gaugeType) {
        this.mapId = mapId;
        this.difficulty = difficulty;
        this.cleared = cleared;
        this.defeatCount = defeatCount;
        this.requiredDefeatCount = requiredDefeatCount;
        this.nowMapHp = nowMapHp;
        this.maxMapHp = maxMapHp;
        this.gaugeIndex = gaugeIndex;
        this.gaugeType = gaugeType;
    }

    public int getMapId() {
        return this.mapId;
    }

    public int getDifficulty() {
        return this.difficulty;
    }

    public int getCleared() {
        return this.cleared;
    }

    public String getDifficultyString() {
        switch(this.difficulty) {
            case 1: return "丁";
            case 2: return "丙";
            case 3: return "乙";
            case 4: return "甲";
        }
        return "？";
    }

    public int getDefeatCount() {
        return this.defeatCount;
    }

    public int getRequiredDefeatCount() {
        return this.requiredDefeatCount;
    }

    public int getNowMapHp() {
        return this.nowMapHp;
    }

    public int getMaxMapHp() {
        return this.maxMapHp;
    }

    public int getGaugeIndex() {
        return this.gaugeIndex;
    }

    public int getGaugeType() {
        return this.gaugeType;
    }
}
