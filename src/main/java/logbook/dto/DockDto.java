package logbook.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import logbook.data.context.GlobalContext;
import logbook.internal.NosakiTimer;

import com.dyuproject.protostuff.Tag;

/**
 * 艦隊のドックを表します
 *
 */
public final class DockDto extends AbstractDto {

    /** ドックID */
    @Tag(1)
    private final String id;

    /** 艦隊名 */
    @Tag(2)
    private final String name;

    /** 艦娘達 */
    @Tag(3)
    private final List<ShipDto> ships = new ArrayList<ShipDto>();

    /** 退避したか？対比した艦娘がいるときは長さ6の配列 */
    @Tag(4)
    private boolean[] escaped = null;

    /** 更新フラグ */
    private transient boolean update;

    /**
     * コンストラクター
     */
    public DockDto(String id, String name, DockDto oldDock) {
        this.id = id;
        this.name = name;
        if (oldDock != null) {
            this.escaped = oldDock.getEscaped();
        }
    }

    /**
     * ドックIDを取得します。
     * 
     * @return ドックID
     */
    public String getId() {
        return this.id;
    }

    /**
     * 艦娘を艦隊に追加します
     * 艦娘の艦隊所属情報も更新します
     * 
     * @param ship
     */
    public void addShip(ShipDto ship) {
        this.ships.add(ship);
    }

    /**
     * 艦隊から艦娘を削除します
     * 
     * @param ship
     */
    public void removeShip(ShipDto ship) {
        int index = this.ships.indexOf(ship);
        if (index != -1) {
            this.ships.remove(index);
        }
    }

    /**
     * 艦隊の艦娘を入れ替えます
     * 
     * @param index
     * @param newShip
     */
    public void setShip(int index, ShipDto newShip) {
        this.ships.set(index, newShip);
    }

    /**
     * 旗艦以外を外します
     */
    public void removeExceptFlagship() {
        while (this.ships.size() > 1) {
            this.ships.remove(this.ships.size() - 1);
        }
    }

    /**
     * 艦隊名を取得します。
     * 
     * @return 艦隊名
     */
    public String getName() {
        return this.name;
    }

    /**
     * 艦娘達を取得します。
     * 
     * @return 艦娘達
     */
    public List<ShipDto> getShips() {
        return Collections.unmodifiableList(this.ships);
    }

    /**
     * 更新フラグを取得します。
     * 
     * @return 更新フラグ
     */
    public boolean isUpdate() {
        return this.update;
    }

    /**
     * 更新フラグを設定します。
     * 
     * @param update 更新フラグ
     */
    public void setUpdate(boolean update) {
        this.update = update;
    }

    public void removeFleetIdFromShips() {
        for (int i = 0; i < this.ships.size(); i++) {
            this.ships.get(i).setFleetid("");
        }
    }

    public void updateFleetIdOfShips() {
        for (int i = 0; i < this.ships.size(); i++) {
            this.ships.get(i).setFleetid(this.id);
            this.ships.get(i).setFleetpos(i);
        }
    }

