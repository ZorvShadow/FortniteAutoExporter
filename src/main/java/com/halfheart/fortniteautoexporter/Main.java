package com.halfheart.fortniteautoexporter;

import com.google.gson.*;
import kotlin.io.FilesKt;
import me.fungames.jfortniteparse.ue4.versions.Ue4Version;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static com.halfheart.fortniteautoexporter.Character.*;
import static com.halfheart.fortniteautoexporter.Backpack.*;
import static com.halfheart.fortniteautoexporter.Weapon.*;
import static com.halfheart.fortniteautoexporter.Pickaxe.*;
import static com.halfheart.fortniteautoexporter.Glider.*;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("FortniteAutoExporter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] Args) throws Exception {
        updateMappings();

        File configFile = new File("config.json");

        if (!configFile.exists()) {
            LOGGER.error("Config file does not exist.");
            return;
        }

        LOGGER.info("Reading config file at " + configFile.getAbsolutePath());

        Config config;
        try (FileReader reader = new FileReader(configFile)) {
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

        while (true) selectItemType();

    }

    public static void selectItemType() throws Exception {
        System.out.println("\nCurrent Item Types:\nCharacter\nBackpack\nPickaxe\nGlider\nWeapon\n");
        System.out.println("Item Type to Export:");
        String selection = new Scanner(System.in).nextLine();
        switch (selection) {
            case "Character":
            case "character":
                promptSkin();

            case "Backpack":
            case "backpack":
                promptBackpack();

            case "Weapon":
            case "weapon":
                promptWeapon();

            case "Pickaxe":
            case "pickaxe":
                promptPickaxe();

            case "Glider":
            case "glider":
                promptGlider();

            default:
                System.out.println("Invalid Selection!");
                selectItemType();
        }
    }

    public static void updateMappings() throws Exception {
        Reader reader = new OkHttpClient().newCall(new Request.Builder().url("https://benbotfn.tk/api/v1/mappings").build()).execute().body().charStream();
        mappingsResponse[] mappingsResponse = GSON.fromJson(reader, mappingsResponse[].class);

        File mappingsFolder = new File("Mappings");
        File mappingsFile = new File(mappingsFolder, mappingsResponse[0].fileName);

        byte[] usmap = new OkHttpClient().newCall(new Request.Builder().url(mappingsResponse[0].url).build()).execute().body().bytes();

        if (!mappingsFolder.exists()) {
            mappingsFolder.mkdir();
        }
        FilesKt.writeBytes(mappingsFile, usmap);

        LOGGER.info("Mappings File: " + mappingsFile.getName());
    }

    public static class Config {
        private String PaksDirectory;
        private Ue4Version UEVersion;
        private String EncryptionKey;
        private boolean exportAssets;
        private boolean dumpMaterials;
    }

    public static class mappingsResponse {
        private String url;
        private String fileName;
    }
}