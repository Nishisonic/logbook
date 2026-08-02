/**
 *
 */
package logbook.internal;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logbook.data.context.GlobalContext;
import logbook.dto.DockDto;
import logbook.dto.ShipDto;

/**
 * 母港給糧艦タイマー
 *
 * 野埼/野埼改を旗艦または2番艦に配置した艦隊は、15分経過後に母港へ戻ると随伴艦のcond値が上昇する。
 * 泊地修理と違いHPは回復せず、また15分を超えて放置しても発動は1回だけで超過分は無駄になる。
 */
public class NosakiTimer {

    /** 野埼 */
    public static final int NOSAKI_SHIP_ID = 996;

    /** 野埼改 */
    public static final int NOSAKI_KAI_SHIP_ID = 1002;

    /** 給糧が発動するまでの時間 */
    public static final long SUPPLY_INTERVAL = 15 * 60 * 1000;

    /** 給糧で到達できるcond値の上限 */
    public static final int MAX_SUPPLY_COND = 54;

    /** 給糧艦自身に必要なcond値 */
    public static final int REQUIRED_SUPPLY_SHIP_COND = 30;

    /** 野埼のcond上昇量 */
    private static final int SUPPLY_POWER = 2;

    /** 野埼改のcond上昇量 */
    private static final int SUPPLY_POWER_KAI = 3;

    private Date startTime = null;

    /** ドックID -> 前回更新時点の経過時間 */
    private final Map<String, Long> lastElapsedMap = new HashMap<>();

    /**
     * 母港給糧艦(野埼/野埼改)か？
     * @param ship
     * @return
     */
    public static boolean isSupplyShip(ShipDto ship) {
        if (ship == null) {
            return false;
        }
        int shipId = ship.getShipId();
        return (shipId == NOSAKI_SHIP_ID) || (shipId == NOSAKI_KAI_SHIP_ID);
    }

    /**
     * 1回の給糧で上昇するcond値
     * @param supplyShip 給糧艦
     * @return
     */
    public static int getSupplyPower(ShipDto supplyShip) {
        return (supplyShip.getShipId() == NOSAKI_KAI_SHIP_ID) ? SUPPLY_POWER_KAI : SUPPLY_POWER;
    }

    public static class SupplyState {
        private final ShipState[] ships;
        private final long elapsed;
        private final long next;
        private final boolean readyNotify;
        private final int power;

        SupplyState(ShipState[] ships, long elapsed, long next, boolean readyNotify, int power) {
            this.ships = ships;
            this.elapsed = elapsed;
            this.next = next;
            this.readyNotify = readyNotify;
            this.power = power;
        }

        /**
         * 母港給糧艦編成として稼働中か？
         * @return
         */
        public boolean isSupplying() {
            return this.ships != null;
        }

        public List<ShipState> get() {
            if (this.ships == null) {
                throw new IllegalStateException("母港給糧艦編成ではありません");
            }
            return Arrays.asList(this.ships);
        }

        /**
         * 給糧待ちが完了しているか？(母港に戻れば発動する)
         * @return
         */
        public boolean isReady() {
            return this.next <= 0;
        }

        /**
         * 今回の更新で給糧可能になったか？
         * @return
         */
        public boolean isReadyNotify() {
            return this.readyNotify;
        }

        /**
         * @return elapsed
         */
        public long getElapsed() {
            return this.elapsed;
        }

        /**
         * 給糧発動までの残り時間
         * @return
         */
        public long getNext() {
            return this.next;
        }

        /**
         * 1回の給糧で上昇するcond値(艦艇修理施設等によるcap前の基礎値)
         * @return
         */
        public int getPower() {
            return this.power;
        }
    }

    public static class ShipState {
        private ShipDto ship;
        private int gain;

        /**
         * 母港に戻ると上昇するcond値
         * @return
         */
        public int getGain() {
            return this.gain;
        }

        /**
         * @return ship
         */
        public ShipDto getShip() {
            return this.ship;
        }

        /**
         * @param ship セットする ship
         */
        public void setShip(ShipDto ship) {
            this.ship = ship;
        }
    }

    public void reset() {
        this.startTime = new Date();
        this.lastElapsedMap.clear();
    }

    /**
     * 母港に戻った時に呼ぶ。15分経過していれば給糧が発動しているのでタイマーを回し直す
     */
    public void enterPort() {
        if (this.startTime == null) {
            this.reset();
            return;
        }
        long elapsed = new Date().getTime() - this.startTime.getTime();
        if (elapsed >= SUPPLY_INTERVAL) {
            this.reset();
        }
    }

    public Date getStartTime() {
        return this.startTime;
    }

    /**
     * @param startTime セットする startTime
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /** 状態を更新 */
    public SupplyState update(DockDto dock, Date now) {
        ShipDto supplyShip = dock.getNosakiSupplyShip();
        if ((this.startTime == null) || (supplyShip == null) || !dock.isNosakiSupplyEnabled()) {
            this.lastElapsedMap.remove(dock.getId());
            return new SupplyState(null, 0, 0, false, 0); // ショートカット
        }
        long elapsed = now.getTime() - this.startTime.getTime();
        long next = Math.max(0, SUPPLY_INTERVAL - elapsed);
        int power = getSupplyPower(supplyShip);

        List<ShipDto> ships = dock.getShips();
        ShipState[] states = new ShipState[ships.size()];

        for (int p = 0; p < ships.size(); ++p) {
            ShipDto ship = ships.get(p);
            if (isSupplyShip(ship)) {
                continue; // 給糧艦自身は対象外
            }
            if (GlobalContext.isNdock(ship)) {
                continue; // 入渠中は対象外
            }
            if (ship.getCond() >= MAX_SUPPLY_COND) {
                continue; // これ以上上がらない
            }
            ShipState state = new ShipState();
            state.setShip(ship);
            state.gain = Math.min(power, MAX_SUPPLY_COND - ship.getCond());
            states[p] = state;
        }

        Long lastElapsed = this.lastElapsedMap.put(dock.getId(), elapsed);
        boolean readyNotify = (elapsed >= SUPPLY_INTERVAL)
                && (lastElapsed != null) && (lastElapsed < SUPPLY_INTERVAL);

        return new SupplyState(states, elapsed, next, readyNotify, power);
    }
}
