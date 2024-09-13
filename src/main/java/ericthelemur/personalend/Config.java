package ericthelemur.personalend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public boolean redirectPortals = false;
    public boolean endCommand = true;
    public boolean gateCommandBehindAdvancement = true;
    public boolean commandOnEndPlatformOnly = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config/personalend.json");

    public static void load() {
        try {
            try (var reader = Files.newBufferedReader(CONFIG_PATH)) {
                var config = GSON.fromJson(reader, Config.class);
                if (config != null) PersonalEnd.CONFIG = config;
            }
        } catch (IOException e) {
            PersonalEnd.LOGGER.info("Config does not exist");
        }
        try {
            try (var writer = Files.newBufferedWriter(CONFIG_PATH)) {
                Files.createDirectories(CONFIG_PATH.getParent());
                GSON.toJson(PersonalEnd.CONFIG, writer);
            }
        } catch (IOException e) {
            PersonalEnd.LOGGER.info("Error writing config {}", e.toString());
        }
        PersonalEnd.LOGGER.info("{}", PersonalEnd.CONFIG);
    }

    @Override
    public String toString() {
        return "Config{" +
                "redirectPortals=" + redirectPortals +
                ", endCommand=" + endCommand +
                ", gateCommandBehindAdvancement=" + gateCommandBehindAdvancement +
                '}';
    }
}
