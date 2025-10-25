package logbook.dto;

import java.util.HashMap;
import java.util.Map;

import com.dyuproject.protostuff.Tag;

public class StartAirbaseDto {

    @Tag(1)
    private Map<Integer, int[]> strikePoint = new HashMap<>();

    public int[] getStrikePoint(int i) {
        return this.strikePoint.get(i);
    }

    public void setStrikePoint(int i,int[] strikePoint) {
        this.strikePoint.put(i, strikePoint);
    }

    public void clearStrikePoint() {
        this.strikePoint.clear();
    }
}
