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


public class Backpack {
    public static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config config;
    public static CosmeticResponse[] cosmeticResponse;
    public static IoPackage pkg;

    public static long start;

    public static boolean useBID;

    public static String backpackName;
    public static String backpackPath;

    public static void promptBackpack() throws Exception {

        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, Config.class);
        }

        System.out.println("Enter Backpack Name or BID:");
        String backpackSelection = new Scanner(System.in).nextLine().replace(".uasset", "");

        useBID = backpackSelection.contains("BID_");

        // Path and Name by Selection
        start = System.currentTimeMillis();

        if (!useBID) {
            String skinSelectionFormat =
                    String.format("https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name=%s&backendType=AthenaBackpack",
                            backpackSelection);
            Reader reader = new OkHttpClient().newCall(new Request.Builder().url(skinSelectionFormat).build()).execute().body().charStream();
            cosmeticResponse = GSON.fromJson(reader, CosmeticResponse[].class);
            reader.close();

            if (cosmeticResponse.length == 0) {
                LOGGER.error("Backpack Not Found.");
                promptBackpack();
            }

            if (cosmeticResponse[0].path == null) {
                LOGGER.error("Invalid Backpack Selection.");
                promptBackpack();
            }

            backpackName = cosmeticResponse[0].name;
            backpackPath = cosmeticResponse[0].path + ".uasset";
        } else if (useBID) {
            backpackName = backpackSelection;
            backpackPath = "/Game/Athena/Items/Cosmetics/BackPacks/" + backpackName + ".uasset";
        }

        pkg = (IoPackage) fileProvider.loadGameFile(backpackPath);
        if (pkg == null) LOGGER.error("Error Parsing Package.");

        processBackpack();
    }

    public static void processBackpack() throws Exception {

        String CP = processBID(pkg);
        List<MeshMaterialData> CPDataList = new ArrayList<>();
        pkg = (IoPackage) fileProvider.loadGameFile(CP);
        processCP(pkg, CPDataList, 1);

        // Create Processed JSON
        JsonObject processed = new JsonObject();
        processed.addProperty("objectName", backpackName.replace(".uasset", ""));

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

