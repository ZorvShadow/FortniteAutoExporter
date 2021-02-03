package com.halfheart.fortniteautoexporter;

import com.google.gson.*;
import kotlin.io.FilesKt;
import me.fungames.jfortniteparse.fileprovider.DefaultFileProvider;
import me.fungames.jfortniteparse.ue4.assets.mappings.UsmapTypeMappingsProvider;
import me.fungames.jfortniteparse.ue4.objects.core.misc.FGuid;
import me.fungames.jfortniteparse.ue4.pak.PakFileReader;
import me.fungames.jfortniteparse.ue4.versions.Ue4Version;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import com.halfheart.fortniteautoexporter.JSONStructures.*;

import static com.halfheart.fortniteautoexporter.Utils.range;
import static com.halfheart.fortniteautoexporter.types.Character.*;
import static com.halfheart.fortniteautoexporter.types.Backpack.*;
import static com.halfheart.fortniteautoexporter.types.Glider.*;
import static com.halfheart.fortniteautoexporter.types.Mesh.*;
import static com.halfheart.fortniteautoexporter.types.Weapon.*;
import static com.halfheart.fortniteautoexporter.types.Pickaxe.*;
import static com.halfheart.fortniteautoexporter.Utils.clearScreen;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static DefaultFileProvider fileProvider;

    public static void main(String[] Args) throws Exception {
        String mappingsName = "";
        try {
            mappingsName = updateMappings();
            AutoAES.getKeys();
        } catch (Exception e) {
            LOGGER.error("Error Reading Keys/Mappings...");
        }

        File configFile = new File("config.json");

        if (!configFile.exists()) {
            LOGGER.error("Config file does not exist.");
            return;
        }

        Config config;
        try (FileReader reader = new FileReader(configFile)) {
            config = GSON.fromJson(reader, Config.class);
        }

        File pakDir = new File(config.PaksDirectory);

        if (!pakDir.exists()) {
            LOGGER.error("Directory " + pakDir.getAbsolutePath() + " does not exist.");
            return;
        }

        if (config.UEVersion == null || !Arrays.toString(Ue4Version.values()).contains(config.UEVersion.toString())) {
            LOGGER.error("Invalid Unreal Version.");
        }

        fileProvider = new DefaultFileProvider(new File(config.PaksDirectory), config.UEVersion);
        fileProvider.submitKey(FGuid.Companion.getMainGuid(), config.mainKey);

        for (int i : range(config.dynamicKeys.toArray().length)) {
            if (config.dynamicKeys.get(i).fileName != null && !config.dynamicKeys.get(i).fileName.isEmpty()) {
                Optional<PakFileReader> GuidGet = fileProvider.getUnloadedPaks().stream().filter(it -> it.getFileName().equals(config.dynamicKeys.get(i).fileName)).findFirst();
                if (GuidGet.isPresent()) {
                    FGuid Guid = GuidGet.get().getPakInfo().getEncryptionKeyGuid();
                    fileProvider.submitKey(Guid, config.dynamicKeys.get(i).key);
                }
            }
        }

        UsmapTypeMappingsProvider mappingsProvider = new UsmapTypeMappingsProvider(new File(System.getProperty("user.dir") + "/Mappings/" + mappingsName));
        mappingsProvider.reload();
        fileProvider.setMappingsProvider(mappingsProvider);
        clearScreen();

        LOGGER.info("Mappings File: " + mappingsName);
        LOGGER.info("Reading config file at " + configFile.getAbsolutePath());
        LOGGER.info("Unreal Version: " + config.UEVersion);
        LOGGER.info("Pak Directory: " + pakDir.getAbsolutePath());

        while (true) selectItemType();

    }

    public static void selectItemType() throws Exception {
        Thread.sleep(1000);
        System.out.println("\nCurrent Item Types:\nBackpack (BAK)\nCharacter (CHA)\nGlider (GLI)\nPickaxe (PIC)\nWeapon (WEP)\nMesh\n");
        System.out.println("Item Type to Export:");
        String selection = new Scanner(System.in).nextLine();

        try {
            if (selection.equalsIgnoreCase("character") || (selection.equalsIgnoreCase("cha"))) {
                promptCharacter();
            } else if (selection.equalsIgnoreCase("backpack") || (selection.equalsIgnoreCase("bak"))){
                promptBackpack();
            } else if (selection.equalsIgnoreCase("glider") || (selection.equalsIgnoreCase("gli"))){
                promptGlider();
            } else if (selection.equalsIgnoreCase("weapon") || (selection.equalsIgnoreCase("wep"))){
                promptWeapon();
            } else if (selection.equalsIgnoreCase("pickaxe") || (selection.equalsIgnoreCase("pic"))){
                promptPickaxe();
            } else if (selection.equalsIgnoreCase("mesh")){
                processMeshType();
            } else {
                System.out.println("Invalid Selection!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            selectItemType();
        }
    }

    public static String updateMappings() throws Exception {
        Reader reader = new OkHttpClient().newCall(new Request.Builder().url("https://benbotfn.tk/api/v1/mappings").build()).execute().body().charStream();
        mappingsResponse[] mappingsResponse = GSON.fromJson(reader, mappingsResponse[].class);

        File mappingsFolder = new File("Mappings");
        File mappingsFile = new File(mappingsFolder, mappingsResponse[0].fileName);

        byte[] usmap = new OkHttpClient().newCall(new Request.Builder().url(mappingsResponse[0].url).build()).execute().body().bytes();

        if (!mappingsFolder.exists()) {
            mappingsFolder.mkdir();
        }
        FilesKt.writeBytes(mappingsFile, usmap);

        return mappingsFile.getName();
    }
}