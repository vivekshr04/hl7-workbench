package com.eqe.hl7workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

class DictionaryLoaderTest {
    @Test
    void loadsPackagedFieldDictionary() {
        var dictionary = DictionaryLoader.loadDictionary(
                "resources/dictionaries/field_dictionary.csv");
        assertFalse(dictionary.isEmpty());
        assertEquals("Patient Name", dictionary.get("PID.5"));
    }
}
