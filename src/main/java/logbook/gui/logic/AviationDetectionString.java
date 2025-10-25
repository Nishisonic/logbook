package logbook.gui.logic;
import java.util.List;
import java.util.Objects;

import logbook.dto.ItemDto;
import logbook.dto.ShipDto;

public class AviationDetectionString {
    private double value = 0;

    public AviationDetectionString(List<ShipDto> ships) {
        this.value = ships.stream().mapToDouble(ship -> {
            double sum = 0;
            int[] slots = ship.getOnSlot();
            List<ItemDto> items = ship.getItem2();
            for (int i = 0; i < ship.getSlotNum(); i++) {
                ItemDto item = items.get(i);
                if (Objects.nonNull(item)) {
                    switch (item.getType2()) {
                    case 10: // 水上偵察機
                    case 11: // 水上爆撃機
                        sum += item.getParam().getSaku() * Math.sqrt(Math.sqrt(slots[i]));
                        break;
                    case 41: // 大型飛行艇
                        sum += item.getParam().getSaku() * Math.sqrt(slots[i]);
                        break;
                    }
                }
            }
            return sum;
        }).sum();
    }

    public double getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        double small = 0.00000000000001;
        return String.format("%.1f (6-3:35.2◎確)",
                (Math.floor((this.value + small) * 1000) + 0.1) / 1000.0
                // this.value >= 26.4 ? "◎確" : this.value >= 19.2 ? "○~◎" : this.value >= 12 ? "○確" : "失敗",
                // this.value >= 35.2 ? "◎確" : this.value >= 25.6 ? "○~◎" : this.value >= 16 ? "○確" : "失敗"
                );
    }
}
