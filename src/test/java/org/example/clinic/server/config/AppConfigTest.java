package org.example.clinic.server.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @BeforeEach
    @AfterEach
    void resetSingleton() throws Exception {
        Field f = AppConfig.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    void argsOverridePropertiesFile() {
        AppConfig cfg = AppConfig.initialize(new String[]{"--server.port=9999"});
        assertEquals(9999, cfg.getInt("server.port"));
    }

    @Test
    void defaultsAreLoadedFromClasspath() {
        AppConfig cfg = AppConfig.initialize(new String[]{});
        assertEquals("clinic-server", cfg.getString("security.jwt.issuer"));
    }

    @Test
    void unknownKeyHasFallback() {
        AppConfig cfg = AppConfig.initialize(new String[]{});
        assertTrue(cfg.getBoolean("does.not.exist", true));
    }
}
