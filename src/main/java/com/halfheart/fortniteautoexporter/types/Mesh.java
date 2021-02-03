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
import me.fungames.jfortniteparse.ue4.assets.exports.UObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.halfheart.fortniteautoexporter.Main.fileProvider;
import static com.halfheart.fortniteautoexporter.ProcessCosmetics.*;


public class Mesh {
    public static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config config;
    public static CosmeticResponse[] cosmeticResponse;
    public static IoPackage pkg;

    public static long start;

    public static String meshName;
    public static String meshPath;

    public static void processMeshType() throws Exception {

        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, Config.class);
        }

        System.out.println("Enter Mesh Path:");
        String meshSelection = new Scanner(System.in).nextLine().replace(".uasset", "");

        // Path and Name by Selection
        start = System.currentTimeMillis();

        String[] meshSplit = meshSelection.split("/");
        meshName = meshSplit[meshSplit.length - 1];
        meshPath = meshSelection;

        promptMesh();
    }

    public static void promptMesh() throws Exception {
        List<MeshMaterialData> MeshDataList = new ArrayList<>();
        pkg = (IoPackage) fileProvider.loadGameFile(meshPath);
        processMesh(pkg, MeshDataList);

        // Create Processed JSON
        JsonObject processed = new JsonObject();
        processed.addProperty("objectName", meshName.replace(".uasset", ""));

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

