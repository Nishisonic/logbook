package logbook.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import logbook.config.AppConfig;
import logbook.constants.AppConstants;
import logbook.data.Data;
import logbook.data.DataType;
import logbook.gui.logic.CreateReportLogic;
import logbook.gui.logic.TableItemCreator;
import logbook.internal.ItemCategoryType;
import logbook.internal.ItemIconType;
import logbook.scripting.TableItemCreatorProxy;
import logbook.util.SwtUtils;

/**
 * 所有装備一覧
 *
 */
public final class ItemTable extends AbstractTableDialog {
    private Combo itemtext;
    private String SelectedEquipType = "全て";

    /**
     * @param parent
     */
    public ItemTable(Shell parent, MenuItem menuItem) {
        super(parent, menuItem);
    }

    @Override
    protected void createContentsBefore() {
        GridLayout shellLayout = new GridLayout(1, false);
        shellLayout.verticalSpacing = 1;
        shellLayout.marginWidth = 1;
        shellLayout.marginHeight = 1;
        shellLayout.marginBottom = 1;
        shellLayout.horizontalSpacing = 1;
        this.shell.setLayout(shellLayout);
        this.itemtext = new Combo(this.shell, SWT.DROP_DOWN | SWT.READ_ONLY);
        int method = AppConfig.get().getItemFilterMethod();
        switch (method) {
        case 0:
            List<String> itemIconTypeNames = new ArrayList<>(ItemIconType.getAll().values());
            itemIconTypeNames.add(0, "全て");
            this.itemtext.setItems(itemIconTypeNames.toArray(new String[0]));
            break;
        case 1:
            List<String> itemCategoryTypeNames = new ArrayList<>(ItemCategoryType.getAll().values());
            itemCategoryTypeNames.add(0, "全て");
            this.itemtext.setItems(itemCategoryTypeNames.toArray(new String[0]));
            break;
        }
        this.itemtext.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ItemTable.this.SelectedEquipType = ItemTable.this.itemtext.getText();
                ItemTable.this.updateTableBody();
                ItemTable.this.reloadTable();
            }
        });
    }

    @Override
    protected void createContents() {
        this.table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        MenuItem itemCopy = new MenuItem(this.tablemenu, SWT.NONE);
        itemCopy.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StringBuilder sb = new StringBuilder();
                for (TableItem item : ItemTable.this.table.getSelection()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(item.getText(1));
                }
                Clipboard clipboard = new Clipboard(Display.getDefault());
                clipboard.setContents(new Object[] { sb.toString() }, new Transfer[] { TextTransfer.getInstance() });
            }
        });
        itemCopy.setText("装備名をコピー(&1)");
    }

    @Override
    protected String getTitleMain() {
        return "所有装備一覧";
    }

    @Override
    protected Point getSize() {
        return SwtUtils.DPIAwareSize(new Point(600, 350));
    }

    @Override
    protected String[] getTableHeader() {
        return CreateReportLogic.getItemListHeader();
    }

    @Override
    protected void updateTableBody() {
        this.body = CreateReportLogic.getItemListBody(this.SelectedEquipType);
    }

    @Override
    protected TableItemCreator getTableItemCreator() {
        return TableItemCreatorProxy.get(AppConstants.ITEMTABLE_PREFIX);
    }

    /**
     * 更新する必要のあるデータ
     */
    @SuppressWarnings("incomplete-switch")
    @Override
    public void update(DataType type, Data data) {
        switch (type) {
        case CHANGE:
        case PORT:
        case SHIP2:
        case SHIP3:
        case SLOTITEM_MEMBER:
        case GET_SHIP:
        case DESTROY_SHIP:
        case DESTROY_ITEM2:
        case POWERUP:
        case LOCK_SLOTITEM:
        case REMODEL_SLOT:
            this.needsUpdate = true;
        }
    }
}
