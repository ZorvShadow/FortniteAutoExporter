package com.halfheart.fortniteautoexporter.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.halfheart.fortniteautoexporter.JSONStructures;
import com.halfheart.fortniteautoexporter.JSONStructures.Config;
import com.halfheart.fortniteautoexporter.JSONStructures.CosmeticResponse;
import com.halfheart.fortniteautoexporter.JSONStructures.MeshMaterialData;
import com.halfheart.fortniteautoexporter.Main;
import me.fungames.jfortniteparse.tb24.JWPSerializer;
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


public class Pickaxe {
    public static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config config;
    public static CosmeticResponse[] cosmeticResponse;
    public static IoPackage pkg;

    public static long start;
    
    public static boolean usePickaxeID;

    public static String pickaxeName;
    public static String pickaxePath;

    public static void promptPickaxe() throws Exception {

        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, Config.class);
        }

        System.out.println("Enter Pickaxe Name or Pickaxe_ID:");
        String pickaxeSelection = new Scanner(System.in).nextLine().replace(".uasset", "");
        usePickaxeID = pickaxeSelection.contains("Pickaxe_ID");

        // Path and Name by Selection

        if (!usePickaxeID) {
            String skinSelectionFormat =
                    String.format("https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name=%s&backendType=AthenaPickaxe",
                            pickaxeSelection);
            Reader reader = new OkHttpClient().newCall(new Request.Builder().url(skinSelectionFormat).build()).execute().body().charStream();
            cosmeticResponse = GSON.fromJson(reader, CosmeticResponse[].class);
            reader.close();

            if (cosmeticResponse.length == 0) {
                LOGGER.error("Pickaxe Not Found.");
                promptPickaxe();
            }

            if (cosmeticResponse[0].path == null) {
                LOGGER.error("Invalid Pickaxe Selection.");
                promptPickaxe();
            }

            pickaxeName = cosmeticResponse[0].name;
            pickaxePath = cosmeticResponse[0].path + ".uasset";
        } else if (usePickaxeID) {
            pickaxeName = pickaxeSelection;
            pickaxePath = "/Game/Content/Athena/Items/Cosmetics/Pickaxes/" + pickaxeName + ".uasset";
        }

        start = System.currentTimeMillis();

        pkg = (IoPackage) fileProvider.loadGameFile(pickaxePath);
        String toJson = JWPSerializer.GSON.toJson(pkg.getExports());
        JSONStructures.PickaxeIDStructure[] PickaxeID = GSON.fromJson(toJson, JSONStructures.PickaxeIDStructure[].class);

        pkg = (IoPackage) fileProvider.loadGameFile(PickaxeID[0].WeaponDefinition[1]);
        if (pkg == null) LOGGER.error("Error Parsing Package.");

        processWeapon();
    }

    public static void processWeapon() throws Exception {



        String WID = processWID(pkg);
        pkg = (IoPackage) fileProvider.loadGameFile(WID);

        List<MeshMaterialData> MeshDataList = new ArrayList<>();
        processMesh(pkg, MeshDataList);

        // Create Processed JSON
        JsonObject processed = new JsonObject();
        processed.addProperty("objectName", pickaxeName.replace(".uasset", ""));

        JsonArray Meshes = new JsonArray();
        processed.add("Meshes", Meshes);
        for (MeshMaterialData data : MeshDataList) {
            if (!Meshes.toString().contains(data.meshPath)) {
                Meshes.add(data.meshPath);
            }
        }

        JsonArray Materials = new JsonArray();
        processed.add("Materials", Materials);

        processMaterial(MeshDataList, Materials);

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
                for (MeshMaterialData data : MeshDataList) {
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

