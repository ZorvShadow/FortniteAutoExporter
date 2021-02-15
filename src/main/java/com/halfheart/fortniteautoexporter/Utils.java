package com.halfheart.fortniteautoexporter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kotlin.ranges.IntRange;

import java.io.IOException;

public class Utils {

    public static IntRange range(int max) {
        return new IntRange(0, max-1);
    }

    public static void clearScreen(){
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (Exception e) {}
    }

    public static class CustomException extends Exception {
        public CustomException(String errorMessage) {
            super(errorMessage);
        }
    }

}
