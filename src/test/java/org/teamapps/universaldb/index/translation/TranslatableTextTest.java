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
import org.teamapps.universaldb.index.log.DefaultLogIndex;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class TranslatableTextTest {

    @Test
    public void getTranslation() {
        TranslatableText text = new TranslatableText("text-en", "en");
        text.setTranslation("text-de", "de");
        text.setTranslation("text-fr", "fr");

        assertEquals("text-en", text.getText("en"));
        assertEquals("text-en", text.getText("xx"));
        assertEquals("text-de", text.getText("de"));
        assertEquals("text-fr", text.getText("fr"));

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
        assertNotNull(text.translationMap);
        assertTrue(text.translationMap.isEmpty());
        assertEquals("\n<=@#!=>\nen:\n<=@#!=>\n", text.getEncodedValue());

        text.setTranslation("leer", "");
        assertFalse(TranslatableText.isNull(text));
        assertTrue(text.isEmpty());
        assertNotNull(text.translationMap);
        assertTrue(text.translationMap.isEmpty());
        assertEquals("\n<=@#!=>\nen:\n<=@#!=>\n", text.getEncodedValue());

        TranslatableText text2 = new TranslatableText(text.getEncodedValue());
        assertTrue(text.equalsOriginal(text2));
        assertEquals(text, text2);

        text2.setTranslation("new original", "en");
        assertFalse(text2.equalsOriginal(text));
        assertEquals("new original", text2.getText());
        assertEquals("\n<=@#!=>\nen:new original\n<=@#!=>\n", text2.getEncodedValue());
        assertNull(text2.translationMap);

        TranslatableText nullText = new TranslatableText("\n<=@#!=>\n");
        assertTrue(TranslatableText.isNull(nullText));
        assertTrue(nullText.isEmpty());
        assertNull(nullText.translationMap);
        assertEquals(new TranslatableText(), nullText);
        TranslatableText defaultConstructedText = new TranslatableText();
        assertEquals(nullText, defaultConstructedText);
        assertEquals("\n<=@#!=>\n", nullText.getEncodedValue());

        TranslatableText text3 = new TranslatableText("\n<=@#!=>\nen:new original\n<=@#!=>\nen:new original\n<=@#!=>\n");
        // this assertion is false, because encoded value of text3 has duplicated entries
        // assertEquals(text2, text3);
        assertEquals(text2.getText(), text3.getText());
        // this will repair the encoded value
        text3.normalize();
        assertEquals(text2, text3);

        text = new TranslatableText("original", "en", Map.of("de", "Übersetzung"));
        assertEquals("\n<=@#!=>\nen:original\n<=@#!=>\nde:Übersetzung\n<=@#!=>\n", text.getEncodedValue());
        text2 = new TranslatableText(text.getEncodedValue());
        assertEquals(text, text2);
        text2.setTranslation("Übersetzung", "de");
        assertEquals(text, text2);
        text2.setTranslation("traduction", "fr");
        assertEquals( TranslatableText.DELIMITER + "en:original" + TranslatableText.DELIMITER +"de:Übersetzung" + TranslatableText.DELIMITER + "fr:traduction" + TranslatableText.DELIMITER, text2.getEncodedValue());
        // create with different translation sequence
        text3 = new TranslatableText(TranslatableText.DELIMITER + "en:original" + TranslatableText.DELIMITER + "fr:traduction" + TranslatableText.DELIMITER + "de:Übersetzung" + TranslatableText.DELIMITER);
        // this assertion is false, because encoded value of text3 has different sequence of languages
        // assertEquals(text2, text3);
        text3.normalize();
        assertEquals(text2, text3);

        try {
            // does not end with DELIMITER
            TranslatableText invalid = new TranslatableText(TranslatableText.DELIMITER + "en:new original");
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // does not start with DELIMITER
            TranslatableText invalid = new TranslatableText("en:new original" + TranslatableText.DELIMITER);
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            // wrong start delimiter
            TranslatableText invalid = new TranslatableText("<=@#!=>\nen:new original" + TranslatableText.DELIMITER);
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation encoding"));
        }
        try {
            TranslatableText invalid = new TranslatableText("original", "english", Map.of());
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid original language"));
        }
        try {
            TranslatableText invalid = new TranslatableText("original", "en", Map.of("deutsch", "Übersetzung"));
            assertNull(invalid);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("invalid translation map"));
        }

    }

    @Test
    public void setTranslation() {
        // Translatable text without translation
        TranslatableText text = TranslatableText.create("original", "en");
        assertEquals("original", text.getText());
        assertEquals("original", text.getText("en"));
        assertEquals("original", text.getTranslation("en"));
        assertEquals("en", text.getOriginalLanguage());
        assertFalse(text.isEmpty());
        assertFalse(TranslatableText.isNull(text));
	    assertNull(text.getTranslation("de"));
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
    public void testIsTranslatableText() {
        assertTrue(TranslatableText.isTranslatableText(null));
        assertFalse(TranslatableText.isTranslatableText(""));
        // @todo: is this really a translatable text?
        assertTrue(TranslatableText.isTranslatableText("\n<=@#!=>\n"));
        assertTrue(TranslatableText.isTranslatableText("\n<=@#!=>\n\n<=@#!=>\n"));

        String encodedValue = TranslatableText.create(null, "de").getEncodedValue();
        assertTrue(TranslatableText.isTranslatableText(encodedValue));
        assertFalse(TranslatableText.isTranslatableText("a normal string"));
    }

    @Test
    public void translationLookup() {
        // languages: en,de,fr,ru
        String encoded = "\n<=@#!=>\nen:original\n<=@#!=>\nde:Original\n<=@#!=>\nfr:originale\n<=@#!=>\nru:оригинал\n<=@#!=>\n";
        TranslatableText text = new TranslatableText(encoded);
        assertEquals("original", text.getText());
        assertEquals("originale", text.translationLookup("fr"));
        // @todo: the language translations are lost
        assertEquals("оригинал", text.translationLookup("ru"));
        assertEquals(encoded, text.getEncodedValue());
    }

    @Test
    public void createWithEncodedValue() {
        String encodedValueNull = null;
        TranslatableText text = new TranslatableText(encodedValueNull);
        assertNotNull(text);
        assertNull(text.getEncodedValue());
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));

        text = new TranslatableText("\n<=@#!=>\n");
        assertEquals("\n<=@#!=>\n", text.getEncodedValue());
        assertNull(text.getText());
        assertNull(text.getTranslation("en"));

        String encodedValueWithTranslation = "\n<=@#!=>\nen=text-en\n<=@#!=>\nde=text-de\n<=@#!=>\n";
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
        String encodedValueWithDuplicateLanguage = "\n<=@#!=>\nen=text-en\n<=@#!=>\nen=text-en\n<=@#!=>\nde=text-de\n<=@#!=>\n";
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
        String inconsistentEncodedValue = "\n<=@#!=>\nen=text-en\n<=@#!=>\nen=text-en2\n<=@#!=>\n";
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

    }

    @Test
    public void getEncodedValue() {
        TranslatableText text = TranslatableText.create("original", "en");
        // @todo: do we need a translation map here?
        assertNotNull(text.getTranslationMap());
        // @todo: here we have duplicated text
//        String encodedValueSingle = "\n<=@#!=>\nen:original\n<=@#!=>\nen:original\n<=@#!=>\n";
        String encodedValueSingle = "\n<=@#!=>\nen:original\n<=@#!=>\n";
        assertEquals(encodedValueSingle, text.getEncodedValue());
        assertNotNull(text.getTranslationMap());
        assertEquals(0, text.getTranslationMap().size());

        text = new TranslatableText(encodedValueSingle);
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


    public void compareImplementations() throws IOException {
        int loops = 10;
        int size = 1_000_000;
        for (int n = 0; n < loops; n++) {
            long time = System.currentTimeMillis();
            List<String> encodedList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TranslatableText translatableText = new TranslatableText("text" + i, "de").setTranslation("text-en" + i, "en").setTranslation("text-fr" + i, "fr");
                String encoded = translatableText.getEncodedValue();
                encodedList.add(encoded);
            }
            System.out.println("Time encode: " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            for (int i = 0; i < size; i++) {
                String value = encodedList.get(i);
                TranslatableText translatableText = new TranslatableText(value);
                if (!("text-en" + i).equals(translatableText.getText("en"))) {
                    System.out.println("Error:" + translatableText.getText("en"));
                }
                if (!("text" + i).equals(translatableText.getText())) {
                    System.out.println("Error:" + translatableText.getText());
                }
            }
            System.out.println("Time decode: " + (System.currentTimeMillis() - time));
        }

        System.out.println("Binary:...");
        for (int n = 0; n < loops; n++) {
            long time = System.currentTimeMillis();
            List<byte[]> encodedList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                BinaryTranslatedText translatableText = new BinaryTranslatedText("text" + i, "de").setTranslation("text-en" + i, "en").setTranslation("text-fr" + i, "fr");
                byte[] encoded = translatableText.getEncodedValue();
                encodedList.add(encoded);
            }
            System.out.println("Time encode: " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
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
            System.out.println("Time decode: " + (System.currentTimeMillis() - time));
        }

    }

    public void testTestTranslationLookup() {
    }

    public void testWriteValues() {
    }

}
