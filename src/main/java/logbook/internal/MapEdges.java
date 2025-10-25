/**
 * 
 */
package logbook.internal;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.commons.io.IOUtils;

import logbook.constants.AppConstants;

public class MapEdges {

    private static Map<String, Map<String, String[]>> edges = new HashMap<>();

    public static void load() throws IOException {
        String jsonString = IOUtils.toString(AppConstants.MAP_EDGES_URI, Charset.forName("UTF-8"));
        JsonReader json = Json.createReader(new StringReader(jsonString));
        JsonObject api = json.readObject();
        for (String area : api.keySet()) {
            Map<String, String[]> medges = new HashMap<>();
            for (String edge : api.getJsonObject(area).keySet()) {
                medges.put(edge, api.getJsonObject(area).getJsonArray(edge).stream().map(JsonValue::toString)
                        .toArray(String[]::new));
            }
            edges.put(area, medges);
        }
    }

    public static String[] get(int[] map) {
        String areaId = "World " + map[0] + "-" + map[1];
        String cell = String.valueOf(map[2]);
        if (Objects.nonNull(edges) && Objects.nonNull(edges.get(areaId))
                && Objects.nonNull(edges.get(areaId).get(cell))) {
            return edges.get(areaId).get(cell);
        }
        return null;
    }

    public static String[] list() {
        return edges.values().stream()
            .flatMap(edge -> edge.values().stream())
            .map(masses -> masses[1])
            .filter(mass -> !mass.matches("^[0-9]*$|^Start.*"))
            .collect(Collectors.toSet())
            .stream()
            .sorted()
            .toArray(String[]::new);
    }
}
