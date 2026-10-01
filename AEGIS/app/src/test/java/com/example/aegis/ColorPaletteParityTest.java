package com.example.aegis;

import static org.junit.Assert.assertTrue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.junit.Test;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

/** Toda cor aegis_* do Stark precisa ter a versão Doom; senão o app fica metade verde, metade azul. */
public class ColorPaletteParityTest {

    // o Gradle roda os testes de unidade com a pasta do módulo (app/) como diretório atual
    private static Set<String> colorNames(String path) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
        NodeList nodes = doc.getElementsByTagName("color");
        Set<String> names = new TreeSet<>();
        for (int i = 0; i < nodes.getLength(); i++) names.add(((Element) nodes.item(i)).getAttribute("name"));
        return names;
    }

    @Test
    public void everyAegisColorHasADoomVersion() throws Exception {
        Set<String> stark = colorNames("src/main/res/values/colors.xml");
        Set<String> doom = colorNames("src/main/res/values-night/colors.xml");

        Set<String> missing = new TreeSet<>();
        for (String name : stark) if (name.startsWith("aegis_") && !doom.contains(name)) missing.add(name);
        assertTrue("Faltam no values-night/colors.xml: " + missing, missing.isEmpty());

        Set<String> onlyDoom = new TreeSet<>(doom);
        onlyDoom.removeAll(stark);
        assertTrue("Existem só no Doom (erro de digitação?): " + onlyDoom, onlyDoom.isEmpty());
    }

    @Test
    public void widgetColorsAreNeverThemed() throws Exception {
        Set<String> doom = colorNames("src/main/res/values-night/colors.xml");
        for (String name : doom) {
            assertTrue("O widget não pode mudar com o tema: " + name, !name.startsWith("widget_"));
        }
    }
}
