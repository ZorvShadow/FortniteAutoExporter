package com.halfheart.fortniteautoexporter.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.halfheart.fortniteautoexporter.JSONStructures;
import com.halfheart.fortniteautoexporter.Main;
import me.fungames.jfortniteparse.ue4.assets.exports.FSkeletalMaterial;
import me.fungames.jfortniteparse.ue4.assets.exports.UObject;
import me.fungames.jfortniteparse.ue4.assets.exports.USkeletalMesh;
import me.fungames.jfortniteparse.ue4.assets.exports.UStaticMesh;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstance;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstanceConstant;
import me.fungames.jfortniteparse.ue4.assets.objects.meshes.FStaticMaterial;
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
import static com.halfheart.fortniteautoexporter.Utils.range;

public class Mesh {
    public static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static JSONStructures.Config config;
    public static long start;

    public static String meshName;
    public static String meshPath;

    public static void ProcessMesh(String MeshArgs) throws Exception {
        try (FileReader reader = new FileReader(new File("config.json"))) {
            config = GSON.fromJson(reader, JSONStructures.Config.class);
        }
        String meshSelection = MeshArgs;

        // Path and Name by Selection
        start = System.currentTimeMillis();

        String[] meshSplit = meshSelection.split("/");
        meshName = meshSplit[meshSplit.length - 1];
        meshPath = meshSelection;

        List<String> ExportList = new ArrayList<>();

        JsonObject Processed = new JsonObject();
        Processed.addProperty("ObjectName", meshName);

        JsonArray PartArray = new JsonArray();
        Processed.add("CharacterParts", PartArray);

        UObject MeshObject = fileProvider.loadObject(meshPath);
        if (MeshObject instanceof USkeletalMesh) {
            JsonObject CurrentPart = new JsonObject();
            PartArray.add(CurrentPart);

            USkeletalMesh SkeletalMesh = (USkeletalMesh) MeshObject;
            CurrentPart.addProperty("Mesh", SkeletalMesh.getPathName());

            JsonArray MaterialArray = new JsonArray();
            CurrentPart.add("Materials", MaterialArray);

            for (FSkeletalMaterial SkeletalMaterial : SkeletalMesh.materials) {
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
        if (MeshObject instanceof UStaticMesh) {
            JsonObject CurrentPart = new JsonObject();
            PartArray.add(CurrentPart);

            UStaticMesh StaticMesh = (UStaticMesh) MeshObject;
            CurrentPart.addProperty("Mesh", StaticMesh.getPathName());
            ExportList.add(StaticMesh.getPathName());

            JsonArray MaterialArray = new JsonArray();
            CurrentPart.add("Materials", MaterialArray);

            for (FStaticMaterial StaticMaterial : StaticMesh.StaticMaterials) {
                JsonObject CurrentMaterial = new JsonObject();
                MaterialArray.add(CurrentMaterial);
                UMaterialInstanceConstant Material = (UMaterialInstanceConstant) fileProvider.loadObject(StaticMaterial.materialInterface.getValue().getPathName());
                CurrentMaterial.addProperty("Material", Material.getPathName());
                ExportList.add(Material.getPathName());

                if (Material.TextureParameterValues != null) {
                    for (UMaterialInstance.FTextureParameterValue TextureParam : Material.TextureParameterValues) {
                        try {
                            CurrentMaterial.addProperty(TextureParam.ParameterInfo.Name.toString(), TextureParam.ParameterValue.getValue().getPathName());
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (Material.ScalarParameterValues != null) {
                    for (UMaterialInstance.FScalarParameterValue ScalarParam : Material.ScalarParameterValues) {
                        CurrentMaterial.addProperty(ScalarParam.ParameterInfo.Name.toString(), ScalarParam.ParameterValue);
                    }
                }
                if (Material.VectorParameterValues != null) {
                    for (UMaterialInstance.FVectorParameterValue VectorParam : Material.VectorParameterValues) {
                        CurrentMaterial.addProperty(VectorParam.ParameterInfo.Name.toString(),
                                VectorParam.ParameterValue.getR() + ", "
                                        + VectorParam.ParameterValue.getG() + ", "
                                        + VectorParam.ParameterValue.getB() + ", "
                                        + VectorParam.ParameterValue.getA());
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
