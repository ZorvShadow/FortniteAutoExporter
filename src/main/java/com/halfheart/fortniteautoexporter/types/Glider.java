package com.halfheart.fortniteautoexporter.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.halfheart.fortniteautoexporter.JSONStructures.Config;
import com.halfheart.fortniteautoexporter.JSONStructures.CosmeticResponse;
import com.halfheart.fortniteautoexporter.Main;
import me.fungames.jfortniteparse.fort.exports.AthenaCosmeticItemDefinition;
import me.fungames.jfortniteparse.fort.exports.AthenaGliderItemDefinition;
import me.fungames.jfortniteparse.ue4.assets.exports.FSkeletalMaterial;
import me.fungames.jfortniteparse.ue4.assets.exports.USkeletalMesh;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstance;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstanceConstant;
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
import static com.halfheart.fortniteautoexporter.ProcessVariantClass.ProcessVariants;
import static com.halfheart.fortniteautoexporter.Utils.range;


public class Glider {
    public static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config config;
    public static CosmeticResponse[] cosmeticResponse;

    public static long start;

    public static boolean useGlider;

    public static String gliderName;
    public static String gliderPath;

    public static void PromptGlider(String GliderArgs) throws Exception {

        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, Config.class);
        }

        String gliderSelection = GliderArgs;

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
                PromptGlider(GliderArgs);
            }

            if (cosmeticResponse[0].path == null) {
                LOGGER.error("Invalid Glider Selection.");
                PromptGlider(GliderArgs);
            }

            gliderName = cosmeticResponse[0].name;
            gliderPath = cosmeticResponse[0].path;
        } else if (useGlider) {
            gliderName = gliderSelection;
            gliderPath = "FortniteGame/Content/Athena/Items/Cosmetics/Gliders/" + gliderName;
        }

        processGlider();
    }

    public static void processGlider() throws Exception {
        JsonObject Processed = new JsonObject();
        Processed.addProperty("ObjectName", gliderName);

        JsonArray PartArray = new JsonArray();
        Processed.add("CharacterParts", PartArray);

        List<String> ExportList = new ArrayList<>();

        AthenaGliderItemDefinition GliderItemDefinition = (AthenaGliderItemDefinition) fileProvider.loadObject(gliderPath);
        ProcessVariants(GliderItemDefinition.ItemVariants, Processed, ExportList);

        JsonObject CurrentPart = new JsonObject();
        PartArray.add(CurrentPart);

        USkeletalMesh SkeletalMesh = (USkeletalMesh) fileProvider.loadObject(GliderItemDefinition.SkeletalMesh);
        ExportList.add(SkeletalMesh.getPathName());

        CurrentPart.addProperty("Mesh", SkeletalMesh.getPathName());
        JsonArray MaterialArray = new JsonArray();
        CurrentPart.add("Materials", MaterialArray);

        for (FSkeletalMaterial SkeletalMaterial : SkeletalMesh.materials) {

            if (SkeletalMaterial.getMaterial() != null) {
                if (SkeletalMaterial.getMaterial().getValue() instanceof UMaterialInstanceConstant) {

                    UMaterialInstanceConstant Material = (UMaterialInstanceConstant) fileProvider.loadObject(SkeletalMaterial.getMaterial().getValue().getPathName());
                    if (Material != null) {
                        JsonObject CurrentMaterial = new JsonObject();
                        MaterialArray.add(CurrentMaterial);
                        CurrentMaterial.addProperty("Material", Material.getPathName());
                        ExportList.add(Material.getPathName());

                        if (Material.TextureParameterValues != null) {
                            for (UMaterialInstance.FTextureParameterValue TextureParam : Material.TextureParameterValues) {
                                try {
                                    CurrentMaterial.addProperty(TextureParam.ParameterInfo.Name.toString(), TextureParam.ParameterValue.getValue().getPathName());
                                } catch (NullPointerException e) {
                                    LOGGER.error("Param is null, skipping");
                                }
                            }
                        }
                        if (Material.ScalarParameterValues != null) {
                            for (UMaterialInstance.FScalarParameterValue ScalarParam : Material.ScalarParameterValues) {
                                try {
                                    CurrentMaterial.addProperty(ScalarParam.ParameterInfo.Name.toString(), ScalarParam.ParameterValue);;
                                } catch (NullPointerException e) {
                                    LOGGER.error("Param is null, skipping");
                                }
                            }
                        }
                        if (Material.VectorParameterValues != null) {
                            for (UMaterialInstance.FVectorParameterValue VectorParam : Material.VectorParameterValues) {
                                try {
                                    CurrentMaterial.addProperty(VectorParam.ParameterInfo.Name.toString(),
                                            VectorParam.ParameterValue.getR() + ", "
                                                    + VectorParam.ParameterValue.getG() + ", "
                                                    + VectorParam.ParameterValue.getB() + ", "
                                                    + VectorParam.ParameterValue.getA());
                                } catch (NullPointerException e) {
                                    LOGGER.error("Param is null, skipping");
                                }
                            }
                        }
                    }
                }
            }
        }
        if (GliderItemDefinition.MaterialOverrides != null) {
            for (AthenaCosmeticItemDefinition.AthenaCosmeticMaterialOverride MaterialOverrideData : GliderItemDefinition.MaterialOverrides) {
                JsonObject CurrentMaterial = new JsonObject();
                MaterialArray.add(CurrentMaterial);

                UMaterialInstanceConstant Material = (UMaterialInstanceConstant) fileProvider.loadObject(MaterialOverrideData.OverrideMaterial.getAssetPathName().toString());
                if (Material != null) {
                    CurrentMaterial.addProperty("Material", Material.getPathName());
                    CurrentMaterial.addProperty("OverrideMaterialIndex", MaterialOverrideData.MaterialOverrideIndex);
                    ExportList.add(Material.getPathName());

                    if (Material.TextureParameterValues != null) {
                        for (UMaterialInstance.FTextureParameterValue TextureParam : Material.TextureParameterValues) {
                            try {
                                CurrentMaterial.addProperty(TextureParam.ParameterInfo.Name.toString(), TextureParam.ParameterValue.getValue().getPathName());
                            } catch (NullPointerException e) {
                                LOGGER.error("Param is null, skipping");
                            }
                        }
                    }
                    if (Material.ScalarParameterValues != null) {
                        for (UMaterialInstance.FScalarParameterValue ScalarParam : Material.ScalarParameterValues) {
                            try {
                                CurrentMaterial.addProperty(ScalarParam.ParameterInfo.Name.toString(), ScalarParam.ParameterValue);;
                            } catch (NullPointerException e) {
                                LOGGER.error("Param is null, skipping");
                            }
                        }
                    }
                    if (Material.VectorParameterValues != null) {
                        for (UMaterialInstance.FVectorParameterValue VectorParam : Material.VectorParameterValues) {
                            try {
                                CurrentMaterial.addProperty(VectorParam.ParameterInfo.Name.toString(),
                                        VectorParam.ParameterValue.getR() + ", "
                                                + VectorParam.ParameterValue.getG() + ", "
                                                + VectorParam.ParameterValue.getB() + ", "
                                                + VectorParam.ParameterValue.getA());
                            } catch (NullPointerException e) {
                                LOGGER.error("Param is null, skipping");
                            }
                        }
                    }
                }
            }
        }

        File processedFile = new File("processed.json");
        processedFile.createNewFile();
        FileWriter writer = new FileWriter(processedFile);
        writer.write(GSON.toJson(Processed));
        writer.close();

        try (PrintWriter printWriter = new PrintWriter("umodel_queue.txt")) {
            printWriter.println("-path=\"" + config.PaksDirectory + "\"");
            printWriter.println("-game=ue4." + "27");
            printWriter.println("-aes=" + config.mainKey);

            for (int i : range(config.dynamicKeys.toArray().length)) {
                printWriter.println("-aes=" + config.dynamicKeys.get(i).key);
            }

            for (String mesh : ExportList) {
                printWriter.println("-pkg=" + mesh.split("\\.")[0]);
            }

            printWriter.println("-export ");
            printWriter.println("-nooverwrite ");
        }

        ProcessBuilder pb = new ProcessBuilder(Arrays.asList("umodel", "@umodel_queue.txt"));
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.start().waitFor();

        LOGGER.info(String.format("Finished Exporting in %.1f sec.", (System.currentTimeMillis() - start) / 1000.0F));
    }
}