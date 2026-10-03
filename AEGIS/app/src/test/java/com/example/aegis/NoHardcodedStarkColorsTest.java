package com.example.aegis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Hex do tema Stark só pode existir em colors.xml; no resto, a cor vem de recurso e segue o tema. */
public class NoHardcodedStarkColorsTest {

    private static final Pattern STARK_HEX =
            Pattern.compile("00E5FF|05070D|0B1220|7FA3B0|B2FFFF|26FFB300|80FFB300", Pattern.CASE_INSENSITIVE);

    // fora do tema por natureza: o sistema desenha o ícone do launcher e o fundo do widget; ThemeMode guarda o clarão
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
            "colors.xml", "ic_launcher_foreground.xml", "ic_launcher_background.xml", "bg_widget.xml", "ThemeMode.java"));

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void noStarkHexOutsideTheColorFiles() throws IOException {
        List<File> files;
        try (Stream<java.nio.file.Path> walk = Files.walk(new File("src/main").toPath())) {
            files = walk.filter(Files::isRegularFile).map(java.nio.file.Path::toFile)
                    .filter(f -> f.getName().endsWith(".java") || f.getName().endsWith(".xml"))
                    .collect(Collectors.toList());
        }
        List<String> offenders = new ArrayList<>();
        for (File f : files) {
            if (ALLOWED.contains(f.getName())) continue;
            if (STARK_HEX.matcher(read(f)).find()) offenders.add(f.getPath().replace('\\', '/'));
        }
        assertTrue("Cor do tema Stark fixa no código (use @color/ ou R.color): " + offenders, offenders.isEmpty());
    }

    @Test
    public void widgetLayoutUsesOnlyWidgetColors() throws IOException {
        String layout = read(new File("src/main/res/layout/widget_target.xml"));
        assertFalse("O widget deve usar @color/widget_*, que não muda com o tema", layout.contains("@color/aegis_"));
    }
}
