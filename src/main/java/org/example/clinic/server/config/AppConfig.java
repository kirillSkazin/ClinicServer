package org.example.clinic.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;


public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final String DEFAULT_RESOURCE = "application.properties";
    private static final String ENV_PREFIX = "APP_";

    private static volatile AppConfig instance;

    private final Map<String, String> values = new LinkedHashMap<>();

    private AppConfig() {
    }

    public static AppConfig get() {
        AppConfig local = instance;
        if (local == null) {
            throw new IllegalStateException(
                    "AppConfig is not initialized. Call AppConfig.initialize(args) first.");
        }
        return local;
    }

    public static AppConfig initialize(String[] args) {
        AppConfig local = instance;
        if (local == null) {
            synchronized (AppConfig.class) {
                local = instance;
                if (local == null) {
                    local = new AppConfig();
                    local.load(args);
                    instance = local;
                }
            }
        }
        return local;
    }

    private void load(String[] args) {
        loadFromClasspath(DEFAULT_RESOURCE);
        String externalFile = System.getProperty("config.file");
        if (externalFile != null && !externalFile.isBlank()) {
            loadFromFileSystem(externalFile);
        }
        loadFromSystemProperties();
        loadFromEnvironment();
        loadFromArgs(args);
        log.info("Configuration loaded: {} keys", values.size());
    }

    private void loadFromClasspath(String resource) {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("Resource '{}' not found in classpath, using defaults from other sources",
                        resource);
                return;
            }
            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            p.forEach((k, v) -> values.put(k.toString(), v.toString()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load classpath config: " + resource, e);
        }
    }

    private void loadFromFileSystem(String path) {
        try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of(path))) {
            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            p.forEach((k, v) -> values.put(k.toString(), v.toString()));
            log.info("Loaded external config from {}", path);
        } catch (IOException e) {
            log.warn("Cannot read external config '{}': {}", path, e.getMessage());
        }
    }

    private void loadFromSystemProperties() {
        System.getProperties().forEach((k, v) -> {
            String key = k.toString();
            if (values.containsKey(key)) {
                values.put(key, v.toString());
            }
        });
    }

    private void loadFromEnvironment() {
        Map<String, String> env = System.getenv();
        for (String key : values.keySet()) {
            String envKey = ENV_PREFIX + key
                    .replace('.', '_')
                    .replace('-', '_')
                    .toUpperCase();
            String envValue = env.get(envKey);
            if (envValue != null) {
                values.put(key, envValue);
            }
        }
    }

    private void loadFromArgs(String[] args) {
        if (args == null) {
            return;
        }
        for (String raw : args) {
            if (raw == null) {
                continue;
            }
            String arg = raw.startsWith("--") ? raw.substring(2) : raw;
            int eq = arg.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = arg.substring(0, eq).trim();
            String value = arg.substring(eq + 1).trim();
            if (!key.isEmpty()) {
                values.put(key, value);
            }
        }
    }

    public String getString(String key) {
        return Objects.requireNonNull(values.get(key),
                "Missing config value for key: " + key);
    }

    public String getString(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public int getInt(String key, int defaultValue) {
        String v = values.get(key);
        return v == null ? defaultValue : Integer.parseInt(v);
    }

    public long getLong(String key, long defaultValue) {
        String v = values.get(key);
        return v == null ? defaultValue : Long.parseLong(v);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String v = values.get(key);
        return v == null ? defaultValue : Boolean.parseBoolean(v);
    }

    public Map<String, String> asMap() {
        return Map.copyOf(values);
    }
}
