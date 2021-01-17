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

public class Backpack {

    private static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config;
    private static CosmeticResponse[] cosmeticResponse;
    private static DefaultFileProvider fileProvider;

    private static IoPackage pkg;
    private static long start;

    public static void promptBackpack() throws Exception {
        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, Config.class);
        }

        System.out.println("Enter Backpack Name:");
        String selection = new Scanner(System.in).nextLine().replace(" ", "%20");
        start = System.currentTimeMillis();
        String skinSelectionFormat = String.format("https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name=%s&backendType=AthenaBackpack", selection);
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

        fileProvider = new DefaultFileProvider(new File(config.PaksDirectory), config.UEVersion);
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

        processBackpack();
    }
    public static void processBackpack() {
        try {
            //BID to CP
            String toJson =  JWPSerializer.GSON.toJson(pkg.getExports());
            BIDStructure[] bidStructure = GSON.fromJson(toJson, BIDStructure[].class);
            System.out.println(bidStructure[0].CharacterParts.get(0).get(1) + ".uasset");
            pkg = (IoPackage) fileProvider.loadGameFile(bidStructure[0].CharacterParts.get(0).get(1) + ".uasset");

            // CP to Mesh to Material
            List<String> MeshesList = new ArrayList<>();
            List<overrideStructure3D> MaterialsList = new ArrayList<>();
            toJson =  JWPSerializer.GSON.toJson(pkg.getExports());
            System.out.println(toJson);
            CPStructure[] cpStructure = GSON.fromJson(toJson, CPStructure[].class);
            MeshesList.add(cpStructure[1].SkeletalMesh.asset_path_name.split("\\.")[0]);

            if (toJson.contains("OverrideMaterial")) {
                for (int j : range(cpStructure[1].MaterialOverrides.length)) {
                    MaterialsList.add(new overrideStructure3D(cpStructure[1].SkeletalMesh.asset_path_name.split("\\.")[1],
                            cpStructure[1].MaterialOverrides[j].OverrideMaterial.asset_path_name.split("\\.")[0],
                            cpStructure[1].MaterialOverrides[j].MaterialOverrideIndex,
                            "true"));
                }
            } else {
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
                scalarParameterMaterialStructure[] scalarParameterMaterialStructure = GSON.fromJson(toJson, scalarParameterMaterialStructure[].class);
                if (toJson.contains("ScalarParameterValues")) {
                    for (int e : range(scalarParameterMaterialStructure[0].ScalarParameterValues.length)) {
                        String scalarType = scalarParameterMaterialStructure[0].ScalarParameterValues[e].ParameterInfo.Name;
                        double scalarValue = scalarParameterMaterialStructure[0].ScalarParameterValues[e].ParameterValue;

                        MaterialName.addProperty(scalarType, scalarValue);
                    }
                }

                vectorParameterMaterialStructure[] vectorParameterMaterialStructure = GSON.fromJson(toJson, vectorParameterMaterialStructure[].class);
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
                Thread.sleep(5000);
                ProcessBuilder pb = new ProcessBuilder(Arrays.asList("umodel", "@umodel_queue.txt"));
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                pb.start().waitFor();
            }

            LOGGER.info(String.format("Finished Exporting in %.1f sec. Replace the line with workingDirectory in the python script with this line:\n\nworkingDirectory = r\"%s\"",  (System.currentTimeMillis() - start) / 1000.0F, new File("").getAbsolutePath()));

        } catch (Exception exception) {
            exception.printStackTrace();
        }



    }

    public static IntRange range(int max) {
        return new IntRange(0, max-1);
    }

    public static class BIDStructure {
        public List<List<String>> CharacterParts;
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
