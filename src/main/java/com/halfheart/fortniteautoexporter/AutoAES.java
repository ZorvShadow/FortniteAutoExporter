package com.halfheart.fortniteautoexporter;

import com.google.gson.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AutoAES {
    private static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void getKeys() throws IOException {
        LOGGER.info("Getting Encryption Keys...");
        Reader reader = new OkHttpClient().newCall(new Request.Builder().url("https://benbotfn.tk/api/v1/aes").build()).execute().body().charStream();
        LOGGER.info("Writing to Config...");
        JSONStructures.aesResponse response = GSON.fromJson(reader, JSONStructures.aesResponse.class);
        reader.close();

        File configFile = new File("config.json");
        FileReader fileReader = new FileReader(configFile);
        JsonElement configJSON = JsonParser.parseReader(fileReader);

        JsonObject configAsJsonObject = configJSON.getAsJsonObject();
        configAsJsonObject.addProperty("mainKey", response.mainKey);
        JsonArray dynamicKeys = configJSON.getAsJsonObject().getAsJsonArray("dynamicKeys");
        while (dynamicKeys.size() != 0) {
            dynamicKeys.remove(0);
        }
        for (Map.Entry<String, String> keyEntry : response.dynamicKeys.entrySet()) {
            JsonObject dynamicKey = new JsonObject();
            dynamicKey.addProperty("fileName", keyEntry.getKey().substring(keyEntry.getKey().lastIndexOf('/') + 1));
            dynamicKey.addProperty("key", keyEntry.getValue());
            dynamicKeys.add(dynamicKey);
        }

        FileWriter fileWriter = new FileWriter(configFile);
        GSON.toJson(configJSON, fileWriter);
        fileWriter.close();
        LOGGER.info("Finished Updating Keys.");
    }
}
