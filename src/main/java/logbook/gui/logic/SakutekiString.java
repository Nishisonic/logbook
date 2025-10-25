package logbook.gui.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import logbook.config.AppConfig;
import logbook.dto.ItemDto;
import logbook.dto.ShipBaseDto;

/**
 * @author Nekopanda
 *
 */
public class SakutekiString implements Comparable<SakutekiString> {
    private double f33TotalShipLoS = 0;
    private double f33TotalItemLoS = 0;
    private double hqLvLoS = 0;
    private double spaceLoS = 0;
    private boolean losFailed = false;

    private static class ShipParam {
        public double shipLoS;
        public double itemLoS;
        private ShipBaseDto ship;
        private List<ItemDto> items;
        private boolean losFailed;

        <SHIP extends ShipBaseDto> ShipParam(SHIP ship) {
            this.ship = ship;
            this.items = new ArrayList<>(ship.getItem2());
            this.items.add(ship.getSlotExItem());
            int itemParamLoS = this.items.stream().filter(Objects::nonNull)
                    .mapToInt(item -> item.getParam().getSakuteki())
                    .sum();

            this.shipLoS = Math.sqrt(this.ship.getSakuteki() - itemParamLoS);
            this.itemLoS = items.stream().filter(Objects::nonNull).mapToDouble(item -> {
                int los = item.getParam().getSaku();
                int lv = item.getLevel();
                switch (item.getType2()) {
                // 艦上攻撃機
                case 8:
                    return 0.8 * los;
                // 艦上偵察機
                case 9:
                    return 1.0 * (los + 1.2 * Math.sqrt(lv));
                // 水上偵察機
                case 10:
                    return 1.2 * (los + 1.2 * Math.sqrt(lv));
                // 水上爆撃機
                case 11:
                    return 1.1 * (los + 1.15 * Math.sqrt(lv));
                // 小型電探
                case 12:
                    return 0.6 * (los + 1.25 * Math.sqrt(lv));
                // 大型電探
                case 13:
                    return 0.6 * (los + 1.4 * Math.sqrt(lv));
                }
                return 0.6 * los;
            }).sum();

            this.losFailed = items.stream().filter(Objects::nonNull).anyMatch(item -> item.getType1() == 0);
        }

        public double getItemLoS() {
            return this.itemLoS;
        }

        public double getShipLoS() {
            return this.shipLoS;
        }

        public boolean isLosFailed() {
            return this.losFailed;
        }
    }

    public <SHIP extends ShipBaseDto> SakutekiString(List<SHIP> ships, int hqLv) {
        List<ShipParam> shipParams = ships.stream().map(ShipParam::new).collect(Collectors.toList());
        this.f33TotalShipLoS = shipParams.stream().mapToDouble(ShipParam::getShipLoS).sum();
        this.f33TotalItemLoS = shipParams.stream().mapToDouble(ShipParam::getItemLoS).sum();
        this.losFailed = shipParams.stream().anyMatch(ShipParam::isLosFailed);

        this.hqLvLoS = -Math.ceil(hqLv * 0.4);
        this.spaceLoS = 2 * (6 - shipParams.size());
    }

    public <SHIP extends ShipBaseDto> SakutekiString(SHIP ship) {
        ShipParam param = new ShipParam(ship);
        this.f33TotalShipLoS = param.getShipLoS();
        this.f33TotalItemLoS = param.getItemLoS();
        this.losFailed = param.isLosFailed();

        this.hqLvLoS = this.spaceLoS = 0;
    }

    public double getValue() {
        double cn = AppConfig.get().getBunkitenKeisu();
        return this.getValue(cn);
    }

    public double getValue(double cn) {
        return this.f33TotalShipLoS + this.f33TotalItemLoS * cn + hqLvLoS + spaceLoS;
    }

    @Override
    public String toString() {
        int method = AppConfig.get().getSakutekiMethodV4();
        double cn = AppConfig.get().getBunkitenKeisu();
        if (method != 0 && this.losFailed) {
            return "<ゲーム画面をリロードしてください>";
        }
        double small = 0.00000000000001;
        switch (method) {
        case 1: // 判定式(33)(艦素索敵分 + 装備分 + 提督Lv分 + 艦隊空き数分)
            return String.format("%.3f (%.3f%+.3f(%.1f)%+.1f%+.1f)",
                    (Math.floor((this.getValue() + small) * 1000) + 0.1) / 1000.0,
                    (Math.floor((this.f33TotalShipLoS + small) * 1000) + 0.1) / 1000.0,
                    (Math.floor((this.f33TotalItemLoS + small) * 1000) + 0.1) / 1000.0,
                    (Math.floor((cn + small) * 10) + 0.1) / 10.0,
                    this.hqLvLoS,
                    this.spaceLoS);
        case 2: // 判定式(33)
            return String.format("%.3f(1) / %.3f(2) / %.3f(3) / %.3f(4) / %.3f(5)",
                    (Math.floor((this.getValue(1) + small) * 1000) + 0.1) / 1000.0,
                    (Math.floor((this.getValue(2) + small) * 1000) + 0.1) / 1000.0,
                    (Math.floor((this.getValue(3) + small) * 1000) + 0.1) / 1000.0,
                    (Math.floor((this.getValue(4) + small) * 1000) + 0.1) / 1000.0,
                    (Math.floor((this.getValue(5) + small) * 1000) + 0.1) / 1000.0);
        }
        return String.format("%.3f (%.1f)",
                (Math.floor((this.getValue() + small) * 1000) + 0.1) / 1000.0,
                (Math.floor((cn + small) * 10) + 0.1) / 10.0);
    }

    @Override
    public int compareTo(SakutekiString o) {
        return Double.compare(this.getValue(), o.getValue());
    }
}
