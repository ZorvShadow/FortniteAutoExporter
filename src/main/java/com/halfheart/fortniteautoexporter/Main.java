package com.halfheart.fortniteautoexporter;

import com.google.gson.*;
import kotlin.io.FilesKt;
import kotlin.ranges.IntRange;
import me.fungames.jfortniteparse.fileprovider.DefaultFileProvider;
import me.fungames.jfortniteparse.tb24.JWPSerializer;
import me.fungames.jfortniteparse.ue4.assets.IoPackage;
import me.fungames.jfortniteparse.ue4.assets.mappings.UsmapTypeMappingsProvider;
import me.fungames.jfortniteparse.ue4.asyncloading2.FPackageObjectIndex;
import me.fungames.jfortniteparse.ue4.objects.core.misc.FGuid;
import me.fungames.jfortniteparse.ue4.pak.PakFileReader;
import me.fungames.jfortniteparse.ue4.versions.Ue4Version;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config;
    private static CosmeticResponse[] cosmeticResponse;
    private static DefaultFileProvider fileProvider;

    private static IoPackage pkg;
    private static File pakDir;

    private static long start;

    public static void main(String[] Args) throws Exception {
        updateMappings();

        File configFile = new File("config.json");

        if (!configFile.exists()) {
            LOGGER.error("Config file does not exist.");
            return;
        }

        LOGGER.info("Reading config file at " + configFile.getAbsolutePath());

        try (FileReader reader = new FileReader(configFile)){
            config = GSON.fromJson(reader, Config.class);
        }

        LOGGER.info("Unreal Version: " + config.UEVersion);

        pakDir = new File(config.PaksDirectory);

        if (!pakDir.exists()) {
            LOGGER.error("Directory " + pakDir.getAbsolutePath() + " does not exist.");
            return;
        }

        LOGGER.info("Pak Directory: " + pakDir.getAbsolutePath());

        if (config.UEVersion == null || !Arrays.toString(Ue4Version.values()).contains(config.UEVersion.toString())) {
            LOGGER.error("Invalid Unreal Version.");
        }

        while (true) promptSkin();
    }
    public static void promptSkin() throws Exception {
        System.out.println("Enter Skin Selection:");
        String skinSelection = new Scanner(System.in).nextLine().replace(" ", "%20");
        start = System.currentTimeMillis();
        String skinSelectionFormat = String.format("https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name=%s&backendType=AthenaCharacter", skinSelection);
        Reader reader = new OkHttpClient().newCall(new Request.Builder().url(skinSelectionFormat).build()).execute().body().charStream();
        cosmeticResponse = GSON.fromJson(reader, CosmeticResponse[].class);
        reader.close();

        if (cosmeticResponse.length == 0) {
            LOGGER.error("Skin Not Found.");
            promptSkin();
        }

        if (cosmeticResponse[0].path == null) {
            LOGGER.error("Invalid Skin Selection.");
            promptSkin();
        }
        fileProvider = new DefaultFileProvider(pakDir, config.UEVersion);
        fileProvider.submitKey(FGuid.Companion.getMainGuid(), config.EncryptionKey);

        reader = new OkHttpClient().newCall(new Request.Builder().url("https://benbotfn.tk/api/v1/mappings").build()).execute().body().charStream();
        mappingsResponse[] mappingsResponse = GSON.fromJson(reader, mappingsResponse[].class);
        UsmapTypeMappingsProvider mappingsProvider = new UsmapTypeMappingsProvider(new File(System.getProperty("user.dir") + "/Mappings/" + mappingsResponse[0].fileName));
        mappingsProvider.reload();
        fileProvider.setMappingsProvider(mappingsProvider);

        pkg = (IoPackage) fileProvider.loadGameFile(cosmeticResponse[0].path + ".uasset");

        if (pkg == null) {
            LOGGER.error("Error Parsing Package.");
        }

        processSkin();
    }
    public static void processSkin() {
        try {

            // CID to HID
            String toJson = JWPSerializer.GSON.toJson(pkg.getExports());
            CIDStructure[] cidStructure = GSON.fromJson(toJson, CIDStructure[].class);
            String HIDPath = "/Game/Athena/Heroes/" + cidStructure[0].HeroDefinition[0];

            // HID to HS
            pkg = (IoPackage) fileProvider.loadGameFile(HIDPath + ".uasset");
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
            List<String> MaterialsList = new ArrayList<>();
            for (int i : range(hsStructure[0].CharacterParts.length))  {
                pkg = (IoPackage) fileProvider.loadGameFile(CharacterParts.toArray()[i] + ".uasset");
                String toJsonCP =  JWPSerializer.GSON.toJson(pkg.getExports());
                CPStructure[] cpStructure = GSON.fromJson(toJsonCP, CPStructure[].class);
                MeshesList.add(cpStructure[1].SkeletalMesh.asset_path_name.split("\\.")[0]);

                pkg = (IoPackage) fileProvider.loadGameFile(cpStructure[1].SkeletalMesh.asset_path_name.split("\\.")[0] + ".uasset");
                int j = 0;
                for (FPackageObjectIndex e : pkg.getImportMap()) {
                    if (e.isNull()) {
                        break;
                    } else {
                        if (pkg.resolveObjectIndex(e, true).getPkg().toString().contains("Material")) {
                            MaterialsList.add(pkg.resolveObjectIndex(e, true).getPkg().toString());
                        }
                    }
                }
            }

            // Create processed.json
            JsonObject processedRoot = new JsonObject();
            processedRoot.addProperty("objectName", cosmeticResponse[0].name);

            JsonArray meshArray = new JsonArray();
            processedRoot.add("Meshes", meshArray);
            for (String i : MeshesList) {
                meshArray.add(i);
            }

            JsonArray materialArray = new JsonArray();
            processedRoot.add("Materials", materialArray);

            // Material to Texture Parameters
            for (int i : range(MaterialsList.toArray().length)) {

                pkg = (IoPackage) fileProvider.loadGameFile(MaterialsList.toArray()[i].toString());
                toJson = JWPSerializer.GSON.toJson(pkg.getExports());
                textureParameterMaterialStructure[] materialStructure = GSON.fromJson(toJson, textureParameterMaterialStructure[].class);

                if (config.dumpMaterials) {
                    File materialDumpFolder = new File("MaterialDumps");
                    if (!materialDumpFolder.exists()) {
                        materialDumpFolder.mkdir();
                    }
                    File materialFile = new File(materialDumpFolder, MaterialsList.toArray()[i].toString() + ".json");
                    FileUtils.touch(materialFile);
                    FilesKt.writeBytes(materialFile, toJson.getBytes());
                }

                JsonObject MaterialName = new JsonObject();
                materialArray.add(MaterialName);
                String[] splitMaterialsList = MaterialsList.get(i).split("/");
                MaterialName.addProperty("MaterialName", splitMaterialsList[splitMaterialsList.length - 1]);
                MaterialName.addProperty("MaterialPath", MaterialsList.toArray()[i].toString());

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
                scalarParameterMaterialStructure[] scalarParameterMaterialStructure = GSON.fromJson(toJson, Main.scalarParameterMaterialStructure[].class);
                if (toJson.contains("ScalarParameterValues")) {
                    for (int e : range(scalarParameterMaterialStructure[0].ScalarParameterValues.length)) {
                        String scalarType = scalarParameterMaterialStructure[0].ScalarParameterValues[e].ParameterInfo.Name;
                        double scalarValue = scalarParameterMaterialStructure[0].ScalarParameterValues[e].ParameterValue;

                        MaterialName.addProperty(scalarType, scalarValue);
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
                    for (String mats : MaterialsList) {
                        printWriter.println("-pkg=" + mats);
                    }
                }
                Thread.sleep(5000);
                ProcessBuilder pb = new ProcessBuilder(Arrays.asList("umodel", "@umodel_queue.txt"));
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                pb.start().waitFor();
            }

            LOGGER.info(String.format("Finished Exporting in %.1f sec. Replace the line with workingDirectory in the python script with this line:\n\nworkingDirectory = \"%s\\\\\"\n\n",  (System.currentTimeMillis() - start) / 1000.0F, new File("").getAbsolutePath().replace("\\", "\\\\"))); ;
        } catch (Exception exception) {
            exception.printStackTrace();
        }



    }

    public static IntRange range(int max) {
        return new IntRange(0, max-1);
    }

    public static void updateMappings() throws Exception {
        Reader reader = new OkHttpClient().newCall(new Request.Builder().url("https://benbotfn.tk/api/v1/mappings").build()).execute().body().charStream();
        mappingsResponse[] mappingsResponse = GSON.fromJson(reader, mappingsResponse[].class);

        File mappingsFolder = new File("Mappings");
        File mappingsFile = new File(mappingsFolder, mappingsResponse[0].fileName);

        byte[] usmap = new OkHttpClient().newCall(new Request.Builder().url(mappingsResponse[0].url).build()).execute().body().bytes();

        if (!mappingsFolder.exists()) {
            mappingsFolder.mkdir();
        }
        FilesKt.writeBytes(mappingsFile, usmap);

        LOGGER.info("Mappings File: " + mappingsFile.getName());
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

    public static class Config {
        private String PaksDirectory;
        private Ue4Version UEVersion;
        private String EncryptionKey;
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
