/*-
 * ========================LICENSE_START=================================
 * UniversalDB
 * ---
 * Copyright (C) 2014 - 2025 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.universaldb.index.translation;

import org.apache.commons.collections4.splitmap.AbstractIterableGetMapDecorator;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.teamapps.datamodel.testdb1.FieldTest;
import org.teamapps.universaldb.index.text.TextFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;

public class TranslatableTextTest {

    @BeforeClass
    public static void init() throws Exception {
//        TranslatableText.getStandardVersion() = 0;
    }

    @Test
    public void invalidConstruction() {
        try {
            TranslatableText invalid = new TranslatableText("text-en", null);
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Error: no language for translatable text"));
        }
        try {
            TranslatableText invalid = new TranslatableText("text-en", "english");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Error: language is not an iso code"));
        }
        Map<String, String> translationMap = new HashMap<>();
    }

    @Test
    public void getTranslation() {
        TranslatableText text = new TranslatableText("text-en", "en");
        text.setTranslation("text-de", "de");
        text.setTranslation("text-fr", "fr");

        assertEquals("text-en", text.toString());
        assertEquals("text-en", text.getText("en"));
        assertEquals("text-en", text.getText("xx"));
        assertEquals("text-de", text.getText("de"));
        assertEquals("text-fr", text.getText("fr"));
	    assertNull("es is not a translated language", text.getTranslation("es"));
        assertEquals("text-en", text.getText("es"));
        assertEquals("no-value", text.getText("es", "no-value"));

        assertEquals("en", text.getOriginalLanguage());
        assertTrue(text.getTranslationMap().containsKey("de"));
        assertTrue(text.contains("en"));
        assertTrue(text.getLanguages().contains("en"));
        assertTrue(text.getLanguages().contains("de"));

        String encodedValue = text.getEncodedValue();
        assertNotNull(encodedValue);
        text = new TranslatableText(encodedValue);
        assertEquals("text-en", text.getText("en"));
        assertEquals("text-en", text.getText("xx"));
        assertEquals("text-de", text.getText("de"));
        assertEquals("text-fr", text.getText("fr"));

        assertTrue(text.getTranslationMap().containsKey("de"));
        assertTrue(text.contains("en"));

        assertEquals("en", text.getLanguages().getFirst());
        assertEquals(3, text.getLanguages().size());

        assertEquals("text-en", text.getText(List.of("en", "fr")));
        assertEquals("text-fr", text.getText(List.of("fr", "en")));
        assertEquals("text-de", text.getText(List.of("de", "fr")));
        assertEquals("text-fr", text.getText(List.of("fr", "de")));

        assertTrue(text.hasTranslations());
        assertEquals(2, text.translationsCount());  // original = en, translations = de,fr
        assertFalse(text.isTranslation(Set.of("en")));
        assertFalse(text.isTranslation(Set.of("en", "de")));
        assertTrue(text.isTranslation(Set.of("fr", "de")));
        assertTrue(text.isTranslation(Set.of("es", "de")));
        assertFalse(text.isTranslation(Set.of("es", "ru")));
    }

    @Test
    public void createTranslatableTextOldDelimiter() {
        TranslatableText text = null;
        TranslatableText nullText = new TranslatableText(TranslatableText.OLD_DELIMITER);
        assertTrue(TranslatableText.isNull(nullText));
        assertTrue(nullText.isEmpty());
        assertNull(nullText.translationMap);
        assertEquals(new TranslatableText(), nullText);
        TranslatableText defaultConstructedText = new TranslatableText();
        assertEquals(nullText, defaultConstructedText);
        assertEquals(TranslatableText.OLD_DELIMITER, nullText.getEncodedValue());

        text = new TranslatableText("new original", "en");
        TranslatableText text2 = new TranslatableText(TranslatableText.OLD_DELIMITER+"en:new original"+TranslatableText.OLD_DELIMITER+"en:new original"+TranslatableText.OLD_DELIMITER);
        // this assertion is false, because encoded value of text3 has duplicated entries
        // assertEquals(text2, text3);
        assertEquals(text.getText(), text2.getText());
        assertEquals(text, text2);
        // this will repair the encoded value
        text2.normalize();
        assertEquals(text, text2);
    }

    @Test
    public void createTranslatableText() {
        TranslatableText text = null;
        assertTrue(TranslatableText.isNull(text));

        text = new TranslatableText();
        assertTrue(TranslatableText.isNull(text));
        assertTrue(text.isEmpty());
        assertNull(text.translationMap);

        text = TranslatableText.create("", "en");
        assertFalse(TranslatableText.isNull(text));
        assertTrue(text.isEmpty());
        assertEquals("", text.toString());
        assertNotNull(text.translationMap);
        assertTrue(text.translationMap.isEmpty());
        if (TranslatableText.getStandardWriteVersion()==1) {
            assertEquals(TranslatableText.DELIMITER + "1en:", text.getEncodedValue());
            // DELIMITER.length=3 VERSION.length=1, so encoded length = 3 + 1 + 3 = 7
            assertEquals(TranslatableText.DELIMITER.getBytes(StandardCharsets.UTF_8).length + 4, text.getEncodedValue().getBytes(StandardCharsets.UTF_8).length);
        } else if (TranslatableText.getStandardWriteVersion()==2) {
            assertEquals(TranslatableText.DELIMITER + "2en: ~", text.getEncodedValue());
            // DELIMITER.length=3 VERSION.length=1, so encoded length = 3 + 1 + 3 + 1 + 1= 9
            assertEquals(TranslatableText.DELIMITER.getBytes(StandardCharsets.UTF_8).length + 6, text.getEncodedValue().getBytes(StandardCharsets.UTF_8).length);
        }

        text.setTranslation("leer", "");
        assertFalse(TranslatableText.isNull(text));
        assertTrue(text.isEmpty());
        assertNotNull(text.translationMap);
        assertTrue(text.translationMap.isEmpty());
        if (TranslatableText.getStandardWriteVersion()==1) {
            assertEquals(TranslatableText.DELIMITER + "1en:", text.getEncodedValue());
        } else if (TranslatableText.getStandardWriteVersion()==0) {
            assertEquals(TranslatableText.OLD_DELIMITER + "en:"  + TranslatableText.OLD_DELIMITER, text.getEncodedValue());
        } else {
            assertEquals(TranslatableText.DELIMITER + "2en: ~", text.getEncodedValue());
        }

        TranslatableText text2 = new TranslatableText(text.getEncodedValue());
        assertTrue(text.equalsOriginal(text2));
        assertEquals(text, text2);

        text2.setTranslation("new original", "en");
        assertFalse(text2.equalsOriginal(text));
        assertEquals("new original", text2.getText());
        if (TranslatableText.getStandardWriteVersion()==1) {
            assertEquals(TranslatableText.DELIMITER + "1en:new original", text2.getEncodedValue());
        } else if (TranslatableText.getStandardWriteVersion()==0) {
            assertEquals(TranslatableText.OLD_DELIMITER + "en:new original"  + TranslatableText.OLD_DELIMITER, text2.getEncodedValue());
        } else {
            assertEquals(TranslatableText.DELIMITER + "2en:,~new original", text2.getEncodedValue());
        }
        assertNotNull(text2.translationMap);

        text = new TranslatableText("original", "en");
        text.setTranslation("Übersetzung", "de");
        if (TranslatableText.getStandardWriteVersion()==1) {
            assertEquals(TranslatableText.DELIMITER + "1en:original" + TranslatableText.DELIMITER + "de:Übersetzung", text.getEncodedValue());
        } else if (TranslatableText.getStandardWriteVersion()==0) {
            assertEquals(TranslatableText.OLD_DELIMITER + "en:original"  + TranslatableText.OLD_DELIMITER + "de:Übersetzung" + TranslatableText.OLD_DELIMITER, text.getEncodedValue());
        } else {
            assertEquals(TranslatableText.DELIMITER + "2en:(~originalde:+~Übersetzung", text.getEncodedValue());
        }
        text2 = new TranslatableText(text.getEncodedValue());
        assertEquals(text, text2);
        text2.setTranslation("Übersetzung", "de");
        assertEquals(text, text2);
        text2.setTranslation("traduction", "fr");
        if (TranslatableText.getStandardWriteVersion()==1) {
            assertEquals(TranslatableText.DELIMITER + "1en:original" + TranslatableText.DELIMITER + "de:Übersetzung" + TranslatableText.DELIMITER + "fr:traduction", text2.getEncodedValue());
        } else if (TranslatableText.getStandardWriteVersion()==0) {
            assertEquals(TranslatableText.OLD_DELIMITER + "en:original"  + TranslatableText.OLD_DELIMITER + "de:Übersetzung" + TranslatableText.OLD_DELIMITER + "fr:traduction" + TranslatableText.OLD_DELIMITER, text2.getEncodedValue());
        } else {
            assertEquals(TranslatableText.DELIMITER + "2en:(~originalde:+~Übersetzungfr:*~traduction", text2.getEncodedValue());
        }

        TranslatableText t = new TranslatableText(text2.getEncodedValue());
        assertEquals("traduction", t.getText("fr"));

        // create with different translation sequence
        TranslatableText text3 = new TranslatableText(TranslatableText.OLD_DELIMITER + "en:original" + TranslatableText.OLD_DELIMITER + "fr:traduction" + TranslatableText.OLD_DELIMITER + "de:Übersetzung" + TranslatableText.OLD_DELIMITER);
        // this assertion is false, because encoded value of text3 has different sequence of languages
        // assertEquals(text2, text3);
        text3.normalize();
        assertEquals(text2, text3);
    }

    @Test
    public void setTranslation() {
        // Translatable text without translation
        TranslatableText text = TranslatableText.create("original", "en");
        assertNull(text.translationLookup(""));
        assertNull(text.translationLookup("xx"));
        assertEquals("original", text.getText());
        assertEquals("original", text.getText("en"));
        assertEquals("original", text.getTranslation("en"));
        assertEquals("original", text.translationLookup("en"));
        assertEquals("en", text.getOriginalLanguage());
        assertFalse(text.isEmpty());
        assertFalse(TranslatableText.isNull(text));
        assertNull(text.getTranslation("de"));
        assertEquals("", text.getTranslation(List.of()));
        assertEquals("original", text.getTranslation(List.of("de", "en")));
        assertEquals("original", text.getText("de"));

        // set new text in original language
        text.setTranslation("new text", "en");
        assertEquals("new text", text.getTranslation("en"));
        assertEquals("new text", text.getText());
        assert(text.translationMap == null || text.translationMap.isEmpty());
        assertEquals(0, text.getTranslationMap().size());
        assertEquals("new text", text.getText("de"));
        assertNull(text.getTranslation("de"));

        // set new translation
        text.setTranslation("deutsch", "de");
        assertEquals("deutsch", text.getTranslation("de"));
        assertEquals("deutsch", text.getTranslation(List.of("de", "en")));
        assertEquals("new text", text.getTranslation(List.of("en", "de")));
        assertEquals("new text", text.getTranslation(List.of("fr", "en")));

        // set translation with incorrect iso
        text = TranslatableText.create("original", "en");
        text.setTranslation("deutsch", "deutsch");
        assertNull(text.getTranslation("deutsch"));
        assertEquals("original", text.getTranslation(List.of("deutsch", "en")));

    }

    @Test
    public void testIso3Language() {
        // ISO3: deu=deutsch, eng=englisch, fra=französisch, zho=chinese, cmn=mandarin,
        // Translatable text without translation
        TranslatableText text = TranslatableText.create("original", "eng");

        assertEquals("eng", text.getOriginalLanguage());
        assertEquals("eng", text.getLanguages().getFirst());
        assertEquals("original", text.getText());
        assertEquals("original", text.getText("eng"));
        assertEquals("original", text.getTranslation("eng"));
        assertEquals("eng", text.getOriginalLanguage());

        assertFalse(text.isEmpty());
        assertFalse(TranslatableText.isNull(text));
        assertNull(text.getTranslation("deu"));
        assertEquals("original", text.getTranslation(List.of("deu", "eng")));
        assertEquals("original", text.getText("deu"));

        text.setTranslation("deutsch", "deu");
        assertEquals("eng", text.getLanguages().getFirst());
        assertEquals("deu", text.getLanguages().get(1));
        assertEquals("original", text.getText());
        assertEquals("deutsch", text.getText("deu"));

        TranslatableText text2 = new TranslatableText(text.getEncodedValue());
        assertEquals("eng", text2.getOriginalLanguage());
        assertEquals("eng", text2.getLanguages().getFirst());
        assertEquals("original", text2.getText());
        assertEquals("deutsch", text2.getText("deu"));

        String oldEncoded = TranslatableText.OLD_DELIMITER+"engoriginal"+TranslatableText.OLD_DELIMITER+"deuOriginal"+TranslatableText.OLD_DELIMITER+"fraoriginale"+TranslatableText.OLD_DELIMITER+"rusоригинал"+TranslatableText.OLD_DELIMITER;
        text = new TranslatableText(oldEncoded);
        assertEquals("original", text.getText("eng"));
        assertEquals("Original", text.getText("deu"));
        assertEquals("none", text.getText("en", "none"));
        text.normalize();
        text = new TranslatableText(text.getEncodedValue());
        assertEquals(4, text.getLanguages().size());
        assertEquals("original", text.getText("eng"));
        assertEquals("originale", text.getText("fra"));


        String encoded = TranslatableText.DELIMITER+"1engoriginal"+TranslatableText.DELIMITER+"deuOriginal"+TranslatableText.DELIMITER+"fraoriginale"+TranslatableText.DELIMITER+"rusоригинал";
        text = new TranslatableText(encoded);
        assertEquals("original", text.getText("eng"));
        assertEquals("Original", text.getText("deu"));
        assertEquals("none", text.getText("en", "none"));
    }

    @Test
    public void testIsTranslatableText() {
        assertTrue(TranslatableText.isTranslatableText(null));
        // @todo: this is changed from old behaviour:
        assertTrue(TranslatableText.isTranslatableText(""));
        // @todo: is this really a translatable text?
        assertTrue(TranslatableText.isTranslatableText(TranslatableText.OLD_DELIMITER));
        assertTrue(TranslatableText.isTranslatableText(TranslatableText.OLD_DELIMITER+TranslatableText.OLD_DELIMITER));

        String encodedValue = TranslatableText.create(null, "de").getEncodedValue();
        assertTrue(TranslatableText.isTranslatableText(encodedValue));
        assertFalse(TranslatableText.isTranslatableText("a normal string"));
        assertFalse(TranslatableText.isTranslatableText(TranslatableText.DELIMITER));
    }

    @Test
    public void translationLookup() {
        // languages: en,de,fr,ru
        String encoded = TranslatableText.OLD_DELIMITER+"en:original"+TranslatableText.OLD_DELIMITER+"de:Original"+TranslatableText.OLD_DELIMITER+"fr:originale"+TranslatableText.OLD_DELIMITER+"ru:оригинал"+TranslatableText.OLD_DELIMITER;
        TranslatableText text = new TranslatableText(encoded);
        assertEquals("original", text.getText());
        assertEquals("originale", text.translationLookup("fr"));
        // @todo: the language translations are lost
        assertEquals("оригинал", text.translationLookup("ru"));
        assertEquals(encoded, text.getEncodedValue());
    }

    @Test
    public void testOverwriteOriginalTranslation() {
        String encodedValueWithTranslation = TranslatableText.OLD_DELIMITER+"en:text-en"+TranslatableText.OLD_DELIMITER+"de:text-de"+TranslatableText.OLD_DELIMITER;
        TranslatableText text = new TranslatableText(encodedValueWithTranslation);
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("text-en", text.getText());
        assertEquals("text-de", text.getText("de"));
        text.setTranslation("new-en", "en");
        assertEquals("new-en", text.getText());
        assertEquals("text-de", text.getText("de"));
        if (TranslatableText.getStandardWriteVersion()==0) {
            assertEquals(TranslatableText.OLD_DELIMITER + "en:new-en"+ TranslatableText.OLD_DELIMITER +"de:text-de" + TranslatableText.OLD_DELIMITER, text.getEncodedValue());
        } else if (TranslatableText.getStandardWriteVersion()==1) {
            assertEquals(TranslatableText.DELIMITER + "1en:new-en"+ TranslatableText.DELIMITER +"de:text-de", text.getEncodedValue());
        } else if (TranslatableText.getStandardWriteVersion()==2) {
            assertEquals(TranslatableText.DELIMITER + "2en:&~new-ende:'~text-de", text.getEncodedValue());
        }
    }

    @Test
    public void createWithOldEncodedValue() {
        String encodedValueNull = null;
        TranslatableText text = new TranslatableText(encodedValueNull);
        assertNotNull(text);
        assertTrue(TranslatableText.isNull(text));
        assertNull(text.getEncodedValue());
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));

        text = new TranslatableText(TranslatableText.OLD_DELIMITER);
        assertEquals(TranslatableText.OLD_DELIMITER, text.getEncodedValue());
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));

        String encodedValueWithTranslation = TranslatableText.OLD_DELIMITER+"en:text-en"+TranslatableText.OLD_DELIMITER+"de:text-de"+TranslatableText.OLD_DELIMITER;
        text = new TranslatableText(encodedValueWithTranslation);
        assertFalse(text.isTranslation(Set.of("en")));
        assertTrue(text.isTranslation(Set.of("de")));
        assertEquals(encodedValueWithTranslation, text.getEncodedValue());
        assertEquals("text-en", text.getTranslation("en"));
        // test side effects of getTranslation
        assertEquals(encodedValueWithTranslation, text.getEncodedValue());
        assertEquals("text-en", text.getText());
        // test side effects of getText
        assertEquals(encodedValueWithTranslation, text.getEncodedValue());
        assertEquals("text-de", text.getText("de"));
        // test side effects of getText with language
        assertEquals(encodedValueWithTranslation, text.getEncodedValue());

        // inconsistent encodedValue (with original language duplicated as translation with different value
        String encodedValueWithDuplicateLanguage = TranslatableText.OLD_DELIMITER+"en:text-en"+TranslatableText.OLD_DELIMITER+"en:text-en"+TranslatableText.OLD_DELIMITER+"de:text-de"+TranslatableText.OLD_DELIMITER;
        text = new TranslatableText(encodedValueWithDuplicateLanguage);
        assertFalse(text.isTranslation(Set.of("en")));
        assertEquals(encodedValueWithDuplicateLanguage, text.getEncodedValue());
        assertEquals("text-en", text.getTranslation("en"));
        // test side effects of getTranslation
        assertEquals(encodedValueWithDuplicateLanguage, text.getEncodedValue());
        assertEquals("text-en", text.getText());
        // test side effects of getText
        assertEquals(encodedValueWithDuplicateLanguage, text.getEncodedValue());
        assertEquals("text-de", text.getText("de"));
        // test side effects of getText with language
        assertEquals(encodedValueWithDuplicateLanguage, text.getEncodedValue());
        // @todo: this will be false, because the encodedValue of text duplicates the original language
        //assertEquals(text, TranslatableText.create("text-en", "en").setTranslation("text-de", "de"));

        // inconsistent encodedValue (with original language duplicated as translation with different value
        String inconsistentEncodedValue = TranslatableText.OLD_DELIMITER+"en:text-en"+TranslatableText.OLD_DELIMITER+"en:text-en2"+TranslatableText.OLD_DELIMITER;
        text = new TranslatableText(inconsistentEncodedValue);
        assertEquals(inconsistentEncodedValue, text.getEncodedValue());
        assertEquals("text-en", text.getTranslation("en"));
        // test side effects of getTranslation
        assertEquals(inconsistentEncodedValue, text.getEncodedValue());
        assertEquals("text-en", text.getText());
        // test side effects of getText
        assertEquals(inconsistentEncodedValue, text.getEncodedValue());
        assertEquals("text-en", text.getText("de"));
        // test side effects of getText with language
        assertEquals(inconsistentEncodedValue, text.getEncodedValue());

        try {
            // does not end with DELIMITER
            TranslatableText invalid = new TranslatableText(TranslatableText.OLD_DELIMITER + "en:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // does not start with DELIMITER
            TranslatableText invalid = new TranslatableText("en:new original" + TranslatableText.OLD_DELIMITER);
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // wrong start delimiter
            TranslatableText invalid = new TranslatableText("<=@#!=>\nen:new original" + TranslatableText.OLD_DELIMITER);
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
    }

    @Test
    public void createWithEncodedValue() {
        String encodedValueNull = null;
        TranslatableText text = new TranslatableText(encodedValueNull);
        assertNotNull(text);
        assertTrue(TranslatableText.isNull(text));
        assertNull(text.getEncodedValue());
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));

        text = new TranslatableText(TranslatableText.DELIMITER+"1");
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));
        assertTrue(TranslatableText.isNull(text));
        assertTrue(text.isEmpty());
        assertNull(text.translationMap);
        assertEquals(new TranslatableText(), text);
        assertEquals(text, new TranslatableText());
        assertEquals(TranslatableText.DELIMITER+"1", text.getEncodedValue());

        String encodedValueSingleLanguage = TranslatableText.DELIMITER+"2en:)~example 1";
        text = new TranslatableText(encodedValueSingleLanguage);
        assertEquals("example 1", text.getText());

        String encodedValueDuplicatedLanguage = TranslatableText.DELIMITER+"2en:)~example 1en:)~example 1fr:)~example f";
        text = new TranslatableText(encodedValueDuplicatedLanguage);
        assertEquals("example 1", text.getText());
        assertTrue("fr", text.contains("fr"));

        String encodedValueWithTranslation = TranslatableText.DELIMITER+"1en:text-en"+TranslatableText.DELIMITER+"de:text-de";
        text = new TranslatableText(encodedValueWithTranslation);
        assertFalse(text.isTranslation(Set.of("en")));
        assertTrue(text.isTranslation(Set.of("de")));
        assertEquals(encodedValueWithTranslation, text.getEncodedValue());
        assertEquals("text-en", text.getTranslation("en"));
        // test side effects of getTranslation
        assertEquals(encodedValueWithTranslation, text.getEncodedValue());
        assertEquals("text-en", text.getText());
        // test side effects of getText
        assertEquals(encodedValueWithTranslation, text.getEncodedValue());
        assertEquals("text-de", text.getText("de"));
        // test side effects of getText with language
        assertEquals(encodedValueWithTranslation, text.getEncodedValue());

        // inconsistent encodedValue (with original language duplicated as translation with different value
        String encodedValueWithDuplicateLanguage = TranslatableText.DELIMITER+"1en:text-en"+TranslatableText.DELIMITER+"en:text-en"+TranslatableText.DELIMITER+"de:text-de"+TranslatableText.DELIMITER;
        text = new TranslatableText(encodedValueWithDuplicateLanguage);
        assertFalse(text.isTranslation(Set.of("en")));
        assertTrue(text.isTranslation(Set.of("de")));
        assertEquals(encodedValueWithDuplicateLanguage, text.getEncodedValue());
        assertEquals("text-en", text.getTranslation("en"));
        // test side effects of getTranslation
        assertEquals(encodedValueWithDuplicateLanguage, text.getEncodedValue());
        assertEquals("text-en", text.getText());
        // test side effects of getText
        assertEquals(encodedValueWithDuplicateLanguage, text.getEncodedValue());
        assertEquals("text-de", text.getText("de"));
        // test side effects of getText with language
        assertEquals(encodedValueWithDuplicateLanguage, text.getEncodedValue());
        // @todo: this will be false, because the encodedValue of text duplicates the original language
        //assertEquals(text, TranslatableText.create("text-en", "en").setTranslation("text-de", "de"));

        // inconsistent encodedValue (with original language duplicated as translation with different value
        String inconsistentEncodedValue = TranslatableText.OLD_DELIMITER+"en:text-en"+TranslatableText.OLD_DELIMITER+"en:text-en2"+TranslatableText.OLD_DELIMITER;
        text = new TranslatableText(inconsistentEncodedValue);
        assertEquals(inconsistentEncodedValue, text.getEncodedValue());
        assertEquals("text-en", text.getTranslation("en"));
        // test side effects of getTranslation
        assertEquals(inconsistentEncodedValue, text.getEncodedValue());
        assertEquals("text-en", text.getText());
        // test side effects of getText
        assertEquals(inconsistentEncodedValue, text.getEncodedValue());
        assertEquals("text-en", text.getText("de"));
        // test side effects of getText with language
        assertEquals(inconsistentEncodedValue, text.getEncodedValue());

        try {
            // invalid version
            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "3en:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // no start DELIMITER
            TranslatableText invalid = new TranslatableText("1en:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // wrong start delimiter
            TranslatableText invalid = new TranslatableText("\u200A1en:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
//        try {
//            // no original language
//            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "1new original");
//            assertNull(invalid);
//        } catch (RuntimeException e) {
//            assertEquals("Error: language colon missing", e.getMessage());
//        }
//        try {
//            // no original language
//            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "2new original");
//            assertNull(invalid);
//        } catch (RuntimeException e) {
//            assertEquals("Error: language colon missing", e.getMessage());
//        }
        try {
            // missing colon
            TranslatableText empty = new TranslatableText(TranslatableText.DELIMITER + "1eng");
            assertEquals("",empty.getText());
        } catch (RuntimeException e) {
            assertEquals("Error: language colon missing", e.getMessage());
        }
        try {
            // missing length
            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "2eng");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
        }
//        try {
//            // short original language
//            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "1e:new original");
//            assertNull(invalid);
//        } catch (RuntimeException e) {
//            assertEquals("Error: language colon missing", e.getMessage());
//        }
        try {
            // wrong original language
            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "2e:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
        }
//        try {
//            // wrong original language
//            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "1english:new original");
//            assertNull(invalid);
//        } catch (RuntimeException e) {
//            assertEquals("Error: language colon missing", e.getMessage());
//        }
        try {
            // wrong original language
            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "2english:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
        }
        try {
            // wrong length field
            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "2en:€~new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
        }
        try {
            // wrong length field
            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "2en:\r~new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
        }
        try {
            // wrong length field
            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "2en:12~new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
        }
        try {
            // wrong length field
            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "2en: ~new original");
            invalid.normalize();
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue("wrong exception message: " + e.getMessage(), e.getMessage().contains("Error: parsing encoded TranslatableText at 18"));
        }
    }

    @Test
    public void getEncodedValue() {
        TranslatableText text = TranslatableText.create("original", "en");
        // @todo: do we need a translation map here?
        assertNotNull(text.getTranslationMap());
        // @todo: here we have duplicated text
        String encodedValueSingle = switch (TranslatableText.getStandardWriteVersion()) {
            case 0 -> TranslatableText.OLD_DELIMITER + "en:original" + TranslatableText.OLD_DELIMITER;
            case 1 -> TranslatableText.DELIMITER + "1en:original";
            case 2 -> TranslatableText.DELIMITER + "2en:(~original";
            default -> "";
        };
        assertEquals(encodedValueSingle, text.getEncodedValue());
        assertNotNull(text.getTranslationMap());
        assertEquals(0, text.getTranslationMap().size());

        text = new TranslatableText(encodedValueSingle);
        assertTrue(text.getTranslationMap().isEmpty());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.setTranslation("translation", "de");

        text = new TranslatableText(encodedValueSingle);
        assertTrue(text.getTranslationMap().isEmpty());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        assertEquals(encodedValueSingle, text.getEncodedValue());
        text.setTranslation("translation", "de");
        String encodedWithTranslation = encodedValueSingle + switch (TranslatableText.getStandardWriteVersion()) {
            case 0 -> "de:translation"+TranslatableText.OLD_DELIMITER;
            case 1 -> TranslatableText.DELIMITER+"de:translation";
            case 2 -> "de:+~translation";
            default -> "";
        };
        assertEquals(encodedWithTranslation, text.getEncodedValue());

        String currentEncodedValueSingleDelimiterAtEnd = encodedValueSingle + (TranslatableText.getStandardWriteVersion()<1 ? TranslatableText.OLD_DELIMITER : "");
        text = new TranslatableText(currentEncodedValueSingleDelimiterAtEnd);
        assertTrue(text.getTranslationMap().isEmpty());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.normalize();
        assertEquals(encodedValueSingle, text.getEncodedValue());

        String oldEncodedValueSingle = TranslatableText.OLD_DELIMITER+"en:original"+TranslatableText.OLD_DELIMITER;
        text = new TranslatableText(oldEncodedValueSingle);
        assertTrue(text.getTranslationMap().isEmpty());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.setTranslation("translation", "de");

        String newEncodedValueDuplicate = TranslatableText.DELIMITER+"1en:original"+TranslatableText.DELIMITER+"en:original";
        text = new TranslatableText(newEncodedValueDuplicate);
        assertTrue(text.getTranslationMap().isEmpty());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.setTranslation("translation", "de");
        assertEquals(encodedWithTranslation, text.getEncodedValue());

        String oldEncodedValueDuplicate = TranslatableText.OLD_DELIMITER+"en:original"+TranslatableText.OLD_DELIMITER+"en:original"+TranslatableText.OLD_DELIMITER;
        text = new TranslatableText(oldEncodedValueDuplicate);
        assertTrue(text.getTranslationMap().isEmpty());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.setTranslation("translation", "de");

        String encodedValueWithTranslation = text.getEncodedValue();
        TranslatableText text2 = new TranslatableText(encodedValueWithTranslation);
        assertEquals(encodedValueWithTranslation, text2.getEncodedValue());
        assertEquals("original", text2.getTranslation("en"));
        assertEquals(encodedValueWithTranslation, text2.getEncodedValue());
        assertEquals("original", text2.getText());
        assertEquals(encodedValueWithTranslation, text2.getEncodedValue());
        assertEquals("translation", text2.getText("de"));
        assertEquals(encodedValueWithTranslation, text2.getEncodedValue());

    }

    @Test
    public void testEquals() {
        TranslatableText text1 = TranslatableText.create("text", "en");
        TranslatableText text2 = TranslatableText.create("text", "en");
        assertEquals(text1, text2);
        Set<TranslatableText> textMap = new HashSet<>();
        textMap.add(text1);
        textMap.add(text2);
        assertEquals(1, textMap.size());

        text2.setTranslation("deutsch", "de");
        assertTrue(text1.equalsOriginal(text2));
        assertTrue(text2.equalsOriginal(text1));
        assertNotEquals(text1, text2);

        textMap = new HashSet<>();
        textMap.add(text1);
        textMap.add(text2);
        assertEquals(2, textMap.size());

        text1.setTranslation("Text", "de");
        assertTrue(text1.equalsOriginal(text2));
        textMap = new HashSet<>();
        textMap.add(text1);
        textMap.add(text2);
        assertNotEquals(text1, text2);
        assertEquals(2, textMap.size());
        // @todo: alternative (if we only have original value for equality)
//		assertEquals(text1, text2);
//        assertEquals(1, textMap.size());

    }

    public void measureEncodeDecode(String prefix, Function<TranslatableText, String> encode, String... languages) throws IOException {
        int loops = 10;
        int size = 1_000_000;
        long fullTimeEncode = 0;
        long fullTimeGet = 0;
        long fullTimeDecode = 0;
        long minEncode = Long.MAX_VALUE;
        long maxGet = 0;
        long minGet = Long.MAX_VALUE;
        long maxEncode = 0;
        long minDecode = Long.MAX_VALUE;
        long maxDecode = 0;
        System.out.println(prefix + " text encoding:...");
        for (int n = 0; n < loops; n++) {
            long startTime = System.currentTimeMillis();
            List<String> encodedList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TranslatableText translatableText = new TranslatableText("text" + i, "de");
                for (String language : languages) {
                    translatableText.setTranslation("text-" + language + i, language);
                }
                String encoded = encode.apply(translatableText);
                encodedList.add(encoded);
            }
            long encodeDuration = System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            for (int i = 0; i < size; i++) {
                String value = encodedList.get(i);
                TranslatableText translatableText = new TranslatableText(value);
                for (String language :  languages) {
                    assertEquals("text-" + language + i, translatableText.getText(language));
                }
                assertEquals("text" + i, translatableText.getText());
            }
            long getTranslationDuration = System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            for (int i = 0; i < size; i++) {
                String value = encodedList.get(i);
                TranslatableText translatableText = new TranslatableText(value);
                Map<String, String> map = translatableText.getTranslationMap();
                assertEquals(languages.length, map.size());
            }
            long decodeDuration = System.currentTimeMillis() - startTime;

            if (n == 0) {
                System.out.printf("  length=%,d UTF8=%,d\n", encodedList.getFirst().length(), encodedList.getFirst().getBytes(StandardCharsets.UTF_8).length);
                System.out.printf("  First encode: %,dms First decode: %,dms\n", encodeDuration, decodeDuration);
            } else {
                fullTimeEncode += encodeDuration;
                fullTimeGet += getTranslationDuration;
                fullTimeDecode += decodeDuration;
                if (encodeDuration < minEncode) minEncode = encodeDuration;
                if (encodeDuration > maxEncode) maxEncode = encodeDuration;
                if (getTranslationDuration < minGet) minGet = getTranslationDuration;
                if (getTranslationDuration > maxGet) maxGet = getTranslationDuration;
                if (decodeDuration < minDecode) minDecode = decodeDuration;
                if (decodeDuration > maxDecode) maxDecode = decodeDuration;
            }
        }
        System.out.printf("  Time  encode: %,dms  min=%,dms max=%,dms\n", fullTimeEncode / (loops - 1), minEncode, maxEncode);
        System.out.printf("  Time  get   : %,dms  min=%,dms max=%,dms\n", fullTimeGet / (loops - 1), minGet, maxGet);
        System.out.printf("  Time  decode: %,dms  min=%,dms max=%,dms\n", fullTimeDecode / (loops - 1), minDecode, maxDecode);
    }

    @Test
    public void compareImplementations() throws IOException {
        int loops = 10;
        int size = 1_000_000;
        measureEncodeDecode("old", t -> t.createEncodedValue(0), "en", "fr");
        measureEncodeDecode("new", TranslatableText::getEncodedValue, "en", "fr");
        measureEncodeDecode("len", t -> t.createEncodedValue(2), "en", "fr");
        measureEncodeDecode("iso3", TranslatableText::getEncodedValue, "eng", "fra");

        long fullTimeEncode = 0;
        long fullTimeDecode = 0;
        long minEncode = 100_0000_000;
        long maxEncode = 0;
        long minDecode = 100_0000_000;
        long maxDecode = 0;
        System.out.println("Binary:...");
        for (int n = 0; n < loops; n++) {
            long startTime = System.currentTimeMillis();
            List<byte[]> encodedList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                BinaryTranslatedText translatableText = new BinaryTranslatedText("text" + i, "de").setTranslation("text-en" + i, "en").setTranslation("text-fr" + i, "fr");
                byte[] encoded = translatableText.getEncodedValue();
                encodedList.add(encoded);
            }
            long encodeDuration = System.currentTimeMillis() - startTime;
            startTime = System.currentTimeMillis();
            for (int i = 0; i < size; i++) {
                byte[] value = encodedList.get(i);
                BinaryTranslatedText translatableText = new BinaryTranslatedText(value);
                if (!("text-en" + i).equals(translatableText.getText("en"))) {
                    System.out.println("Error:" + translatableText.getText("en"));
                }
                if (!("text" + i).equals(translatableText.getText())) {
                    System.out.println("Error:" + translatableText.getText());
                }
            }
            long decodeDuration = System.currentTimeMillis() - startTime;

            if (n == 0) {
                System.out.println("  First binary encode: " + encodeDuration + "  First binary decode: " + decodeDuration);
            } else {
                fullTimeEncode += encodeDuration;
                fullTimeDecode += decodeDuration;
                if (encodeDuration < minEncode) minEncode = encodeDuration;
                if (encodeDuration > maxEncode) maxEncode = encodeDuration;
                if (decodeDuration < minDecode) minDecode = decodeDuration;
                if (decodeDuration > maxDecode) maxDecode = decodeDuration;
            }
        }
        System.out.println("  Time  binary encode: " + fullTimeEncode/(loops-1) + " min=" + minEncode + " max=" + maxEncode);
        System.out.println("  Time  binary decode: " + fullTimeDecode/(loops-1) + " min=" + minDecode + " max=" + maxDecode);
    }

}
