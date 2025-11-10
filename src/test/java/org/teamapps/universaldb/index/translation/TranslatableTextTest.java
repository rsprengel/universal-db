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

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.*;

public class TranslatableTextTest {

    @Test
    public void invalidConstruction() {
        try {
            TranslatableTextIf invalid = TranslatableTextIf.create("text-en", null);
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Error: no language for translatable text"));
        }
        try {
            TranslatableTextIf invalid = TranslatableTextIf.create("text-en", "english");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Error: language is not an iso code"));
        }
        try {
            // does not end with DELIMITER
            TranslatableTextIf invalid = TranslatableTextIf.create(TranslatableTextIf.OLD_DELIMITER + "en:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // does not start with DELIMITER
            TranslatableTextIf invalid = TranslatableTextIf.create("en:new original" + TranslatableTextIf.OLD_DELIMITER);
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // wrong start delimiter
            TranslatableTextIf invalid = TranslatableTextIf.create("<=@#!=>\nen:new original" + TranslatableTextIf.OLD_DELIMITER);
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }

        try {
            // invalid version
            TranslatableTextIf invalid = TranslatableTextIf.create(TranslatableTextIf.DELIMITER + "3en:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // no start DELIMITER
            TranslatableTextIf invalid = TranslatableTextIf.create("1en:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // wrong start delimiter
            TranslatableTextIf invalid = TranslatableTextIf.create("\u200A1en:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // missing colon
            TranslatableTextIf empty = TranslatableTextIf.create(TranslatableTextIf.DELIMITER + "1eng");
            assertEquals("",empty.getText());
        } catch (RuntimeException e) {
            assertEquals("Error: language colon missing", e.getMessage());
        }
        if (TranslatableTextIf.getVersion() == 2) {
            try {
                // missing length
                TranslatableTextIf invalid = TranslatableTextIf.create(TranslatableTextIf.DELIMITER + "2eng");
                assertNull(invalid);
            } catch (RuntimeException e) {
                assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
            }
            try {
                // wrong original language
                TranslatableTextIf invalid = TranslatableTextIf.create(TranslatableTextIf.DELIMITER + "2e:new original");
                assertNull(invalid);
            } catch (RuntimeException e) {
                assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
            }
            try {
                // wrong original language
                TranslatableTextIf invalid = TranslatableTextIf.create(TranslatableTextIf.DELIMITER + "2english:new original");
                assertNull(invalid);
            } catch (RuntimeException e) {
                assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
            }
            try {
                // wrong length field
                TranslatableTextIf invalid = TranslatableTextIf.create(TranslatableTextIf.DELIMITER + "2en:€~new original");
                assertNull(invalid);
            } catch (RuntimeException e) {
                assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
            }
            try {
                // wrong length field
                TranslatableTextIf invalid = TranslatableTextIf.create(TranslatableTextIf.DELIMITER + "2en:\r~new original");
                assertNull(invalid);
            } catch (RuntimeException e) {
                assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
            }
            try {
                // wrong length field
                TranslatableTextIf invalid = TranslatableTextIf.create(TranslatableTextIf.DELIMITER + "2en:12~new original");
                assertNull(invalid);
            } catch (RuntimeException e) {
                assertEquals("Error: parsing encoded TranslatableText at 13", e.getMessage());
            }
            try {
                // wrong length field
                TranslatableTextIf invalid = TranslatableTextIf.create(TranslatableTextIf.DELIMITER + "2en: ~new original");
                invalid.normalize();
                assertNull(invalid);
            } catch (RuntimeException e) {
                assertTrue("wrong exception message: " + e.getMessage(), e.getMessage().contains("Error: parsing encoded TranslatableText at 18"));
            }
        }
    }

    @Test
    public void getTranslation() {
        TranslatableTextIf text = TranslatableTextIf.create("text-en", "en");
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
        text = TranslatableTextIf.create(encodedValue);
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
        TranslatableTextIf text = null;
        TranslatableTextIf nullText = TranslatableTextIf.create(TranslatableTextIf.OLD_DELIMITER);
        assertTrue(TranslatableTextIf.isNull(nullText));
        assertTrue(nullText.isEmpty());
        assertEquals(TranslatableTextIf.create(), nullText);
        TranslatableTextIf defaultConstructedText = TranslatableTextIf.create();
        assertEquals(nullText, defaultConstructedText);
        assertEquals(TranslatableTextIf.OLD_DELIMITER, nullText.getEncodedValue());

        text = TranslatableTextIf.create("new original", "en");
        TranslatableTextIf text2 = TranslatableTextIf.create(TranslatableTextIf.OLD_DELIMITER+"en:new original"+ TranslatableTextIf.OLD_DELIMITER+"en:new original"+ TranslatableTextIf.OLD_DELIMITER);
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
        TranslatableTextIf text = null;
        assertTrue(TranslatableTextIf.isNull(text));

        text = TranslatableTextIf.create();
        assertTrue(TranslatableTextIf.isNull(text));
        assertTrue(text.isEmpty());

        text = TranslatableTextIf.create("", "en");
        assertFalse(TranslatableTextIf.isNull(text));
        assertTrue(text.isEmpty());
        assertEquals("", text.toString());
        assertEquals(1, text.getTranslationMap().size());
        if (TranslatableTextIf.getVersion()==1) {
            assertEquals(TranslatableTextIf.DELIMITER + "1en:", text.getEncodedValue());
            // DELIMITER.length=3 VERSION.length=1, so encoded length = 3 + 1 + 3 = 7
            assertEquals(TranslatableTextIf.DELIMITER.getBytes(StandardCharsets.UTF_8).length + 4, text.getEncodedValue().getBytes(StandardCharsets.UTF_8).length);
        } else if (TranslatableTextIf.getVersion()==2) {
            assertEquals(TranslatableTextIf.DELIMITER + "2en: ~", text.getEncodedValue());
            // DELIMITER.length=3 VERSION.length=1, so encoded length = 3 + 1 + 3 + 1 + 1= 9
            assertEquals(TranslatableTextIf.DELIMITER.getBytes(StandardCharsets.UTF_8).length + 6, text.getEncodedValue().getBytes(StandardCharsets.UTF_8).length);
        }

        text.setTranslation("leer", "");
        assertFalse(TranslatableTextIf.isNull(text));
        assertTrue(text.isEmpty());
        assertEquals(1, text.getTranslationMap().size());
        if (TranslatableTextIf.getVersion()==1) {
            assertEquals(TranslatableTextIf.DELIMITER + "1en:", text.getEncodedValue());
        } else if (TranslatableTextIf.getVersion()==0) {
            assertEquals(TranslatableTextIf.OLD_DELIMITER + "en:"  + TranslatableTextIf.OLD_DELIMITER, text.getEncodedValue());
        } else {
            assertEquals(TranslatableTextIf.DELIMITER + "2en: ~", text.getEncodedValue());
        }

        TranslatableTextIf text2 = TranslatableTextIf.create(text.getEncodedValue());
        assertTrue(text.equalsOriginal(text2));
        assertEquals(text, text2);

        text2.setTranslation("new original", "en");
        assertFalse(text2.equalsOriginal(text));
        assertEquals("new original", text2.getText());
        if (TranslatableTextIf.getVersion()==1) {
            assertEquals(TranslatableTextIf.DELIMITER + "1en:new original", text2.getEncodedValue());
        } else if (TranslatableTextIf.getVersion()==0) {
            assertEquals(TranslatableTextIf.OLD_DELIMITER + "en:new original"  + TranslatableTextIf.OLD_DELIMITER, text2.getEncodedValue());
        } else {
            assertEquals(TranslatableTextIf.DELIMITER + "2en:,~new original", text2.getEncodedValue());
        }
        assertEquals(1, text2.getTranslationMap().size());

        text = TranslatableTextIf.create("original", "en");
        text.setTranslation("Übersetzung", "de");
        if (TranslatableTextIf.getVersion()==1) {
            assertEquals(TranslatableTextIf.DELIMITER + "1en:original" + TranslatableTextIf.DELIMITER + "de:Übersetzung", text.getEncodedValue());
        } else if (TranslatableTextIf.getVersion()==0) {
            assertEquals(TranslatableTextIf.OLD_DELIMITER + "en:original"  + TranslatableTextIf.OLD_DELIMITER + "de:Übersetzung" + TranslatableTextIf.OLD_DELIMITER, text.getEncodedValue());
        } else {
            assertEquals(TranslatableTextIf.DELIMITER + "2en:(~originalde:+~Übersetzung", text.getEncodedValue());
        }
        text2 = TranslatableTextIf.create(text.getEncodedValue());
        assertEquals(text, text2);
        text2.setTranslation("Übersetzung", "de");
        assertEquals(text, text2);
        text2.setTranslation("traduction", "fr");
        if (TranslatableTextIf.getVersion()==1) {
            assertEquals(TranslatableTextIf.DELIMITER + "1en:original" + TranslatableTextIf.DELIMITER + "de:Übersetzung" + TranslatableTextIf.DELIMITER + "fr:traduction", text2.getEncodedValue());
        } else if (TranslatableTextIf.getVersion()==0) {
            assertEquals(TranslatableTextIf.OLD_DELIMITER + "en:original"  + TranslatableTextIf.OLD_DELIMITER + "de:Übersetzung" + TranslatableTextIf.OLD_DELIMITER + "fr:traduction" + TranslatableTextIf.OLD_DELIMITER, text2.getEncodedValue());
        } else {
            assertEquals(TranslatableTextIf.DELIMITER + "2en:(~originalde:+~Übersetzungfr:*~traduction", text2.getEncodedValue());
        }

        TranslatableTextIf t = TranslatableTextIf.create(text2.getEncodedValue());
        assertEquals("traduction", t.getText("fr"));

        // create with different translation sequence
        TranslatableTextIf text3 = TranslatableTextIf.create(TranslatableTextIf.OLD_DELIMITER + "en:original" + TranslatableTextIf.OLD_DELIMITER + "fr:traduction" + TranslatableTextIf.OLD_DELIMITER + "de:Übersetzung" + TranslatableTextIf.OLD_DELIMITER);
        // this assertion is false, because encoded value of text3 has different sequence of languages
        // assertEquals(text2, text3);
        text3.normalize();
        assertEquals(text2, text3);
    }

    @Test
    public void setTranslation() {
        // Translatable text without translation
        TranslatableTextIf text = TranslatableTextIf.create("original", "en");
        assertNull(text.translationLookup(""));
        assertNull(text.translationLookup("xx"));
        assertEquals("original", text.getText());
        assertEquals("original", text.getText("en"));
        assertEquals("original", text.getTranslation("en"));
        assertEquals("original", text.translationLookup("en"));
        assertEquals("en", text.getOriginalLanguage());
        assertFalse(text.isEmpty());
        assertFalse(TranslatableTextIf.isNull(text));
        assertNull(text.getTranslation("de"));
        assertEquals("", text.getTranslation(List.of()));
        assertEquals("original", text.getTranslation(List.of("de", "en")));
        assertEquals("original", text.getText("de"));

        // set new text in original language
        text.setTranslation("new text", "en");
        assertEquals("new text", text.getTranslation("en"));
        assertEquals("new text", text.getText());
        assertEquals(1, text.getTranslationMap().size());
        assertEquals("new text", text.getText("de"));
        assertNull(text.getTranslation("de"));

        // set new translation
        text.setTranslation("deutsch", "de");
        assertEquals("deutsch", text.getTranslation("de"));
        assertEquals("deutsch", text.getTranslation(List.of("de", "en")));
        assertEquals("new text", text.getTranslation(List.of("en", "de")));
        assertEquals("new text", text.getTranslation(List.of("fr", "en")));

        // set translation with incorrect iso
        text = TranslatableTextIf.create("original", "en");
        text.setTranslation("deutsch", "deutsch");
        assertNull(text.getTranslation("deutsch"));
        assertEquals("original", text.getTranslation(List.of("deutsch", "en")));

    }

    @Test
    public void testIso3Language() {
        // ISO3: deu=deutsch, eng=englisch, fra=französisch, zho=chinese, cmn=mandarin,
        // Translatable text without translation
        TranslatableTextIf text = TranslatableTextIf.create("original", "eng");

        assertEquals("eng", text.getOriginalLanguage());
        assertEquals("eng", text.getLanguages().getFirst());
        assertEquals("original", text.getText());
        assertEquals("original", text.getText("eng"));
        assertEquals("original", text.getTranslation("eng"));
        assertEquals("eng", text.getOriginalLanguage());

        assertFalse(text.isEmpty());
        assertFalse(TranslatableTextIf.isNull(text));
        assertNull(text.getTranslation("deu"));
        assertEquals("original", text.getTranslation(List.of("deu", "eng")));
        assertEquals("original", text.getText("deu"));

        text.setTranslation("deutsch", "deu");
        assertEquals("eng", text.getLanguages().getFirst());
        assertEquals("deu", text.getLanguages().get(1));
        assertEquals("original", text.getText());
        assertEquals("deutsch", text.getText("deu"));

        TranslatableTextIf text2 = TranslatableTextIf.create(text.getEncodedValue());
        assertEquals("eng", text2.getOriginalLanguage());
        assertEquals("eng", text2.getLanguages().getFirst());
        assertEquals("original", text2.getText());
        assertEquals("deutsch", text2.getText("deu"));

        String oldEncoded = TranslatableTextIf.OLD_DELIMITER+"engoriginal"+ TranslatableTextIf.OLD_DELIMITER+"deuOriginal"+ TranslatableTextIf.OLD_DELIMITER+"fraoriginale"+ TranslatableTextIf.OLD_DELIMITER+"rusоригинал"+ TranslatableTextIf.OLD_DELIMITER;
        text = TranslatableTextIf.create(oldEncoded);
        assertEquals("original", text.getText("eng"));
        assertEquals("Original", text.getText("deu"));
        assertEquals("none", text.getText("en", "none"));
        text.normalize();
        text = TranslatableTextIf.create(text.getEncodedValue());
        assertEquals(4, text.getLanguages().size());
        assertEquals("original", text.getText("eng"));
        assertEquals("originale", text.getText("fra"));


        String encoded = TranslatableTextIf.DELIMITER+"1engoriginal"+ TranslatableTextIf.DELIMITER+"deuOriginal"+ TranslatableTextIf.DELIMITER+"fraoriginale"+ TranslatableTextIf.DELIMITER+"rusоригинал";
        text = TranslatableTextIf.create(encoded);
        assertEquals("original", text.getText("eng"));
        assertEquals("Original", text.getText("deu"));
        assertEquals("none", text.getText("en", "none"));
    }

    @Test
    public void testIsTranslatableText() {
        assertTrue(TranslatableTextIf.isTranslatableText(null));
        // @todo: this is changed from old behaviour:
        assertTrue(TranslatableTextIf.isTranslatableText(""));
        // @todo: is this really a translatable text?
        assertTrue(TranslatableTextIf.isTranslatableText(TranslatableTextIf.OLD_DELIMITER));
        assertTrue(TranslatableTextIf.isTranslatableText(TranslatableTextIf.OLD_DELIMITER+ TranslatableTextIf.OLD_DELIMITER));

        String encodedValue = TranslatableTextIf.create(null, "de").getEncodedValue();
        assertTrue(TranslatableTextIf.isTranslatableText(encodedValue));
        assertFalse(TranslatableTextIf.isTranslatableText("a normal string"));
        assertFalse(TranslatableTextIf.isTranslatableText(TranslatableTextIf.DELIMITER));
    }

    @Test
    public void translationLookup() {
        // languages: en,de,fr,ru
        String encoded = TranslatableTextIf.OLD_DELIMITER+"en:original"+ TranslatableTextIf.OLD_DELIMITER+"de:Original"+ TranslatableTextIf.OLD_DELIMITER+"fr:originale"+ TranslatableTextIf.OLD_DELIMITER+"ru:оригинал"+ TranslatableTextIf.OLD_DELIMITER;
        TranslatableTextIf text = TranslatableTextIf.create(encoded);
        assertEquals("original", text.getText());
        assertEquals("originale", text.translationLookup("fr"));
        // @todo: the language translations are lost
        assertEquals("оригинал", text.translationLookup("ru"));
        assertEquals(encoded, text.getEncodedValue());
    }

    @Test
    public void testOverwriteOriginalTranslation() {
        String encodedValueWithTranslation = TranslatableTextIf.OLD_DELIMITER+"en:text-en"+ TranslatableTextIf.OLD_DELIMITER+"de:text-de"+ TranslatableTextIf.OLD_DELIMITER;
        TranslatableTextIf text = TranslatableTextIf.create(encodedValueWithTranslation);
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("text-en", text.getText());
        assertEquals("text-de", text.getText("de"));
        text.setTranslation("new-en", "en");
        assertEquals("new-en", text.getText());
        assertEquals("text-de", text.getText("de"));
        if (TranslatableTextIf.getVersion()==0) {
            assertEquals(TranslatableTextIf.OLD_DELIMITER + "en:new-en"+ TranslatableTextIf.OLD_DELIMITER +"de:text-de" + TranslatableTextIf.OLD_DELIMITER, text.getEncodedValue());
        } else if (TranslatableTextIf.getVersion()==1) {
            assertEquals(TranslatableTextIf.DELIMITER + "1en:new-en"+ TranslatableTextIf.DELIMITER +"de:text-de", text.getEncodedValue());
        } else if (TranslatableTextIf.getVersion()==2) {
            assertEquals(TranslatableTextIf.DELIMITER + "2en:&~new-ende:'~text-de", text.getEncodedValue());
        }
    }

    @Test
    public void createWithOldEncodedValue() {
        String encodedValueNull = null;
        TranslatableTextIf text = TranslatableTextIf.create(encodedValueNull);
        assertNotNull(text);
        assertTrue(TranslatableTextIf.isNull(text));
        assertNull(text.getEncodedValue());
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));

        text = TranslatableTextIf.create(TranslatableTextIf.OLD_DELIMITER);
        assertEquals(TranslatableTextIf.OLD_DELIMITER, text.getEncodedValue());
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));

        String encodedValueWithTranslation = TranslatableTextIf.OLD_DELIMITER+"en:text-en"+ TranslatableTextIf.OLD_DELIMITER+"de:text-de"+ TranslatableTextIf.OLD_DELIMITER;
        text = TranslatableTextIf.create(encodedValueWithTranslation);
        assertFalse(text.isTranslation(Set.of("en")));
        assertTrue(text.isTranslation(Set.of("de")));
        if (TranslatableTextIf.getVersion()==0) {
            encodedValueWithTranslation += "en:text-en"+ TranslatableTextIf.OLD_DELIMITER;
        }
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
        String encodedValueWithDuplicateLanguage = TranslatableTextIf.OLD_DELIMITER+"en:text-en"+ TranslatableTextIf.OLD_DELIMITER+"en:text-en"+ TranslatableTextIf.OLD_DELIMITER+"de:text-de"+ TranslatableTextIf.OLD_DELIMITER;
        text = TranslatableTextIf.create(encodedValueWithDuplicateLanguage);
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
        //assertEquals(text, TranslatableTextIf.create("text-en", "en").setTranslation("text-de", "de"));

        // inconsistent encodedValue (with original language duplicated as translation with different value
        String inconsistentEncodedValue = TranslatableTextIf.OLD_DELIMITER+"en:text-en"+ TranslatableTextIf.OLD_DELIMITER+"en:text-en2"+ TranslatableTextIf.OLD_DELIMITER;
        text = TranslatableTextIf.create(inconsistentEncodedValue);
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

    }

