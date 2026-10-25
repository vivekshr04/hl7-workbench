package com.eqe.hl7workbench;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Loads dictionary CSV files from an external path or the application classpath. */
public final class DictionaryLoader {
    private DictionaryLoader() {
    }

    public static Map<String, String> loadDictionary(String filePath) {
        Map<String, String> dictionary = new HashMap<>();
        try (InputStream stream = openDictionary(filePath);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.readLine(); // Header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    dictionary.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException exception) {
            System.err.println("Unable to load dictionary: " + filePath);
            exception.printStackTrace();
        }
        return dictionary;
    }

    static InputStream openDictionary(String filePath) throws IOException {
        File external = new File(filePath);
        if (external.isFile()) {
            return new FileInputStream(external);
        }
        String resourcePath = filePath.startsWith("/") ? filePath : "/" + filePath;
        InputStream packaged = DictionaryLoader.class.getResourceAsStream(resourcePath);
        if (packaged == null) {
            throw new IOException("Dictionary not found: " + filePath);
        }
        return packaged;
    }
}