    /**
     * 大破艦がいるか？を取得します
     * 
     * @return 大破艦がいるか？
     */
    public boolean isBadlyDamaged() {
        for (int i = 0; i < this.ships.size(); ++i) {
            if ((this.escaped != null) && this.escaped[i]) {
                continue; // 退避した艦はカウントしない
            }
            ShipDto ship = this.ships.get(i);
            if (ship.isBadlyDamage()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 退避したか？
     * 退避した艦娘がいるときは長さ6の配列
     * 連合艦隊でない場合はnull
     * 
     * @return escaped
     */
    public boolean[] getEscaped() {
        return this.escaped;
    }

    /**
     * @param escaped セットする escaped
     */
    public void setEscaped(boolean[] escaped) {
        this.escaped = escaped;
    }

    /**
     * 指定位置の艦娘を取得します。編成に存在しない場合は空
     *
     * @param index 0始まりの艦隊内位置
     * @return
     */
    private Optional<ShipDto> shipAt(int index) {
        if ((index < 0) || (index >= this.ships.size())) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.ships.get(index));
    }

    /**
     * 旗艦が工作艦か？
     *
     * @return
     */
    public boolean isFlagshipAkashi() {
        return this.shipAt(0)
                .filter(ship -> ship.getStype() == 19)
                .isPresent();
    }

    /**
     * 泊地修理可能艦数
     * 
     * @return
     */
    public int getAkashiCapacity() {
        if (!this.isFlagshipAkashi()) {
            // 旗艦が工作艦でない
            return 0;
        }
        Optional<ShipDto> flagship = this.shipAt(0);
        Optional<ShipDto> secondShip = this.shipAt(1);
        int numRepairShips = 0;
        if (flagship.filter(ship -> ship.getName().startsWith("明石")).isPresent() ||
                secondShip.filter(ship -> ship.getName().startsWith("明石")).isPresent()) {
            numRepairShips += 2;
        }
        numRepairShips += flagship.map(ShipDto::getItem)
                .map(items -> items.stream().filter(item -> item.getName().equals("艦艇修理施設")).count()).orElse(0L) +
                secondShip.map(ShipDto::getItem)
                        .map(items -> items.stream().filter(item -> item.getName().equals("艦艇修理施設")).count())
                        .orElse(0L);

        return numRepairShips;
    }

    /**
     * 泊地修理可能な編成か
     * 
     * @return
     */
    public boolean isAkashiRepairEnabled() {
        int reapairCapacity = this.getAkashiCapacity();
        for (int p = 0; p < this.ships.size(); ++p) {
            ShipDto ship = this.ships.get(p);
            if ((p < reapairCapacity) && // 泊地修理範囲
                    !GlobalContext.isNdock(ship) && // 入渠中でない
                    !ship.isHalfDamage() && // 中破以上でない
                    (ship.getNowhp() != ship.getMaxhp())) // 無傷でない
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 旗艦または2番艦に配置された母港給糧艦(野埼/野埼改)を取得します。
     * いない場合はnull
     *
     * @return
     */
    public ShipDto getNosakiSupplyShip() {
        for (int p = 0; p < 2; ++p) {
            Optional<ShipDto> ship = this.shipAt(p);
            if (ship.filter(NosakiTimer::isSupplyShip).isPresent()) {
                return ship.get();
            }
        }
        return null;
    }

    /**
     * 旗艦または2番艦が野埼か？
     *
     * @return
     */
    public boolean isFlagshipOrSecondShipNosaki() {
        return this.getNosakiSupplyShip() != null;
    }

    /**
     * 母港給糧が発動する編成か
     *
     * @return
     */
    public boolean isNosakiSupplyEnabled() {
        ShipDto supplyShip = this.getNosakiSupplyShip();
        if (supplyShip == null) {
            // 旗艦・2番艦に野埼がいない
            return false;
        }
        if (GlobalContext.isMission(this.id)) {
            // 遠征中
            return false;
        }
        if ((supplyShip.getCond() < NosakiTimer.REQUIRED_SUPPLY_SHIP_COND) || // cond不足
                supplyShip.isSlightDamage() || // 小破以上
                (supplyShip.getFuel() < supplyShip.getFuelMax()) || // 燃料未補給
                (supplyShip.getBull() < supplyShip.getBullMax()) || // 弾薬未補給
                GlobalContext.isNdock(supplyShip)) // 入渠中
        {
            return false;
        }
        // cond値が上がる随伴艦がいるか
        for (ShipDto ship : this.ships) {
            if ((ship == null) || NosakiTimer.isSupplyShip(ship)) {
                continue; // 給糧艦自身は対象外
            }
            if (!GlobalContext.isNdock(ship) && (ship.getCond() < NosakiTimer.MAX_SUPPLY_COND)) {
                return true;
            }
        }
        return false;
    }
}
