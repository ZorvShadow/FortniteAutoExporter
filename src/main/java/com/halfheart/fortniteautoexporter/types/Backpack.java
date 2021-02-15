package com.halfheart.fortniteautoexporter.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.halfheart.fortniteautoexporter.JSONStructures;
import com.halfheart.fortniteautoexporter.Main;
import me.fungames.jfortniteparse.fort.enums.EFortCustomPartType;
import me.fungames.jfortniteparse.fort.exports.AthenaBackpackItemDefinition;
import me.fungames.jfortniteparse.fort.exports.CustomCharacterPart;
import me.fungames.jfortniteparse.ue4.assets.exports.FSkeletalMaterial;
import me.fungames.jfortniteparse.ue4.assets.exports.USkeletalMesh;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstance;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstanceConstant;
import me.fungames.jfortniteparse.ue4.objects.uobject.FPackageIndex;
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

public class Backpack {

    private static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static JSONStructures.Config config;
    public static long start;
    public static JSONStructures.CosmeticResponse[] cosmeticResponse;

    public static boolean useBID;

    public static String backpackName;
    public static String backpackPath;

    public static void PromptBackpack(String BackpackArgs) throws Exception {
        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, JSONStructures.Config.class);
        }
        String backpackSelection = BackpackArgs;

        useBID = backpackSelection.contains("BID_");

        // Path and Name by Selection
        start = System.currentTimeMillis();

        if (!useBID) {
            String skinSelectionFormat =
                    String.format("https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name=%s&backendType=AthenaBackpack",
                            backpackSelection);
            Reader reader = new OkHttpClient().newCall(new Request.Builder().url(skinSelectionFormat).build()).execute().body().charStream();
            cosmeticResponse = GSON.fromJson(reader, JSONStructures.CosmeticResponse[].class);
            reader.close();

            if (cosmeticResponse.length == 0) {
                LOGGER.error("Backpack Not Found.");
                PromptBackpack(BackpackArgs);
            }

            if (cosmeticResponse[0].path == null) {
                LOGGER.error("Invalid Backpack Selection.");
                PromptBackpack(BackpackArgs);
            }

            backpackName = cosmeticResponse[0].name;
            backpackPath = cosmeticResponse[0].path;

        } else if (useBID) {
            backpackName = backpackSelection;
            backpackPath = "FortniteGame/Content/Athena/Items/Cosmetics/Backpacks/" + backpackName;
        }

        ProcessBackpack();

    }

    public static void ProcessBackpack() throws Exception {
        JsonObject Processed = new JsonObject();
        Processed.addProperty("ObjectName", backpackName);

        JsonArray PartArray = new JsonArray();
        Processed.add("CharacterParts", PartArray);

        List<String> ExportList = new ArrayList<>();

        AthenaBackpackItemDefinition BackpackItemDefinition = (AthenaBackpackItemDefinition) fileProvider.loadObject(backpackPath);
        ProcessVariants(BackpackItemDefinition.ItemVariants, Processed, ExportList);
        for (FPackageIndex CharacterPartPath : BackpackItemDefinition.CharacterParts) {
            JsonObject CurrentPart = new JsonObject();
            PartArray.add(CurrentPart);

            CustomCharacterPart CharacterPart = (CustomCharacterPart) fileProvider.loadObject(CharacterPartPath.load().getPathName());
            USkeletalMesh SkeletalMesh = (USkeletalMesh) fileProvider.loadObject(CharacterPart.SkeletalMesh);
            ExportList.add(SkeletalMesh.getPathName());

            CurrentPart.addProperty("Mesh", CharacterPart.SkeletalMesh.toString());
            CurrentPart.addProperty("CharacterPartType", CharacterPart.CharacterPartType == null ? EFortCustomPartType.Backpack.name() : CharacterPart.CharacterPartType.name());

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
            if (CharacterPart.MaterialOverrides != null) {
                for (CustomCharacterPart.CustomPartMaterialOverrideData MaterialOverrideData : CharacterPart.MaterialOverrides) {

                    UMaterialInstanceConstant Material = (UMaterialInstanceConstant) fileProvider.loadObject(MaterialOverrideData.OverrideMaterial.getAssetPathName().toString());
                    if (Material != null) {
                        JsonObject CurrentMaterial = new JsonObject();
                        MaterialArray.add(CurrentMaterial);
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
}
