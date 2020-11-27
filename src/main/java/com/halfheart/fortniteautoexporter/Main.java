package com.halfheart.fortniteautoexporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kotlin.ranges.IntRange;
import me.fungames.jfortniteparse.fileprovider.DefaultFileProvider;
import me.fungames.jfortniteparse.ue4.assets.Package;
import me.fungames.jfortniteparse.ue4.locres.FnLanguage;
import me.fungames.jfortniteparse.ue4.locres.Locres;
import me.fungames.jfortniteparse.ue4.objects.core.misc.FGuid;
import me.fungames.jfortniteparse.ue4.versions.Ue4Version;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config;
    private static CosmeticResponse[] cosmeticResponse;
    private static DefaultFileProvider fileProvider;

    private static Package pkg;
    private static Locres locres;

    public static void main(String[] Args) throws Exception {
        try {
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

            File pakDir = new File(config.PaksDirectory);

            if (!pakDir.exists()) {
                LOGGER.error("Directory " + pakDir.getAbsolutePath() + " does not exist.");
                return;
            }

            LOGGER.info("Pak Directory: " + pakDir.getAbsolutePath());

            if (config.UEVersion == null || !Arrays.toString(Ue4Version.values()).contains(config.UEVersion.toString())) {
                LOGGER.error("Invalid Unreal Version.");
            }

            while (true) {
                System.out.println("Enter Skin Selection:");
                String skinSelection = String.format("https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name=%s&backendType=AthenaCharacter",
                        new Scanner(System.in).nextLine().replace(" ", "%20"));
                Reader reader = new OkHttpClient().newCall(new Request.Builder().url(skinSelection).build()).execute().body().charStream();
                cosmeticResponse = GSON.fromJson(reader, CosmeticResponse[].class);
                reader.close();


                if (cosmeticResponse.length == 0) {
                    LOGGER.error("Skin Not Found.");
                    continue;
                }

                if (cosmeticResponse[0].path == null) {
                    LOGGER.error("Invalid Skin Selection.");
                    continue;
                }
                break;
            }
            fileProvider = new DefaultFileProvider(pakDir, config.UEVersion);
            fileProvider.submitKey(FGuid.Companion.getMainGuid(), config.EncryptionKey);
            locres = fileProvider.loadLocres(FnLanguage.EN);

            pkg = fileProvider.loadGameFile(cosmeticResponse[0].path + ".uasset");

            if (pkg == null) {
                LOGGER.error("Error Parsing Package.");
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        processSkin();

        System.out.println(String.format("Please replace workingDirectory in the python script with: \n%s\n", System.getProperty("user.dir").replace("\\", "\\\\") + "\\\\"));
        LOGGER.info("Finished Exporting.");

        System.exit(0);
    }
    public static void processSkin() throws Exception {
        try {

            // CID to HID
            String toJson = pkg.toJson(locres);
            CIDStructure cidStructure = GSON.fromJson(toJson, CIDStructure.class);
            String HIDPath = "";
            for (int i : range(cidStructure.import_map.length)) {
                if (cidStructure.import_map[i].class_name.contains("Package") &&
                        cidStructure.import_map[i].object_name.contains("HID")) HIDPath = cidStructure.import_map[i].object_name;
            }

            // HID to HS
            pkg = fileProvider.loadGameFile(HIDPath + ".uasset");
            toJson = pkg.toJson(locres);

            HIDStructure hidStructure = GSON.fromJson(toJson, HIDStructure.class);
            String HSPath = "";
            for (int i : range(hidStructure.export_properties.length)) {
                String[] HSSplit = hidStructure.export_properties[i].Specializations[0].assetPath.split("\\.");
                HSPath = HSSplit[0];
            }

            // HS to CP
            pkg = fileProvider.loadGameFile(HSPath + ".uasset");
            toJson = pkg.toJson(locres);
            HSStructure hsStructure = GSON.fromJson(toJson, HSStructure.class);

            List<String> CharacterParts = new ArrayList<>();
            for (int i : range(hsStructure.export_properties[0].CharacterParts.length)) {
                CharacterParts.add(hsStructure.export_properties[0].CharacterParts[i].assetPath.split("\\.")[0]);
            }

            // CP to Mesh
            List<String> MeshesList = new ArrayList<>();
            for (int i : range(hsStructure.export_properties[0].CharacterParts.length)) {
                pkg = fileProvider.loadGameFile(CharacterParts.toArray()[i] + ".uasset");
                toJson = pkg.toJson(locres);
                CPStructure cpStructure = GSON.fromJson(toJson, CPStructure.class);
                MeshesList.add(cpStructure.export_properties[1].SkeletalMesh.assetPath.split("\\.")[0]);
            }

            // Mesh to Material
            List<String> MaterialsList = new ArrayList<>();
            for (int i : range(hsStructure.export_properties[0].CharacterParts.length))  {
                pkg = fileProvider.loadGameFile(MeshesList.toArray()[i] + ".uasset");
                toJson = pkg.toJson(locres);

                MeshStructure meshStructure = GSON.fromJson(toJson, MeshStructure.class);
                for (int e : range(meshStructure.import_map.length)) {
                    if (meshStructure.import_map[e].class_name.contains("Package") && meshStructure.import_map[e].object_name.contains("Material")) {
                        MaterialsList.add(meshStructure.import_map[e].object_name);
                    }
                }
            }

            // Create processed.json
            JsonObject processedRoot = new JsonObject();
            processedRoot.addProperty("characterName", cosmeticResponse[0].name);

            JsonArray meshArray = new JsonArray();
            processedRoot.add("Meshes", meshArray);
            for (String i : MeshesList) {
                meshArray.add(System.getProperty("user.dir") + "\\UmodelExport" + i.replace("/", "\\") + ".psk");
            }

            JsonArray materialArray = new JsonArray();
            processedRoot.add("Materials", materialArray);

            // Material to Texture Parameters

            String[] validTextures = {"Diffuse", "Emissive", "M", "Normals", "SpecularMasks", "SkinFX_Mask"};

            for (int i : range(MaterialsList.toArray().length)) {

                pkg = fileProvider.loadGameFile(MaterialsList.toArray()[i].toString());
                toJson = pkg.toJson(locres);
                MaterialStructure materialStructure = GSON.fromJson(toJson, MaterialStructure.class);

                JsonObject MaterialName = new JsonObject();
                materialArray.add(MaterialName);
                String[] splitMaterialsList = MaterialsList.toArray()[i].toString().split("/");
                MaterialName.addProperty("MaterialName", splitMaterialsList[splitMaterialsList.length-1]);

                if (toJson.contains("TextureParameterValues")) {
                    for (int e : range(materialStructure.export_properties[0].TextureParameterValues.length)) {
                        String textureType = materialStructure.export_properties[0].TextureParameterValues[e].ParameterInfo.Name;
                        String textureValue = materialStructure.export_properties[0].TextureParameterValues[e].ParameterValue;

                        for (int j : range(materialStructure.import_map.length)) {
                            if (materialStructure.import_map[j].class_name.contains("Package")
                                    && materialStructure.import_map[j].object_name.contains(textureValue)
                                    && Arrays.asList(validTextures).contains(textureType)) {
                                MaterialName.addProperty(textureType, System.getProperty("user.dir") + "\\UmodelExport"
                                        + materialStructure.import_map[j].object_name.replace("/", "\\") + ".tga");
                            }
                        }
                    }
                }
            }

            // Write processed.json
            File processedFile = new File("processed.json");
            processedFile.createNewFile();
            FileWriter writer = new FileWriter(processedFile);
            writer.write(GSON.toJson(processedRoot));
            writer.close();

            try (PrintWriter printWriter = new PrintWriter("umodel_queue.txt")) {
                printWriter.println("-path=\"" + config.PaksDirectory + "\"");
                String[] SplitUEVersion = config.UEVersion.toString().split("_");
                printWriter.println("-game=ue4." + SplitUEVersion[2]);
                printWriter.println("-aes=" + config.EncryptionKey);
                printWriter.println("-export ");
                for (String meshes : MeshesList) {
                    printWriter.println("-pkg=" + meshes);
                }
            }
            Thread.sleep(5000);
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList("umodel", "@umodel_queue.txt"));
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.start().waitFor();
        } catch (Exception exception) {
            exception.printStackTrace();
        }



    }

    public static IntRange range(int max) {
        return new IntRange(0, max-1);
    }

    public static class CIDStructure {
        private importMapSelection[] import_map;
        public class importMapSelection {
            public String class_name;
            public String object_name;
        }
    }
    public static class HIDStructure {
        private Specializations[] export_properties;
        public class Specializations {
            public AssetPath[] Specializations;
        }
        public class AssetPath {
            public String assetPath;
        }
    }
    public static class HSStructure {
        private CharacterParts[] export_properties;
        public class CharacterParts {
            public AssetPath[] CharacterParts;
        }
        public class AssetPath {
            public String assetPath;
        }
    }
    public static class CPStructure {
        private SkeletalMesh[] export_properties;
        public class SkeletalMesh {
            public AssetPath SkeletalMesh;
            public OverrideMaterial[] MaterialOverrides;
        }
        public class OverrideMaterial {
            public AssetPath OverrideMaterial;
        }
        public class AssetPath {
            public String assetPath;
        }
    }
    public static class MeshStructure {
        private importMapSelection[] import_map;
        public class importMapSelection {
            public String class_name;
            public String object_name;
        }
    }
    public static class MaterialStructure {
        private importedmapstuff[] import_map;
        public class importedmapstuff {
            public String class_name;
            public String object_name;
        }

        private TextureParameterValues[] export_properties;
        public class TextureParameterValues {

            public Parameters[] TextureParameterValues;
        }
        public class Parameters {
            public ParameterInfo ParameterInfo;
            public String ParameterValue;
        }
        public class ParameterInfo {
            public String Name;
        }
    }

    public static class Config {
        private String PaksDirectory;
        private Ue4Version UEVersion;
        private String EncryptionKey;
        private boolean dumpAssets;

    }
    public static class CosmeticResponse {
        private String id;
        private String path;
        private String name;
    }
    private static class CustomException extends Exception {
        public CustomException(String message) {
            super(message);
        }
    }
}