    @Test
    public void createWithEncodedValue() {
        String encodedValueNull = null;
        TranslatableTextIf text = TranslatableTextIf.create(encodedValueNull);
        assertNotNull(text);
        assertTrue(TranslatableTextIf.isNull(text));
        assertNull(text.getEncodedValue());
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));

        text = TranslatableTextIf.create(TranslatableTextIf.DELIMITER+"1");
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));
        assertTrue(TranslatableTextIf.isNull(text));
        assertTrue(text.isEmpty());
        assertEquals(TranslatableTextIf.create(), text);
        assertEquals(text, TranslatableTextIf.create());
        assertEquals(TranslatableTextIf.DELIMITER+"1", text.getEncodedValue());

        if (TranslatableTextIf.getVersion() == 2) {
            String encodedValueSingleLanguage = TranslatableTextIf.DELIMITER + "2en:)~example 1";
            text = TranslatableTextIf.create(encodedValueSingleLanguage);
            assertEquals("example 1", text.getText());

            String encodedValueDuplicatedLanguage = TranslatableTextIf.DELIMITER+"2en:)~example 1en:)~example 1fr:)~example f";
            text = TranslatableTextIf.create(encodedValueDuplicatedLanguage);
            assertEquals("example 1", text.getText());
            assertTrue("fr", text.contains("fr"));
        }

        if (TranslatableTextIf.getVersion() > 0) {
            String encodedValueWithTranslation = TranslatableTextIf.DELIMITER + "1en:text-en" + TranslatableTextIf.DELIMITER + "de:text-de";
            text = TranslatableTextIf.create(encodedValueWithTranslation);
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
            String encodedValueWithDuplicateLanguage = TranslatableTextIf.DELIMITER+"1en:text-en"+ TranslatableTextIf.DELIMITER+"en:text-en"+ TranslatableTextIf.DELIMITER+"de:text-de"+ TranslatableTextIf.DELIMITER;
            text = TranslatableTextIf.create(encodedValueWithDuplicateLanguage);
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
            //assertEquals(text, TranslatableTextIf.create("text-en", "en").setTranslation("text-de", "de"));
        }

        // inconsistent encodedValue (with original language duplicated as translation with different value
        String inconsistentEncodedValue = TranslatableTextIf.OLD_DELIMITER+"en:text-en"+ TranslatableTextIf.OLD_DELIMITER+"en:text-en2"+ TranslatableTextIf.OLD_DELIMITER;
        text = TranslatableTextIf.create(inconsistentEncodedValue);
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
    }

    @Test
    public void getEncodedValue() {
        TranslatableTextIf text = TranslatableTextIf.create("original", "en");
        // @todo: do we need a translation map here?
        assertNotNull(text.getTranslationMap());
        // @todo: here we have duplicated text
        String encodedValueSingle = switch (TranslatableTextIf.getVersion.get()) {
            case 0 -> TranslatableTextIf.OLD_DELIMITER + "en:original" + TranslatableTextIf.OLD_DELIMITER;
            case 1 -> TranslatableTextIf.DELIMITER + "1en:original";
            case 2 -> TranslatableTextIf.DELIMITER + "2en:(~original";
            default -> "";
        };
        assertEquals(encodedValueSingle, text.getEncodedValue());
        assertNotNull(text.getTranslationMap());
        assertEquals(1, text.getTranslationMap().size());

        text = TranslatableTextIf.create(encodedValueSingle);
        assertEquals(1, text.getTranslationMap().size());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.setTranslation("translation", "de");

        text = TranslatableTextIf.create(encodedValueSingle);
        assertEquals(1, text.getTranslationMap().size());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        assertEquals(encodedValueSingle, text.getEncodedValue());
        text.setTranslation("translation", "de");
        String encodedWithTranslation = encodedValueSingle + switch (TranslatableTextIf.getVersion.get()) {
            case 0 -> "de:translation"+ TranslatableTextIf.OLD_DELIMITER;
            case 1 -> TranslatableTextIf.DELIMITER+"de:translation";
            case 2 -> "de:+~translation";
            default -> "";
        };
        assertEquals(encodedWithTranslation, text.getEncodedValue());

        String currentEncodedValueSingleDelimiterAtEnd = encodedValueSingle + (TranslatableTextIf.getVersion()<1 ? TranslatableTextIf.OLD_DELIMITER : "");
        text = TranslatableTextIf.create(currentEncodedValueSingleDelimiterAtEnd);
        assertEquals(1, text.getTranslationMap().size());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.normalize();
        assertEquals(encodedValueSingle, text.getEncodedValue());

        String oldEncodedValueSingle = TranslatableTextIf.OLD_DELIMITER+"en:original"+ TranslatableTextIf.OLD_DELIMITER;
        text = TranslatableTextIf.create(oldEncodedValueSingle);
        assertEquals(1, text.getTranslationMap().size());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.setTranslation("translation", "de");

        String newEncodedValueDuplicate = TranslatableTextIf.DELIMITER+"1en:original"+ TranslatableTextIf.DELIMITER+"en:original";
        text = TranslatableTextIf.create(newEncodedValueDuplicate);
        assertEquals(1, text.getTranslationMap().size());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.setTranslation("translation", "de");
        assertEquals(encodedWithTranslation, text.getEncodedValue());

        String oldEncodedValueDuplicate = TranslatableTextIf.OLD_DELIMITER+"en:original"+ TranslatableTextIf.OLD_DELIMITER+"en:original"+ TranslatableTextIf.OLD_DELIMITER;
        text = TranslatableTextIf.create(oldEncodedValueDuplicate);
        assertEquals(1, text.getTranslationMap().size());
        assertEquals("en", text.getOriginalLanguage());
        assertEquals("original", text.getText());
        text.setTranslation("translation", "de");

        String encodedValueWithTranslation = text.getEncodedValue();
        TranslatableTextIf text2 = TranslatableTextIf.create(encodedValueWithTranslation);
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
        TranslatableTextIf text1 = TranslatableTextIf.create("text", "en");
        TranslatableTextIf text2 = TranslatableTextIf.create("text", "en");
        assertEquals(text1, text2);
        Set<TranslatableTextIf> textMap = new HashSet<>();
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

    public void measureEncodeDecode(String prefix, String... languages) throws IOException {
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
                TranslatableTextIf translatableText = TranslatableTextIf.create("text" + i, "de");
                for (String language : languages) {
                    translatableText.setTranslation("text-" + language + i, language);
                }
                String encoded = translatableText.getEncodedValue();
                encodedList.add(encoded);
            }
            long encodeDuration = System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            for (int i = 0; i < size; i++) {
                String value = encodedList.get(i);
                TranslatableTextIf translatableText = TranslatableTextIf.create(value);
                for (String language :  languages) {
                    assertEquals("text-" + language + i, translatableText.getText(language));
                }
                assertEquals("text" + i, translatableText.getText());
            }
            long getTranslationDuration = System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            for (int i = 0; i < size; i++) {
                String value = encodedList.get(i);
                TranslatableTextIf translatableText = TranslatableTextIf.create(value);
                Map<String, String> map = translatableText.getTranslationMap();
                assertEquals(languages.length+1, map.size());
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
        measureEncodeDecode("iso2", "en", "fr");
        measureEncodeDecode("iso3", "eng", "fra");

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
