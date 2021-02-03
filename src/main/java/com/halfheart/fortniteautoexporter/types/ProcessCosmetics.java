package com.halfheart.fortniteautoexporter;

import com.google.gson.*;
import kotlin.io.FilesKt;
import me.fungames.jfortniteparse.tb24.JWPSerializer;
import me.fungames.jfortniteparse.ue4.assets.IoPackage;

import com.halfheart.fortniteautoexporter.JSONStructures.*;
import com.halfheart.fortniteautoexporter.JSONStructures.HSStructure.CharacterParts;
import com.halfheart.fortniteautoexporter.JSONStructures.Config;
import me.fungames.jfortniteparse.ue4.assets.exports.UObject;
import me.fungames.jfortniteparse.ue4.asyncloading2.FPackageObjectIndex;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.halfheart.fortniteautoexporter.Utils.*;
import static com.halfheart.fortniteautoexporter.Main.fileProvider;
import static com.halfheart.fortniteautoexporter.types.Character.processCharacter;


public class ProcessCosmetics {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String processCIDVariant(IoPackage pkg, List<String> CPVariants, List<JSONStructures.VariantMaterialList> MaterialVariants) throws Exception {
        String toJson = JWPSerializer.GSON.toJson(pkg.getExports());
        CIDStructure[] CID = GSON.fromJson(toJson, CIDStructure[].class);

        // Variants
        String variantSelection = "";
        while (true) {
            System.out.println("All Variants:");
            List<String> VariantStrings = new ArrayList<>();
            for (int i : range(CID[1].PartOptions.length)) {
                System.out.println(CID[1].PartOptions[i].VariantName.string);
                VariantStrings.add(CID[1].PartOptions[i].VariantName.string);
            }

            System.out.println("Enter Variant Selection:");
            variantSelection = new Scanner(System.in).nextLine();

            if (variantSelection.toLowerCase().equals("default")) {
                processCharacter();
            }

            if (!VariantStrings.toString().toLowerCase().contains(variantSelection.toLowerCase())) {
                System.out.println("Invalid Style Selection!");
            } else {
                break;
            }
        }

        int targetVariantIndex = 0;
        for (int i : range(CID[1].PartOptions.length)) {
            if (variantSelection.toLowerCase().equals(CID[1].PartOptions[i].VariantName.string.toLowerCase())) {
                System.out.println("Selected variant " + CID[1].PartOptions[i].VariantName.string + " at Index " + i);
                targetVariantIndex = i;
            }
        }

        for (int i : range(CID[1].PartOptions[targetVariantIndex].VariantParts.length)) {
            CPVariants.add(CID[1].PartOptions[targetVariantIndex].VariantParts[i].asset_path_name);
        }

        for (int i : range(CID[1].PartOptions[targetVariantIndex].VariantMaterials.length)) {
            CIDStructure.PartOptions.VariantMaterials VariantMat = CID[1].PartOptions[targetVariantIndex].VariantMaterials[i];
            MaterialVariants.add(new VariantMaterialList(VariantMat.MaterialToSwap.asset_path_name.split("\\.")[0], VariantMat.OverrideMaterial.asset_path_name.split("\\.")[0], VariantMat.MaterialOverrideIndex));
        }

        return "/Game/Athena/Heroes/" + CID[0].HeroDefinition[0] + "::" + variantSelection;
    }

