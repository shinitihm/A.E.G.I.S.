package com.example.aegis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.aegis.data.ApiClient;
import com.example.aegis.data.model.Armor;
import com.example.aegis.data.model.ChatResponse;
import com.example.aegis.data.model.CompareResult;
import com.example.aegis.data.model.Health;
import com.example.aegis.data.model.HeroDetail;
import com.example.aegis.data.model.HeroPage;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Contrato com o backend: os JSONs em src/test/resources/contract/ são gerados pelo próprio FastAPI
 * (backend/tests/export_contract.py). Se um nome de campo mudar lá, este teste quebra aqui.
 */
public class ContractTest {

    private static <T> T read(String name, Type type) throws IOException {
        try (InputStream in = ContractTest.class.getResourceAsStream("/contract/" + name + ".json")) {
            assertNotNull("faltou o arquivo contract/" + name + ".json", in);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return ApiClient.GSON.fromJson(reader, type);
            }
        }
    }

    @Test
    public void heroDetail() throws IOException {
        HeroDetail hero = read("hero_detail", HeroDetail.class);
        assertEquals("Hulk", hero.name);
        assertEquals("Bruce Banner", hero.realName); // real_name -> realName
        assertEquals("http://img/hulk.jpg", hero.imageUrl);
        assertEquals(3000, hero.appearances);
        assertEquals("Gamma rage", hero.bio);
        assertEquals(12, hero.powers.size());
        assertEquals("HIGH", hero.threat.levelCode); // level_code -> levelCode
        assertEquals(5, hero.threat.factors.size());
        assertEquals(40, hero.threat.factors.get(0).max);
    }

    @Test
    public void heroPage() throws IOException {
        HeroPage page = read("hero_page", HeroPage.class);
        assertEquals(2, page.items.size());
        assertNull(page.nextPage); // null = fim da lista
        assertNotNull(page.items.get(0).threat); // Hulk já foi escaneado (o export abriu a ficha dele antes)
        assertEquals("HIGH", page.items.get(0).threat.levelCode);
        assertNull(page.items.get(1).threat);    // Thor ainda não
    }

    @Test
    public void compare() throws IOException {
        CompareResult r = read("compare", CompareResult.class);
        assertEquals(Integer.valueOf(1), r.winnerId);
        assertTrue(r.probabilityA > 50);
        assertEquals("Hulk", r.a.name);
        assertEquals("Thor", r.b.name);
        assertTrue(r.verdict.contains("Hulk"));
    }

    @Test
    public void armors() throws IOException {
        Type type = new TypeToken<List<Armor>>() { }.getType();
        List<Armor> armors = read("armors", type);
        assertTrue(armors.size() >= 15);
        Armor hulkbuster = null;
        for (Armor a : armors) if (a.mark == 44) hulkbuster = a;
        assertNotNull(hulkbuster);
        assertEquals("MARK XLIV", hulkbuster.code);
        assertEquals("Contenção pesada", hulkbuster.armorClass); // armor_class -> armorClass
        assertEquals(97, hulkbuster.stats.power);
        assertEquals("#B71C1C", hulkbuster.primaryColor);
        assertTrue(hulkbuster.weapons.size() > 0);
    }

    @Test
    public void chatAction() throws IOException {
        ChatResponse r = read("chat_action", ChatResponse.class);
        assertEquals("OPEN_HERO:1", r.action);
        assertEquals("OFFLINE", r.mode);
        assertTrue(r.reply.length() > 0);
    }

    @Test
    public void health() throws IOException {
        Health h = read("health", Health.class);
        assertEquals("ONLINE", h.status);
        assertEquals("OFFLINE", h.jarvis);
    }
}
