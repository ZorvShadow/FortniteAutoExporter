package com.halfheart.fortniteautoexporter.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.halfheart.fortniteautoexporter.JSONStructures.Config;
import com.halfheart.fortniteautoexporter.JSONStructures.CosmeticResponse;
import com.halfheart.fortniteautoexporter.JSONStructures.MeshMaterialData;
import com.halfheart.fortniteautoexporter.Main;
import me.fungames.jfortniteparse.ue4.assets.IoPackage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.halfheart.fortniteautoexporter.Main.fileProvider;
import static com.halfheart.fortniteautoexporter.ProcessCosmetics.*;


public class Glider {
    public static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config config;
    public static CosmeticResponse[] cosmeticResponse;
    public static IoPackage pkg;

    public static long start;

    public static boolean useGlider;

    public static String gliderName;
    public static String gliderPath;

    public static void promptGlider() throws Exception {

        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, Config.class);
        }

        System.out.println("Enter Glider Name or Glider_ID:");
        String gliderSelection = new Scanner(System.in).nextLine().replace(".uasset", "");

        useGlider = gliderSelection.contains("_");

        // Path and Name by Selection
        start = System.currentTimeMillis();

        if (!useGlider) {
            String skinSelectionFormat =
                    String.format("https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name=%s&backendType=AthenaGlider",
                            gliderSelection);
            Reader reader = new OkHttpClient().newCall(new Request.Builder().url(skinSelectionFormat).build()).execute().body().charStream();
            cosmeticResponse = GSON.fromJson(reader, CosmeticResponse[].class);
            reader.close();

            if (cosmeticResponse.length == 0) {
                LOGGER.error("Glider Not Found.");
                promptGlider();
            }

            if (cosmeticResponse[0].path == null) {
                LOGGER.error("Invalid Glider Selection.");
                promptGlider();
            }

            gliderName = cosmeticResponse[0].name;
            gliderPath = cosmeticResponse[0].path + ".uasset";
        } else if (useGlider) {
            gliderName = gliderSelection;
            gliderPath = "/Game/Athena/Items/Cosmetics/Gliders/" + gliderName + ".uasset";
        }

        pkg = (IoPackage) fileProvider.loadGameFile(gliderPath);
        if (pkg == null) LOGGER.error("Error Parsing Package.");

        processGlider();
    }

    public static void processGlider() throws Exception {
        
        List<MeshMaterialData> CPDataList = new ArrayList<>();
        processCP(pkg, CPDataList, 0);

        // Create Processed JSON
        JsonObject processed = new JsonObject();
        processed.addProperty("objectName", gliderName.replace(".uasset", ""));

        JsonArray Meshes = new JsonArray();
        processed.add("Meshes", Meshes);
        for (MeshMaterialData data : CPDataList) {
            if (!Meshes.toString().contains(data.meshPath)) {
                Meshes.add(data.meshPath);
            }
        }

        JsonArray Materials = new JsonArray();
        processed.add("Materials", Materials);

        processMaterial(CPDataList, Materials);

        // Write to processed.json
        File processedFile = new File("processed.json");
        processedFile.createNewFile();
        FileWriter writer = new FileWriter(processedFile);
        writer.write(GSON.toJson(processed));
        writer.close();

        // Start Umodel
        if (config.exportAssets) {
            try (PrintWriter printWriter = new PrintWriter("umodel_queue.txt")) {
                printWriter.println("-path=\"" + config.PaksDirectory + "\"");
                printWriter.println("-game=ue4." + "27");
                printWriter.println("-aes=" + config.mainKey);
                printWriter.println("-export ");
                printWriter.println("-nooverwrite ");
                for (MeshMaterialData data : CPDataList) {
                    printWriter.println("-pkg=" + data.materialPath);
                    printWriter.println("-pkg=" + data.meshPath);
                }
            }

            ProcessBuilder pb = new ProcessBuilder(Arrays.asList("umodel", "@umodel_queue.txt"));
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.start().waitFor();

            LOGGER.info(String.format("Finished Exporting in %.1f sec.", (System.currentTimeMillis() - start) / 1000.0F));
            Main.selectItemType();
        }
    }

}

