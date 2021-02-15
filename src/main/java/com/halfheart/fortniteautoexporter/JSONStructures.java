package com.halfheart.fortniteautoexporter;

import com.sun.management.UnixOperatingSystemMXBean;
import me.fungames.jfortniteparse.fort.exports.AthenaCharacterItemDefinition;
import me.fungames.jfortniteparse.fort.exports.AthenaCharacterPartItemDefinition;
import me.fungames.jfortniteparse.fort.exports.variants.FortCosmeticVariant;
import me.fungames.jfortniteparse.fort.exports.variants.FortCosmeticVariantBackedByArray;
import me.fungames.jfortniteparse.ue4.assets.exports.FSkeletalMaterial;
import me.fungames.jfortniteparse.ue4.assets.exports.UObject;
import me.fungames.jfortniteparse.ue4.assets.exports.UPrimaryDataAsset;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstance;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInstanceConstant;
import me.fungames.jfortniteparse.ue4.assets.exports.mats.UMaterialInterface;
import me.fungames.jfortniteparse.ue4.objects.uobject.FName;
import me.fungames.jfortniteparse.ue4.objects.uobject.FSoftObjectPath;
import me.fungames.jfortniteparse.ue4.versions.Ue4Version;

import java.util.List;
import java.util.Map;

public class JSONStructures {
    public static class FortHeroSpecialization extends UObject {
        public List<FSoftObjectPath> CharacterParts;

        public FortHeroSpecialization() {

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
}
