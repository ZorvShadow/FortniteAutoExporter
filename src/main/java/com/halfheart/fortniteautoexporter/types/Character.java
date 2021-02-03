package com.halfheart.fortniteautoexporter.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.halfheart.fortniteautoexporter.JSONStructures;
import com.halfheart.fortniteautoexporter.JSONStructures.Config;
import com.halfheart.fortniteautoexporter.JSONStructures.CosmeticResponse;
import com.halfheart.fortniteautoexporter.JSONStructures.HSStructure.CharacterParts;
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
import static com.halfheart.fortniteautoexporter.Utils.range;


public class Character {
    public static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config config;
    public static CosmeticResponse[] cosmeticResponse;
    public static IoPackage pkg;

    public static long start;

    public static boolean useCID;
    public static boolean useHID;

    public static String characterName;
    public static String characterPath;

    public static void promptCharacter() throws Exception {

        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, Config.class);
        }

        System.out.println("\nCurrent Character Types:\nBR\nStW\n");
        System.out.println("Character Type to Export:");
        String characterTypeSelection = new Scanner(System.in).nextLine();

        // Character Selection

        String characterSelection = "";
        if (characterTypeSelection.equalsIgnoreCase("BR")) {
            useHID = false;

            System.out.println("Enter Character Name or CID:");
            characterSelection = new Scanner(System.in).nextLine().replace(".uasset", "");

            useCID = characterSelection.contains("CID_");
        } else if (characterTypeSelection.equalsIgnoreCase("StW")) {
            useHID = true;
            useCID = false;
            System.out.println("Enter Hero HID:");
            characterSelection = new Scanner(System.in).nextLine().replace(".uasset", "");
        } else {
            System.out.println("Invalid Character Type!");
        }

        // Path and Name by Selection
        start = System.currentTimeMillis();

        if (!useCID & !useHID) {
            String skinSelectionFormat =
                    String.format("https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name=%s&backendType=AthenaCharacter",
                            characterSelection);
            Reader reader = new OkHttpClient().newCall(new Request.Builder().url(skinSelectionFormat).build()).execute().body().charStream();
            cosmeticResponse = GSON.fromJson(reader, CosmeticResponse[].class);
            reader.close();

            if (cosmeticResponse.length == 0) {
                LOGGER.error("Character Not Found.");
                promptCharacter();
            }

            if (cosmeticResponse[0].path == null) {
                LOGGER.error("Invalid Character Selection.");
                promptCharacter();
            }

            characterName = cosmeticResponse[0].name;
            characterPath = cosmeticResponse[0].path + ".uasset";
        } else if (useCID & !useHID) {
            characterName = characterSelection;
            characterPath = "/Game/Athena/Items/Cosmetics/Characters/" + characterName + ".uasset";
        } else if (!useCID & useHID) {
            String[] characterSelectionSplit = characterSelection.split("/");
            characterName = characterSelectionSplit[characterSelectionSplit.length - 1];
            characterPath = characterSelection + ".uasset";
        }

        pkg = (IoPackage) fileProvider.loadGameFile(characterPath);
        if (pkg == null) LOGGER.error("Error Parsing Package.");

        if (JWPSerializer.GSON.toJson(pkg.getExports()).contains("FortCosmeticCharacterPartVariant")) {
            processCharacterVariants();
        } else {
            processCharacter();
        }

    }

    public static void processCharacter() throws Exception {

        // CID to Character Parts
        String HID = "";
        if (!useHID) {
            HID = processCID(pkg);
        } else if (useHID) {
            HID = characterPath;
        }

        pkg = (IoPackage) fileProvider.loadGameFile(HID);
        String HS = processHID(pkg);

        pkg = (IoPackage) fileProvider.loadGameFile(HS);
        CharacterParts[] CP = processHS(pkg);

        List<MeshMaterialData> CPDataList = new ArrayList<>();
        for (CharacterParts characterPart : CP) {
            pkg = (IoPackage) fileProvider.loadGameFile(characterPart.asset_path_name);
            processCP(pkg, CPDataList, 1);
        }

        // Create Processed JSON
        JsonObject processed = new JsonObject();
        processed.addProperty("objectName", characterName.replace(".uasset", ""));

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

                for (int i : range(config.dynamicKeys.toArray().length)) {
                   printWriter.println("-aes=" + config.dynamicKeys.get(i).key);
                }

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

    public static void processCharacterVariants() throws Exception {

        // Create Processed JSON
        JsonObject processed = new JsonObject();
        processed.addProperty("objectName", characterName.replace(".uasset", ""));

        List<String> VariantCP = new ArrayList<>();
        List<JSONStructures.VariantMaterialList> VariantMaterialsList = new ArrayList<>();
        String HIDandVARIANT = processCIDVariant(pkg, VariantCP, VariantMaterialsList);

        processed.addProperty("objectName", String.format(characterName.replace(".uasset", "") + " (%s)",HIDandVARIANT.split("::")[1].toUpperCase()));

        JsonArray VariantMaterials = new JsonArray();
        processed.add("VariantMaterials", VariantMaterials);
        processMaterialVariants(VariantMaterialsList, VariantMaterials);

        pkg = (IoPackage) fileProvider.loadGameFile(HIDandVARIANT.split("::")[0]);
        String HS = processHID(pkg);

        pkg = (IoPackage) fileProvider.loadGameFile(HS);
        CharacterParts[] CP = processHS(pkg);

        List<MeshMaterialData> CPDataList = new ArrayList<>();
        for (String characterPart : VariantCP) {
            pkg = (IoPackage) fileProvider.loadGameFile(characterPart);
            processCP(pkg, CPDataList, 1);
        }
        for (CharacterParts characterPart : CP) {
            pkg = (IoPackage) fileProvider.loadGameFile(characterPart.asset_path_name);
            processCP(pkg, CPDataList, 1);
        }

        JsonArray Meshes = new JsonArray();
        processed.add("Meshes", Meshes);
        for (MeshMaterialData data : CPDataList) {
            if (!Meshes.toString().contains(data.meshPath)) {
                Meshes.add(data.meshPath);
            }
        }

        JsonArray CPTypesArray = new JsonArray();
        processed.add("CharacterPartTypes", CPTypesArray);
        String previousType = "";
        for (MeshMaterialData data : CPDataList) {
            System.out.println("PREVIOUS " + previousType);
            System.out.println("CURRENT " + data.CPType);
            if (previousType.equalsIgnoreCase(data.CPType)) {
                previousType = "";
                System.out.println("ABOVE TWO ARE MATCHING, IGNORE");
            } else {
                if (data.CPType != null) {
                    CPTypesArray.add(data.CPType);
                    previousType = data.CPType;
                }
            }
            System.out.println("-------------------------");

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

                for (int i : range(config.dynamicKeys.toArray().length)) {
                    printWriter.println("-aes=" + config.dynamicKeys.get(i).key);
                }

                printWriter.println("-export ");
                printWriter.println("-nooverwrite ");
                for (MeshMaterialData data : CPDataList) {
                    printWriter.println("-pkg=" + data.materialPath);
                    printWriter.println("-pkg=" + data.meshPath);
                }
                for (JSONStructures.VariantMaterialList data : VariantMaterialsList) {
                    printWriter.println("-pkg=" + data.overrideMaterial);
                    printWriter.println("-pkg=" + data.originalMaterial);
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