    public static void processMaterialVariants(List<VariantMaterialList> dataList, JsonArray MaterialList) throws IOException {
        for (VariantMaterialList data : dataList) {
            IoPackage pkg = (IoPackage) fileProvider.loadGameFile(data.overrideMaterial);
            String toJson = JWPSerializer.GSON.toJson(pkg.getExports());

            if (Config.dumpMaterials) {
                File materialDumpFolder = new File("MaterialDumps");
                if (!materialDumpFolder.exists()) {
                    materialDumpFolder.mkdir();
                }
                File materialFile = new File(materialDumpFolder, data.overrideMaterial + ".json");
                FileUtils.touch(materialFile);
                FilesKt.writeBytes(materialFile, toJson.getBytes());
            }

            JsonObject MaterialArray = new JsonObject();
            MaterialList.add(MaterialArray);
            MaterialArray.addProperty("OriginalMaterial", data.originalMaterial);
            MaterialArray.addProperty("OverrideMaterial", data.overrideMaterial);
            MaterialArray.addProperty("OverrideIndex", data.overrideIndex);

            try {
                MaterialStructure[] Material = GSON.fromJson(toJson, MaterialStructure[].class);
                if (toJson.contains("TextureParameterValues")) {
                    for (int e : range(Material[0].TextureParameterValues.length)) {
                        String textureType = Material[0].TextureParameterValues[e].ParameterInfo.Name;
                        String textureValue = Material[0].TextureParameterValues[e].ParameterValue[1];
                        MaterialArray.addProperty(textureType, textureValue);
                    }
                }
                if (toJson.contains("ScalarParameterValues")) {
                    for (int e : range(Material[0].ScalarParameterValues.length)) {
                        String textureType = Material[0].ScalarParameterValues[e].ParameterInfo.Name;
                        double textureValue = Material[0].ScalarParameterValues[e].ParameterValue;
                        MaterialArray.addProperty(textureType, textureValue);
                    }
                }

                if (toJson.contains("VectorParameterValues")) {
                    for (int e : range(Material[0].VectorParameterValues.length)) {
                        String vectorType = Material[0].VectorParameterValues[e].ParameterInfo.Name;
                        String vectorValues = Material[0].VectorParameterValues[e].ParameterValue.r
                                + "," + Material[0].VectorParameterValues[e].ParameterValue.g
                                + "," + Material[0].VectorParameterValues[e].ParameterValue.b
                                + "," + Material[0].VectorParameterValues[e].ParameterValue.a;

                        MaterialArray.addProperty(vectorType, vectorValues);
                    }
                }
            } catch (JsonSyntaxException e) {}
        }
    }

    public static String processWID(IoPackage pkg) throws Exception {
        String toJson = JWPSerializer.GSON.toJson(pkg.getExports());
        if (!toJson.contains("WeaponMeshOverride") || !toJson.contains("PickupSkeletalMesh")) {
            throw new CustomException("Weapon does not contain mesh.");
        }
        WIDStructure[] WID = GSON.fromJson(toJson, WIDStructure[].class);
        if (toJson.contains("WeaponMeshOverride")) {
            return WID[0].WeaponMeshOverride.asset_path_name.split("\\.")[0];
        } else {
            return WID[0].PickupSkeletalMesh.asset_path_name.split("\\.")[0];
        }
    }

    public static String processBID(IoPackage pkg) {
        String toJson =  JWPSerializer.GSON.toJson(pkg.getExports());
        BIDStructure[] BID = GSON.fromJson(toJson, BIDStructure[].class);
        return BID[0].CharacterParts.get(0).get(1);
    }

    public static String processCID(IoPackage pkg) {
        String toJson = JWPSerializer.GSON.toJson(pkg.getExports());
        CIDStructure[] CID = GSON.fromJson(toJson, CIDStructure[].class);
        return "/Game/Athena/Heroes/" + CID[0].HeroDefinition[0];
    }

    public static String processHID(IoPackage pkg) {
        String toJson = JWPSerializer.GSON.toJson(pkg.getExports());
        HIDStructure[] HID = GSON.fromJson(toJson, HIDStructure[].class);
        return HID[0].Specializations[0].asset_path_name.split("\\.")[0];
    }

    public static CharacterParts[] processHS(IoPackage pkg) {
        String toJson = JWPSerializer.GSON.toJson(pkg.getExports());
        HSStructure[] HS = GSON.fromJson(toJson, HSStructure[].class);
        return HS[0].CharacterParts;
    }

