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
import java.util.function.Function;

import static org.junit.Assert.*;

public class TranslatableTextBenchmarkTests {

    public void measureEncodeDecode(String prefix, Function<TranslatableText, String> encode, String... languages) {
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
