package logbook.gui.background;

import java.io.StringReader;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import logbook.constants.AppConstants;

import org.apache.commons.io.IOUtils;

/**
 * アップデートチェックを行います
 *
 */
public final class AsyncExecUpdateCheck extends Thread {

    public static interface UpdateResult {
        void onSuccess(String version);

        void onError(Exception e);
    }

    private final UpdateResult handler;

    /**
     * コンストラクター
     * 
     * @param handler
     */
    public AsyncExecUpdateCheck(UpdateResult handler) {
        this.handler = handler;
        this.setName("logbook_async_exec_update_check");
    }

    @Override
    public void run() {
        try {
            String jsonString = IOUtils.toString(AppConstants.UPDATE_CHECK_URI, Charset.forName("UTF-8"));
            JsonReader json = Json.createReader(new StringReader(jsonString));
            JsonObject api = json.readObject();
            String version = api.getString("tag_name").replace("v", "");
            this.handler.onSuccess(version);
        } catch (Exception e) {
            this.handler.onError(e);
        }
    }
}
