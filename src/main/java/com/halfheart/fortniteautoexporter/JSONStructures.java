package com.halfheart.fortniteautoexporter;

import me.fungames.jfortniteparse.ue4.versions.Ue4Version;

import java.util.List;
import java.util.Map;

public class JSONStructures {

    public static class PickaxeIDStructure {
        public String[] WeaponDefinition;
    }

    public static class WIDStructure {
        public AssetPath PickupSkeletalMesh;
        public AssetPath WeaponMeshOverride;

        public class AssetPath {
            public String asset_path_name;
        }
    }

    public static class BIDStructure {
        public List<List<String>> CharacterParts;
    }

    public static class CIDStructure {
        public String[] HeroDefinition;

        public PartOptions[] PartOptions;

        public class PartOptions {
            public VariantParts[] VariantParts;
            public class VariantParts {
                public String asset_path_name;
            }

            public VariantMaterials[] VariantMaterials;
            public class VariantMaterials {
                public AssetPath MaterialToSwap;
                public int MaterialOverrideIndex;
                public AssetPath OverrideMaterial;

                public class AssetPath {
                    public String asset_path_name;
                }
            }


            public VariantName VariantName;
            public class VariantName {
                public String string;
            }
        }
    }

    public static class HIDStructure {
        public Specializations[] Specializations;
        public class Specializations {
            public String asset_path_name;
        }
    }
    public static class HSStructure {
        public CharacterParts[] CharacterParts;
        public class CharacterParts {
               public String asset_path_name;
        }
    }

    public static class CPStructure {
        public String CharacterPartType;
        public SkeletalMesh SkeletalMesh;
        public materialOverride[] MaterialOverrides;
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

    public static class MaterialStructure {
        public ScalarParameters[] ScalarParameterValues;
        public TextureParameters[] TextureParameterValues;
        public VectorParameters[] VectorParameterValues;


        public class TextureParameters {
            public ParameterInfo ParameterInfo;
            public String[] ParameterValue;
            public class ParameterInfo {
                public String Name;
            }
        }

        public class ScalarParameters {
            public ParameterInfo ParameterInfo;
            public double ParameterValue;
            public class ParameterInfo {
                public String Name;
            }
        }

        public class VectorParameters {
            public ParameterInfo ParameterInfo;
            public ParameterValue ParameterValue;
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
    }

    public static class Config {
        public String PaksDirectory;
        public Ue4Version UEVersion;
        public String mainKey;
        public List<Keys> dynamicKeys;
        public boolean exportAssets;
        public static boolean dumpMaterials;

        public class Keys {
            public String fileName;
            public String key;
        }

    }
    public class CosmeticResponse {
        public String id;
        public String path;
        public String name;
    }

    public static class mappingsResponse {
        public String url;
        public String fileName;
    }

    public static class aesResponse {
        public String mainKey;
        public Map<String, String> dynamicKeys;
    }

    public static class MeshMaterialData {
        public String meshPath;
        public String meshName;
        public int overrideIndex;
        public String materialPath;
        public String materialName;
        public String CPType;

        public MeshMaterialData(String meshPath, String meshName, int overrideIndex, String materialPath, String materialName, String CPType) {
            this.meshPath = meshPath;
            this.meshName = meshName;
            this.overrideIndex = overrideIndex;
            this.materialPath = materialPath;
            this.materialName = materialName;
            this.CPType = CPType;
        }

    }

    public static class VariantMaterialList {
        public String originalMaterial;
        public String overrideMaterial;
        public int overrideIndex;

        public VariantMaterialList(String originalMaterial, String overrideMaterial, int overrideIndex) {
            this.originalMaterial = originalMaterial;
            this.overrideMaterial = overrideMaterial;
            this.overrideIndex = overrideIndex;
        }

    }
}
