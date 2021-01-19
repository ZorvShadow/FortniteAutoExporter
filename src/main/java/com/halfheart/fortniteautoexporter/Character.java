package com.halfheart.fortniteautoexporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kotlin.io.FilesKt;
import kotlin.ranges.IntRange;
import me.fungames.jfortniteparse.fileprovider.DefaultFileProvider;
import me.fungames.jfortniteparse.tb24.JWPSerializer;
import me.fungames.jfortniteparse.ue4.assets.IoPackage;
import me.fungames.jfortniteparse.ue4.assets.mappings.UsmapTypeMappingsProvider;
import me.fungames.jfortniteparse.ue4.asyncloading2.FPackageObjectIndex;
import me.fungames.jfortniteparse.ue4.objects.core.misc.FGuid;
import me.fungames.jfortniteparse.ue4.versions.Ue4Version;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Character {

    private static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config;
    private static CosmeticResponse[] cosmeticResponse;
    private static DefaultFileProvider fileProvider;

    private static IoPackage pkg;
    private static long start;
    private static String STWHeroOverride = "";

    public static void promptSkin() throws Exception {
        System.out.println("\nCurrent Character Types:\nBR\nStW\n");
        System.out.println("Character Type to Export:");
        String selection = new Scanner(System.in).nextLine();

        if (selection.equalsIgnoreCase("StW")) {
            System.out.println("Enter Hero ID Path:");
            STWHeroOverride = new Scanner(System.in).nextLine().replace(".uasset", "") + ".uasset";
        } else if (selection.equalsIgnoreCase("BR")) {
        } else {
            System.out.println("Invalid Selection!");
            promptSkin();
        }

        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, Config.class);
        }

        if (STWHeroOverride.isEmpty()) {
            System.out.println("Enter Character Name:");
            String skinSelection = new Scanner(System.in).nextLine().replace(".uasset", "");
            start = System.currentTimeMillis();
            String skinSelectionFormat = String.format("https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name=%s&backendType=AthenaCharacter", skinSelection);
            Reader reader = new OkHttpClient().newCall(new Request.Builder().url(skinSelectionFormat).build()).execute().body().charStream();
            cosmeticResponse = GSON.fromJson(reader, CosmeticResponse[].class);
            reader.close();

            if (cosmeticResponse.length == 0) {
                LOGGER.error("Character Not Found.");
                promptSkin();
            }

            if (cosmeticResponse[0].path == null) {
                LOGGER.error("Invalid Character Selection.");
                promptSkin();
            }
        }

        fileProvider = new DefaultFileProvider(new File(config.PaksDirectory), config.UEVersion);
        fileProvider.submitKey(FGuid.Companion.getMainGuid(), config.EncryptionKey);

        Reader reader = new OkHttpClient().newCall(new Request.Builder().url("https://benbotfn.tk/api/v1/mappings").build()).execute().body().charStream();
        mappingsResponse[] mappingsResponse = GSON.fromJson(reader, mappingsResponse[].class);
        UsmapTypeMappingsProvider mappingsProvider = new UsmapTypeMappingsProvider(new File(System.getProperty("user.dir") + "/Mappings/" + mappingsResponse[0].fileName));
        mappingsProvider.reload();
        fileProvider.setMappingsProvider(mappingsProvider);

        if (STWHeroOverride.isEmpty()) {
            pkg = (IoPackage) fileProvider.loadGameFile(cosmeticResponse[0].path + ".uasset");

            if (pkg == null) {
                LOGGER.error("Error Parsing Package.");
            }
        }

        processSkin();
    }
    public static void processSkin() throws Exception {
        try {
            String HIDPath;
            String toJson;
            if (!STWHeroOverride.isEmpty()) {
                HIDPath = STWHeroOverride;
            } else {
                // CID to HID
                toJson = JWPSerializer.GSON.toJson(pkg.getExports());
                CIDStructure[] cidStructure = GSON.fromJson(toJson, CIDStructure[].class);
                HIDPath = "/Game/Athena/Heroes/" + cidStructure[0].HeroDefinition[0] + ".uasset";
            }

            // HID to HS
            pkg = (IoPackage) fileProvider.loadGameFile(HIDPath);
            toJson = JWPSerializer.GSON.toJson(pkg.getExports());

            HIDStructure[] hidStructure = GSON.fromJson(toJson, HIDStructure[].class);
            String HSPath = hidStructure[0].Specializations[0].asset_path_name.split("\\.")[0];


            // HS to CP
            pkg = (IoPackage) fileProvider.loadGameFile(HSPath + ".uasset");
            toJson = JWPSerializer.GSON.toJson(pkg.getExports());
            HSStructure[] hsStructure = GSON.fromJson(toJson, HSStructure[].class);

            List<String> CharacterParts = new ArrayList<>();
            for (int i : range(hsStructure[0].CharacterParts.length)) {
                CharacterParts.add(hsStructure[0].CharacterParts[i].asset_path_name.split("\\.")[0]);
            }

            // CP to Mesh to Material
            List<String> MeshesList = new ArrayList<>();
            List<overrideStructure3D> MaterialsList = new ArrayList<>();
            for (int i : range(hsStructure[0].CharacterParts.length))  {
                pkg = (IoPackage) fileProvider.loadGameFile(CharacterParts.toArray()[i] + ".uasset");
                String toJsonCP =  JWPSerializer.GSON.toJson(pkg.getExports());
                CPStructure[] cpStructure = GSON.fromJson(toJsonCP, CPStructure[].class);
                MeshesList.add(cpStructure[1].SkeletalMesh.asset_path_name.split("\\.")[0]);

                if (toJsonCP.contains("OverrideMaterial")) {
                    for (int j : range(cpStructure[1].MaterialOverrides.length)) {
                        MaterialsList.add(new overrideStructure3D(cpStructure[1].SkeletalMesh.asset_path_name.split("\\.")[1],
                                cpStructure[1].MaterialOverrides[j].OverrideMaterial.asset_path_name.split("\\.")[0],
                                cpStructure[1].MaterialOverrides[j].MaterialOverrideIndex,
                                "true"));
                    }
                } if (true) {
                    pkg = (IoPackage) fileProvider.loadGameFile(cpStructure[1].SkeletalMesh.asset_path_name.split("\\.")[0] + ".uasset");
                    for (FPackageObjectIndex e : pkg.getImportMap()) {
                        if (e.isNull()) {
                            break;
                        } else {
                            if (pkg.resolveObjectIndex(e, true).getPkg().toString().contains("Material")) {
                                MaterialsList.add(new overrideStructure3D(cpStructure[1].SkeletalMesh.asset_path_name.split("\\.")[1],
                                        pkg.resolveObjectIndex(e, true).getPkg().toString(),
                                        0,
                                        "false"));
                            }
                        }
                    }
                }

            }

            // Create processed.json
            JsonObject processedRoot = new JsonObject();
            if (!STWHeroOverride.isEmpty()) {
                processedRoot.addProperty("objectName", STWHeroOverride.split("/")[STWHeroOverride.split("/").length - 1]
                        .replace(".uasset", ""));
            } else {
                processedRoot.addProperty("objectName", cosmeticResponse[0].name);
            }

            JsonArray meshArray = new JsonArray();
            processedRoot.add("Meshes", meshArray);
            for (String i : MeshesList) {
                meshArray.add(i);
            }

            JsonArray materialArray = new JsonArray();
            processedRoot.add("Materials", materialArray);

            // Material to Texture Parameters
            for (int i : range(MaterialsList.toArray().length)) {

                pkg = (IoPackage) fileProvider.loadGameFile(MaterialsList.get(i).materialPath);
                toJson = JWPSerializer.GSON.toJson(pkg.getExports());
                textureParameterMaterialStructure[] materialStructure = GSON.fromJson(toJson, textureParameterMaterialStructure[].class);

                if (config.dumpMaterials) {
                    File materialDumpFolder = new File("MaterialDumps");
                    if (!materialDumpFolder.exists()) {
                        materialDumpFolder.mkdir();
                    }
                    File materialFile = new File(materialDumpFolder, MaterialsList.get(i).toString() + ".json");
                    FileUtils.touch(materialFile);
                    FilesKt.writeBytes(materialFile, toJson.getBytes());
                }

                JsonObject MaterialName = new JsonObject();
                materialArray.add(MaterialName);
                String[] splitMaterialsList = MaterialsList.get(i).materialPath.split("/");
                MaterialName.addProperty("MaterialName", splitMaterialsList[splitMaterialsList.length - 1]);
                MaterialName.addProperty("MaterialPath", MaterialsList.get(i).materialPath);
                MaterialName.addProperty("OverrideIndex", MaterialsList.get(i).overrideIndex);
                MaterialName.addProperty("TargetMesh", MaterialsList.get(i).meshName);
                MaterialName.addProperty("useOverride", MaterialsList.get(i).isOverride);
                MaterialName.addProperty("meshType", "psk");

                String textureType;
                String textureValue;
                if (toJson.contains("TextureParameterValues")) {
                    for (int e : range(materialStructure[0].TextureParameterValues.length)) {
                        textureType = materialStructure[0].TextureParameterValues[e].ParameterInfo.Name;
                        textureValue = materialStructure[0].TextureParameterValues[e].ParameterValue[1];

                        for (FPackageObjectIndex j : pkg.getImportMap()) {
                            if (j.isNull()) {
                                break;
                            } else {
                                MaterialName.addProperty(textureType, textureValue);
                            }
                        }
                    }
                }
                scalarParameterMaterialStructure[] scalarParameterMaterialStructure = GSON.fromJson(toJson, Character.scalarParameterMaterialStructure[].class);
                if (toJson.contains("ScalarParameterValues")) {
                    for (int e : range(scalarParameterMaterialStructure[0].ScalarParameterValues.length)) {
                        String scalarType = scalarParameterMaterialStructure[0].ScalarParameterValues[e].ParameterInfo.Name;
                        double scalarValue = scalarParameterMaterialStructure[0].ScalarParameterValues[e].ParameterValue;

                        MaterialName.addProperty(scalarType, scalarValue);
                    }
                }

                vectorParameterMaterialStructure[] vectorParameterMaterialStructure = GSON.fromJson(toJson, Character.vectorParameterMaterialStructure[].class);
                if (toJson.contains("VectorParameterValues")) {
                    for (int e : range(vectorParameterMaterialStructure[0].VectorParameterValues.length)) {
                        String vectorType = vectorParameterMaterialStructure[0].VectorParameterValues[e].ParameterInfo.Name;
                        String vectorValues = vectorParameterMaterialStructure[0].VectorParameterValues[e].ParameterValue.r
                                + "," + vectorParameterMaterialStructure[0].VectorParameterValues[e].ParameterValue.g
                                + "," + vectorParameterMaterialStructure[0].VectorParameterValues[e].ParameterValue.b
                                + "," + vectorParameterMaterialStructure[0].VectorParameterValues[e].ParameterValue.a;

                        MaterialName.addProperty(vectorType, vectorValues);
                    }
                }
            }

            // Write processed.json
            File processedFile = new File("processed.json");
            processedFile.createNewFile();
            FileWriter writer = new FileWriter(processedFile);
            writer.write(GSON.toJson(processedRoot));
            writer.close();

            if (config.exportAssets) {
                try (PrintWriter printWriter = new PrintWriter("umodel_queue.txt")) {
                    printWriter.println("-path=\"" + config.PaksDirectory + "\"");
                    printWriter.println("-game=ue4." + "27");
                    printWriter.println("-aes=" + config.EncryptionKey);
                    printWriter.println("-export ");
                    for (String meshes : MeshesList) {
                        printWriter.println("-pkg=" + meshes);
                    }
                    for (overrideStructure3D mats : MaterialsList) {
                        printWriter.println("-pkg=" + mats.materialPath);
                    }
                }
                
                ProcessBuilder pb = new ProcessBuilder(Arrays.asList("umodel", "@umodel_queue.txt"));
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                pb.start().waitFor();
            }

            LOGGER.info(String.format("Finished Exporting in %.1f sec. Replace the line with workingDirectory in the python script with this line:\n\nworkingDirectory = r\"%s\"\n",  (System.currentTimeMillis() - start) / 1000.0F, new File("").getAbsolutePath()));
            Main.selectItemType();
        } catch (Exception exception) {
            exception.printStackTrace();
            Main.selectItemType();
        }



    }

    public static IntRange range(int max) {
        return new IntRange(0, max-1);
    }

    public static void selectSwitch(String selection) throws Exception {
        switch (selection) {
            case "StW":
            case "stw":
            case "STW":
                System.out.println("Enter Hero ID Path:");
                STWHeroOverride = new Scanner(System.in).nextLine().replace(".uasset", "") + ".uasset";

            case "BR":
            case "br":
                break;

            default:
                System.out.println("Invalid Selection!");
                promptSkin();
        }
    }

    public static class CIDStructure {
        private String[] HeroDefinition;
    }
    public static class HIDStructure {
        private Specializations[] Specializations;
        public class Specializations {
            public String asset_path_name;
        }
    }
    public static class HSStructure {
        private PathName[] CharacterParts;
        public class PathName {
            public String asset_path_name;
        }
    }
    public static class CPStructure {
        private SkeletalMesh SkeletalMesh;
        private materialOverride[] MaterialOverrides;
        private assetPath SkinColorSwatch;
        public class SkeletalMesh {
            public String asset_path_name;
        }
        public class materialOverride {
            public assetPath OverrideMaterial;
            public int MaterialOverrideIndex;
        }
        public class assetPath {
            public String asset_path_name;
        }
    }

    public static class textureParameterMaterialStructure {
        public Parameters[] TextureParameterValues;
        public class Parameters {
            public ParameterInfo ParameterInfo;
            public String[] ParameterValue;
        }
        public class ParameterInfo {
            public String Name;
        }
    }
    public static class scalarParameterMaterialStructure {
        public Parameters[] ScalarParameterValues;
        public class Parameters {
            public ParameterInfo ParameterInfo;
            public double ParameterValue;
        }
        public class ParameterInfo {
            public String Name;
        }
    }
    public static class vectorParameterMaterialStructure {
        public Parameters[] VectorParameterValues;
        public class Parameters {
            public ParameterInfo ParameterInfo;
            public ParameterValue ParameterValue;
        }
        public class ParameterInfo {
            public String Name;
        }
        public class ParameterValue {
            float r;
            float g;
            float b;
            float a;
        }
    }

    public static class overrideStructure3D {
        private String meshName;
        private String materialPath;
        private int overrideIndex;
        private String isOverride;

        public overrideStructure3D(String meshName, String materialPath, int overrideIndex, String isOverride) {
            this.meshName = meshName;
            this.materialPath = materialPath;
            this.overrideIndex = overrideIndex;
            this.isOverride = isOverride;
        }
    }

    public static class Config {
        private String PaksDirectory;
        private Ue4Version UEVersion;
        private String EncryptionKey;
        private String STWHeroOverride = "";
        private boolean exportAssets;
        private boolean dumpMaterials;

    }
    public static class CosmeticResponse {
        private String id;
        private String path;
        private String name;
    }
    public static class mappingsResponse {
        private String url;
        private String fileName;
    }
}
