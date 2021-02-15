package com.halfheart.fortniteautoexporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kotlin.Lazy;
import me.fungames.jfortniteparse.fort.enums.EFortCustomPartType;
import me.fungames.jfortniteparse.fort.exports.CustomCharacterPart;
import me.fungames.jfortniteparse.fort.exports.variants.FortCosmeticCharacterPartVariant;
import me.fungames.jfortniteparse.fort.exports.variants.FortCosmeticMaterialVariant;
import me.fungames.jfortniteparse.fort.exports.variants.FortCosmeticVariant;
import me.fungames.jfortniteparse.fort.objects.variants.*;
import me.fungames.jfortniteparse.ue4.assets.exports.FSkeletalMaterial;
import me.fungames.jfortniteparse.ue4.assets.exports.USkeletalMesh;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstance;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstanceConstant;
import me.fungames.jfortniteparse.ue4.objects.core.i18n.FText;
import me.fungames.jfortniteparse.ue4.objects.uobject.FSoftObjectPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.halfheart.fortniteautoexporter.Main.fileProvider;
import static com.halfheart.fortniteautoexporter.Utils.range;

public class ProcessVariantClass {
    public static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void ProcessVariants(List<Lazy<FortCosmeticVariant>> ItemVariants, JsonObject Processed, List<String> Exports) {
        if (ItemVariants != null) {
            for (Lazy<FortCosmeticVariant> Variant : ItemVariants) {

                // Character Parts Variant
                if (Variant.getValue() instanceof FortCosmeticCharacterPartVariant) {
                    // Variant Selection
                    List<FText> VariantNames = new ArrayList<>();
                    System.out.println("All Variants:");
                    for (PartVariantDef VariantName : ((FortCosmeticCharacterPartVariant) Variant.getValue()).PartOptions) {
                        VariantNames.add(VariantName.VariantName);
                        System.out.println(VariantName.VariantName);
                    }
                    String selectedVariant = variantSelector(VariantNames);
                    int selectedVariantIndex = 0;
                    for (int i : range(((FortCosmeticCharacterPartVariant) Variant.getValue()).PartOptions.toArray().length)) {
                        if (selectedVariant.toLowerCase().equals(((FortCosmeticCharacterPartVariant) Variant.getValue()).PartOptions.get(i).VariantName.getText().toLowerCase())) {
                            selectedVariantIndex = i;
                        }
                    }

                    // Process Variant
                    PartVariantDef PartVariant = ((FortCosmeticCharacterPartVariant) Variant.getValue()).PartOptions.get(selectedVariantIndex);

                    JsonArray VariantPartArray = new JsonArray();
                    Processed.add("VariantCharacterParts", VariantPartArray);
                    for (FSoftObjectPath CharaterPartPath : PartVariant.VariantParts) {
                        JsonObject CurrentVariantPart = new JsonObject();
                        VariantPartArray.add(CurrentVariantPart);

                        CustomCharacterPart VariantCharacterPart = (CustomCharacterPart) fileProvider.loadObject(CharaterPartPath);
                        USkeletalMesh SkeletalMesh = (USkeletalMesh) fileProvider.loadObject(VariantCharacterPart.SkeletalMesh);
                        Exports.add(SkeletalMesh.getPathName());

                        CurrentVariantPart.addProperty("Mesh", VariantCharacterPart.SkeletalMesh.toString());
                        CurrentVariantPart.addProperty("CharacterPartType", VariantCharacterPart.CharacterPartType == null ? EFortCustomPartType.Head.name() : VariantCharacterPart.CharacterPartType.name());

                        JsonArray MaterialArray = new JsonArray();
                        CurrentVariantPart.add("Materials", MaterialArray);

                        for (FSkeletalMaterial SkeletalMaterial : SkeletalMesh.getMaterials()) {
                            JsonObject CurrentMaterial = new JsonObject();
                            MaterialArray.add(CurrentMaterial);
                            UMaterialInstanceConstant Material = (UMaterialInstanceConstant) fileProvider.loadObject(SkeletalMaterial.getMaterial().getValue().getPathName());
                            CurrentMaterial.addProperty("Material", Material.getPathName());
                            Exports.add(Material.getPathName());

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

                    JsonArray VariantMaterialArray = new JsonArray();
                    Processed.add("VariantMaterials", VariantMaterialArray);
                    for (BaseVariantDef.MaterialVariants VariantMaterial : PartVariant.VariantMaterials) {
                        JsonObject CurrentVariantMaterial = new JsonObject();
                        VariantMaterialArray.add(CurrentVariantMaterial);

                        CurrentVariantMaterial.addProperty("OriginalMaterial", VariantMaterial.MaterialToSwap.getAssetPathName().getText());
                        CurrentVariantMaterial.addProperty("VariantMaterial", VariantMaterial.OverrideMaterial.getAssetPathName().getText());

                        UMaterialInstanceConstant Material = (UMaterialInstanceConstant) fileProvider.loadObject(VariantMaterial.OverrideMaterial);
                        Exports.add(Material.getPathName());

                        if (Material.TextureParameterValues != null) {
                            for (UMaterialInstance.FTextureParameterValue TextureParam : Material.TextureParameterValues) {
                                try {
                                    CurrentVariantMaterial.addProperty(TextureParam.ParameterInfo.Name.toString(), TextureParam.ParameterValue.getValue().getPathName());
                                } catch (NullPointerException e) {
                                    LOGGER.error("Param is null, skipping");
                                }
                            }
                        }
                        if (Material.ScalarParameterValues != null) {
                            for (UMaterialInstance.FScalarParameterValue ScalarParam : Material.ScalarParameterValues) {
                                try {
                                    CurrentVariantMaterial.addProperty(ScalarParam.ParameterInfo.Name.toString(), ScalarParam.ParameterValue);;
                                } catch (NullPointerException e) {
                                    LOGGER.error("Param is null, skipping");
                                }
                            }
                        }
                        if (Material.VectorParameterValues != null) {
                            for (UMaterialInstance.FVectorParameterValue VectorParam : Material.VectorParameterValues) {
                                try {
                                    CurrentVariantMaterial.addProperty(VectorParam.ParameterInfo.Name.toString(),
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

                    JsonArray VariantParamArray = new JsonArray();
                    Processed.add("VariantParams", VariantParamArray);
                    for (BaseVariantDef.MaterialParamterDef MaterialParams : PartVariant.VariantMaterialParams) {
                        JsonObject CurrentVariantParam = new JsonObject();
                        VariantParamArray.add(CurrentVariantParam);

                        for (MaterialTextureVariant TextureParam : MaterialParams.TextureParams) {
                            CurrentVariantParam.addProperty(TextureParam.ParamName.getText(), TextureParam.Value.getAssetPathName().getText());
                            Exports.add(TextureParam.Value.getAssetPathName().getText());
                        }
                        for (MaterialFloatVariant FloatParam : MaterialParams.FloatParams) {
                            CurrentVariantParam.addProperty(FloatParam.ParamName.getText(), FloatParam.Value);
                        }
                        for (MaterialVectorVariant VectorParam : MaterialParams.ColorParams) {
                            CurrentVariantParam.addProperty(VectorParam.ParamName.getText(), VectorParam.Value.getR() + ", "
                                    + VectorParam.Value.getG() + ", "
                                    + VectorParam.Value.getB() + ", "
                                    + VectorParam.Value.getA());
                        }
                    }
                }

                // Material Variant
                if (Variant.getValue() instanceof FortCosmeticMaterialVariant) {
                    // Variant Selection
                    List<FText> VariantNames = new ArrayList<>();
                    System.out.println("All Variants:");
                    for (MaterialVariantDef VariantName : ((FortCosmeticMaterialVariant) Variant.getValue()).MaterialOptions) {
                        VariantNames.add(VariantName.VariantName);
                        System.out.println(VariantName.VariantName);
                    }
                    String selectedVariant = variantSelector(VariantNames);
                    int selectedVariantIndex = 0;
                    for (int i : range(((FortCosmeticMaterialVariant) Variant.getValue()).MaterialOptions.toArray().length)) {
                        if (selectedVariant.toLowerCase().equals(((FortCosmeticMaterialVariant) Variant.getValue()).MaterialOptions.get(i).VariantName.getText().toLowerCase())) {
                            selectedVariantIndex = i;
                        }
                    }

                    // Process Variant
                    MaterialVariantDef PartVariant = ((FortCosmeticMaterialVariant) Variant.getValue()).MaterialOptions.get(selectedVariantIndex);

                    JsonArray VariantMaterialArray = new JsonArray();
                    Processed.add("VariantMaterials", VariantMaterialArray);
                    for (BaseVariantDef.MaterialVariants VariantMaterial : PartVariant.VariantMaterials) {
                        JsonObject CurrentVariantMaterial = new JsonObject();
                        VariantMaterialArray.add(CurrentVariantMaterial);

                        CurrentVariantMaterial.addProperty("OriginalMaterial", VariantMaterial.MaterialToSwap.getAssetPathName().getText());
                        CurrentVariantMaterial.addProperty("VariantMaterial", VariantMaterial.OverrideMaterial.getAssetPathName().getText());

                        UMaterialInstanceConstant Material = (UMaterialInstanceConstant) fileProvider.loadObject(VariantMaterial.OverrideMaterial);
                        Exports.add(Material.getPathName());

                        if (Material != null) {
                            if (Material.TextureParameterValues != null) {
                                for (UMaterialInstance.FTextureParameterValue TextureParam : Material.TextureParameterValues) {
                                    try {
                                        CurrentVariantMaterial.addProperty(TextureParam.ParameterInfo.Name.toString(), TextureParam.ParameterValue.getValue().getPathName());
                                    } catch (NullPointerException e) {
                                        LOGGER.error("Param is null, skipping");
                                    }
                                }
                            }
                            if (Material.ScalarParameterValues != null) {
                                for (UMaterialInstance.FScalarParameterValue ScalarParam : Material.ScalarParameterValues) {
                                    try {
                                        CurrentVariantMaterial.addProperty(ScalarParam.ParameterInfo.Name.toString(), ScalarParam.ParameterValue);;
                                    } catch (NullPointerException e) {
                                        LOGGER.error("Param is null, skipping");
                                    }
                                }
                            }
                            if (Material.VectorParameterValues != null) {
                                for (UMaterialInstance.FVectorParameterValue VectorParam : Material.VectorParameterValues) {
                                    try {
                                        CurrentVariantMaterial.addProperty(VectorParam.ParameterInfo.Name.toString(),
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

                    JsonArray VariantParamArray = new JsonArray();
                    Processed.add("VariantParams", VariantParamArray);
                    for (BaseVariantDef.MaterialParamterDef MaterialParams : PartVariant.VariantMaterialParams) {
                        JsonObject CurrentVariantParam = new JsonObject();
                        VariantParamArray.add(CurrentVariantParam);
                        CurrentVariantParam.addProperty("MaterialToAlter", MaterialParams.MaterialToAlter.getAssetPathName().getText());

                        for (MaterialTextureVariant TextureParam : MaterialParams.TextureParams) {
                            CurrentVariantParam.addProperty(TextureParam.ParamName.getText(), TextureParam.Value.getAssetPathName().getText());
                            Exports.add(TextureParam.Value.getAssetPathName().getText());
                        }
                        for (MaterialFloatVariant FloatParam : MaterialParams.FloatParams) {
                            CurrentVariantParam.addProperty(FloatParam.ParamName.getText(), FloatParam.Value);
                        }
                        for (MaterialVectorVariant VectorParam : MaterialParams.ColorParams) {
                            CurrentVariantParam.addProperty(VectorParam.ParamName.getText(), VectorParam.Value.getR() + ", "
                                    + VectorParam.Value.getG() + ", "
                                    + VectorParam.Value.getB() + ", "
                                    + VectorParam.Value.getA());
                        }
                    }
                }
            }
        }
    }
    public static String variantSelector(List<FText> Variants) {
        System.out.println("Enter Variant Selection:");
        String variantSelection = new Scanner(System.in).nextLine();

        if (!Variants.toString().toLowerCase().contains(variantSelection.toLowerCase())) {
            System.out.println("Invalid Style Selection!");
            variantSelector(Variants);
        }

        return variantSelection;
    }
}