    public static void processCP(IoPackage pkg, List<MeshMaterialData> dataList, int CPIndex) {
        String toJson = JWPSerializer.GSON.toJson(pkg.getExports());
        CPStructure[] CP = GSON.fromJson(toJson, CPStructure[].class);
        String[] skelMesh = CP[CPIndex].SkeletalMesh.asset_path_name.split("\\.");

        if (toJson.contains("OverrideMaterial")) {
            for (int matOverrideIndex : range(CP[CPIndex].MaterialOverrides.length)) {
                String[] mat = CP[CPIndex].MaterialOverrides[matOverrideIndex].OverrideMaterial.asset_path_name.split("\\.");
                dataList.add(new MeshMaterialData(skelMesh[0],
                        skelMesh[1],
                        CP[CPIndex].MaterialOverrides[matOverrideIndex].MaterialOverrideIndex,
                        mat[0],
                        mat[1],
                        CP[CPIndex].CharacterPartType));
            }
        }
        pkg = (IoPackage) fileProvider.loadGameFile(skelMesh[0]);
        for (FPackageObjectIndex importSrc : pkg.getImportMap()) {
            if (importSrc.isNull()) break;
            IoPackage.ResolvedObject resolvedIndex = pkg.resolveObjectIndex(importSrc, true);
            if (resolvedIndex.getPkg().toString().contains("Material")) {
                String[] mat = resolvedIndex.getPkg().toString().split("/");
                dataList.add(new MeshMaterialData(skelMesh[0], skelMesh[1], -1, resolvedIndex.getPkg().toString(), mat[mat.length - 1],
                        CP[CPIndex].CharacterPartType == null ? "EFortCustomPartType::Other" : CP[CPIndex].CharacterPartType));
            }
        }
    }

    public static void processMesh(IoPackage pkg, List<MeshMaterialData> dataList) {
        String[] skelMesh = pkg.getPathName().split("/");

        for (FPackageObjectIndex importSrc : pkg.getImportMap()) {
            if (importSrc.isNull()) break;
            IoPackage.ResolvedObject resolvedIndex = pkg.resolveObjectIndex(importSrc, true);
            if (resolvedIndex.getPkg().toString().contains("Material")) {
                String[] mat = resolvedIndex.getPkg().toString().split("/");
                dataList.add(new MeshMaterialData(pkg.getPathName(), skelMesh[skelMesh.length - 1], -1, resolvedIndex.getPkg().toString(), mat[mat.length - 1],
                        "NONE"));
            }
        }
    }

    public static void processMaterial(List<MeshMaterialData> dataList, JsonArray MaterialList) throws IOException {
        for (MeshMaterialData data : dataList) {
            IoPackage pkg = (IoPackage) fileProvider.loadGameFile(data.materialPath);
            String toJson = JWPSerializer.GSON.toJson(pkg.getExports());

            if (Config.dumpMaterials) {
                File materialDumpFolder = new File("MaterialDumps");
                if (!materialDumpFolder.exists()) {
                    materialDumpFolder.mkdir();
                }
                File materialFile = new File(materialDumpFolder, data.materialPath + ".json");
                FileUtils.touch(materialFile);
                FilesKt.writeBytes(materialFile, toJson.getBytes());
            }

            JsonObject MaterialArray = new JsonObject();
            MaterialList.add(MaterialArray);
            MaterialArray.addProperty("MaterialName", data.materialName);
            MaterialArray.addProperty("MaterialPath", data.materialPath);
            MaterialArray.addProperty("OverrideIndex", data.overrideIndex);
            MaterialArray.addProperty("TargetMesh", data.meshName);

            try {
                MaterialStructure[] Material = GSON.fromJson(toJson, MaterialStructure[].class);
                if (toJson.contains("TextureParameterValues")) {
                    for (int e : range(Material[0].TextureParameterValues.length)) {
                        String textureType = Material[0].TextureParameterValues[e].ParameterInfo.Name;
                        String textureValue = Material[0].TextureParameterValues[e].ParameterValue[1];
                        MaterialArray.addProperty(textureType, textureValue);
                    }
                }
                if (toJson.contains("ScalarParameterValues")) {
                    for (int e : range(Material[0].ScalarParameterValues.length)) {
                        String textureType = Material[0].ScalarParameterValues[e].ParameterInfo.Name;
                        double textureValue = Material[0].ScalarParameterValues[e].ParameterValue;
                        MaterialArray.addProperty(textureType, textureValue);
                    }
                }

                if (toJson.contains("VectorParameterValues")) {
                    for (int e : range(Material[0].VectorParameterValues.length)) {
                        String vectorType = Material[0].VectorParameterValues[e].ParameterInfo.Name;
                        String vectorValues = Material[0].VectorParameterValues[e].ParameterValue.r
                                + "," + Material[0].VectorParameterValues[e].ParameterValue.g
                                + "," + Material[0].VectorParameterValues[e].ParameterValue.b
                                + "," + Material[0].VectorParameterValues[e].ParameterValue.a;

                        MaterialArray.addProperty(vectorType, vectorValues);
                    }
                }
            } catch (JsonSyntaxException e) {}
        }
    }
}
