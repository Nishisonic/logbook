/**
 * 
 */
package logbook.data.context;

import java.util.Date;

import logbook.dto.DockDto;
import logbook.internal.AkashiTimer;
import logbook.internal.NosakiTimer;

/**
 * リクエストデータによる更新ではなく、タイマーによる更新を行うデータ
 * @author Nekopanda
 */
public class TimerContext {

    private final static TimerContext instance = new TimerContext();

    private final AkashiTimer.RepairState[] akashiRepairStates = new AkashiTimer.RepairState[4];

    private final NosakiTimer.SupplyState[] nosakiSupplyStates = new NosakiTimer.SupplyState[4];

    private Date lastUpdated = new Date();

    public static TimerContext get() {
        return instance;
    }

    public void update() {
        this.lastUpdated = new Date();

        AkashiTimer akashiTimer = GlobalContext.getAkashiTimer();
        NosakiTimer nosakiTimer = GlobalContext.getNosakiTimer();

        for (int i = 0; i < 4; i++) {
            DockDto dock = GlobalContext.getDock(String.valueOf(i + 1));
            AkashiTimer.RepairState repairState = null;
            NosakiTimer.SupplyState supplyState = null;
            if (dock != null) {
                repairState = akashiTimer.update(dock, this.lastUpdated);
                supplyState = nosakiTimer.update(dock, this.lastUpdated);
            }
            this.akashiRepairStates[i] = repairState;
            this.nosakiSupplyStates[i] = supplyState;
        }
    }

    public Date getLastUpdated() {
        return this.lastUpdated;
    }

    public AkashiTimer.RepairState getAkashiRepairState(int index) {
        return this.akashiRepairStates[index];
    }

    public NosakiTimer.SupplyState getNosakiSupplyState(int index) {
        return this.nosakiSupplyStates[index];
    }
}
