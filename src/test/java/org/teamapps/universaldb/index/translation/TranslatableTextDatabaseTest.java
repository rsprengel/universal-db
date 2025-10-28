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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.teamapps.datamodel.testdb1.FieldTest;
import org.teamapps.universaldb.TestBase;
import org.teamapps.universaldb.context.UserContext;
import org.teamapps.universaldb.index.text.TextFilter;

import static org.junit.Assert.*;

public class TranslatableTextDatabaseTest {

    @BeforeClass
    public static void init() throws Exception {
        TestBase.init();
		TranslatableText.setStandardWriteVersion(2);
	    fillDatabase();
    }

    @Test
    public void testLargeText() {
        TranslatableText translatableText = TranslatableText.create("en-text", "en")
                .setTranslation("de-text", "de")
                .setTranslation("fr-text", "fr");
        FieldTest.create()
                .setTextField("ID1")
                .setTranslatableText(translatableText)
                .save();

        UserContext context = UserContext.create("fr", "de");
//        UserContext context = UserContext.create("de");
        FieldTest fieldTest = FieldTest.filter()
                .translatableText(TranslatableTextFilter.termContainsFilter("fr", context))
                .executeExpectSingleton();

        assertEquals("Test.ID1", fieldTest.getTextField());
    }

	@Test
	public void testMultiText() {
		UserContext context = UserContext.create("fr", "de");
		List<FieldTest> fieldTest1 = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("clairieres", context))
				.execute();
		assertEquals(4, fieldTest1.size());
		assertEquals("Test.ID3", fieldTest1.getFirst().getTextField());

		assertEquals("Test.ID7", FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("schlief", context))
				.executeExpectSingleton().getTextField());
	}

	@Test
	public void testMoreLanguages() {
		UserContext context = UserContext.create("de", "nl", "en", "fr", "el", "zh", "ja", "he");
		List<FieldTest> fieldTest = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("Aankoopen", context))
				.execute();
		assertEquals(4, fieldTest.size());
		assertEquals("Test.ID5", fieldTest.get(0).getTextField());
		assertEquals("Test.ID7", fieldTest.get(1).getTextField());
		assertEquals("Test.ID8", fieldTest.get(2).getTextField());

		fieldTest = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("כנף. משים והכו", context))
				.execute();
		assertEquals(2, fieldTest.size());
		assertEquals("Test.ID6", fieldTest.getFirst().getTextField());

		fieldTest = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("後竊聽", context))
				.execute();
		assertEquals(9, fieldTest.size());
		assertEquals("Test.ID1", fieldTest.getFirst().getTextField());

		fieldTest = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("第八章 第三章", context))
				.execute();
		assertEquals(11, fieldTest.size());
		assertEquals("Test.ID1", fieldTest.getFirst().getTextField());

		assertEquals(4, fieldTest.stream().filter(s -> s.getTranslatableText().getText("ja").contains("第八章 第三章")).count());
	}

	@Test
	public void searchTranslatableTextTest() {
		UserContext context = UserContext.create("de", "en");

		// textEqualsFilter ...
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter("Bundesrepublik Deutschland", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter("federal republic of germany", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter("República Federal de Alemania", UserContext.create("de", "en", "es"))).executeExpectSingleton().getTextField());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter("germany", context)).executeExpectSingleton());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter("republic of germany", context)).executeExpectSingleton());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter("República Federal de Alemania", context)).executeExpectSingleton());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter("Deutschland", context)).executeExpectSingleton());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter("Deutschland Bundesrepublik", context)).executeExpectSingleton());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter("bundesrepublik deutschland", context)).executeExpectSingleton());

		// termContainsFilter ...
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("Deutschland", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("Deutschland", context))
				.translatableText(TranslatableTextFilter.termContainsFilter("Bundesrepublik", context))
				.executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("Deutschland", context))
				.translatableText(TranslatableTextFilter.termContainsFilter("germany", context))
				.executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("Republik", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("Alemania", UserContext.create("de", "en", "es"))).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("germany", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("federal germany", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("germany federal", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().fullTextFilter(TranslatableTextFilter.termContainsFilter("Bundesrepublik", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().fullTextFilter(TranslatableTextFilter.termContainsFilter("Deutsch Bund", context)).executeExpectSingleton().getTextField());
		assertNull(FieldTest.filter().fullTextFilter(TranslatableTextFilter.termContainsFilter("Alemania", context)).executeExpectSingleton());

		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("Deutschland germany", context)).executeExpectSingleton());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter("Alemania", context)).executeExpectSingleton());

		// termStartsWithFilter ...
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termStartsWithFilter("Deutsch", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termStartsWithFilter("bund", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termStartsWithFilter("bund deutsch", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termStartsWithFilter("Deutschland Bundesrepublik", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termStartsWithFilter("german", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termStartsWithFilter("republic", context)).executeExpectSingleton().getTextField());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.termStartsWithFilter("Republik", context)).executeExpectSingleton());

		// termEqualsFilter ...
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termEqualsFilter("Bundesrepublik", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termEqualsFilter("Deutschland", context)).executeExpectSingleton().getTextField());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.termEqualsFilter("Republik", context)).executeExpectSingleton());

		// termSimilarFilter
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termSimilarFilter("republica federal de alemania", UserContext.create("de", "en", "es"))).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termSimilarFilter("fedderal republick off germanie", context)).executeExpectSingleton().getTextField());
		assertEquals("Country.germany", FieldTest.filter().translatableText(TranslatableTextFilter.termSimilarFilter("republic of germany", context)).executeExpectSingleton().getTextField());
		assertNull(FieldTest.filter().translatableText(TranslatableTextFilter.termSimilarFilter("rep germany", context)).executeExpectSingleton());
	}

	@Test
	public void searchEqualsVeryLargeNumberOfTerms() {
		UserContext context = UserContext.create("de", "nl", "en", "fr", "zh", "ja", "he", "el");
		String[] german = readLanguage("german");

		String search = german[10];
		assertFalse(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter(search, context))
				.execute().isEmpty());
		assertFalse(FieldTest.filter().translatableText(TranslatableTextFilter.textNotEqualsFilter(search, context))
				.execute().isEmpty());

		search = search + " noch was";
		assertTrue(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter(search, context))
				.execute().isEmpty());

		assertFalse(FieldTest.filter().translatableText(TranslatableTextFilter.textNotEqualsFilter(search, context))
				.execute().isEmpty());

		assertFalse(FieldTest.filter().translatableText(TranslatableTextFilter.termEqualsFilter("hauswesen neugierig", context))
				.execute().isEmpty());

//		assertFalse(FieldTest.filter().translatableText(TranslatableTextFilter.termEqualsFilter(search, context))
//				.execute().isEmpty());

	}

	@Test
	public void searchBenchMark() {
		String[] english = readLanguage("english");
		String[] german = readLanguage("german");
		String[] greek = readLanguage("greek");

		UserContext context = UserContext.create("de", "nl", "en", "fr", "zh", "ja", "he", "el");
		String[] matchStrings = {"clairieres", "schlief", "Aankoopen", "הַבַּרְזֶל", "後竊聽", "第八章 第三章", "αποδεκτών", "RECOMMEND"};
		String[] noMatchStrings = {"clairieresx", "schliefx", "Aankoopenx", "xהַבַּרְזֶל", "後竊聽x", "第八章 第三章x", "αποδεκτώνx", "RECOMMENDx"};
		for (int i=0; i<3; ++i) {
			long singletonTime = System.nanoTime();
			for (String search : matchStrings) {
				assertNotNull(FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter(search, context)).executeExpectSingleton());
			}
			singletonTime = System.nanoTime() - singletonTime;

			long allTime = System.nanoTime();
			for (String search : matchStrings) {
				assertTrue(FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter(search, context)).execute().size() > 1);
			}
			allTime = System.nanoTime() - allTime;

			long noMatchTime = System.nanoTime();
			for (String search : noMatchStrings) {
				assertTrue(FieldTest.filter().translatableText(TranslatableTextFilter.termContainsFilter(search, context)).execute().isEmpty());
			}
			noMatchTime = System.nanoTime() - noMatchTime;

			long germanTime = System.nanoTime();
			for (int germanInx=0; germanInx<11; ++germanInx) {
				String search = german[germanInx];
				assertFalse(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter(search, context))
						.execute().isEmpty());
			}
			germanTime = System.nanoTime() - germanTime;

			long greekTime = System.nanoTime();
			for (int greekInx=0; greekInx<10; ++greekInx) {
				String search = greek[greekInx];
				assertFalse(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter(search, context)).execute().isEmpty());
			}
			greekTime = System.nanoTime() - greekTime;

			long englishTime = System.nanoTime();
			for (int englishInx=0; englishInx<10; ++englishInx) {
				String search = english[englishInx];
				assertFalse(FieldTest.filter().translatableText(TranslatableTextFilter.textEqualsFilter(search, context))
						.execute().isEmpty());
			}
			englishTime = System.nanoTime() - englishTime;

			System.out.println("searchSingle=" + (singletonTime / 1000_000.0d) + "ms, searchAll=" + (allTime / 1000_000.0d) + "ms, searchNoMatch=" + (noMatchTime / 1000_000.0d) + "ms, equalsDE=" + (germanTime / 1000_000.0d) + "ms, equalsEL=" + (greekTime / 1000_000.0d) + "ms, equalsEN=" + (englishTime / 1000_000d));
		}
	}

	@Test
	public void getTextBenchmark() {
		// sequence of language in encodedText: en, de, ja, el, fr, he, nl, zh
		TranslatableText text = FieldTest.filter().textField(TextFilter.textEqualsFilter("Test.ID11")).executeExpectSingleton().getTranslatableText();
		String[] languages = {"de", "nl", "en", "fr", "zh", "ja", "he", "el"};
		for (int j=0; j<2; ++j) {
			TranslatableText freshText = new TranslatableText(text.getEncodedValue());
			for (String language : languages) {
				long start = System.nanoTime();
				for (int i = 0; i < 1000; ++i) {
					assertTrue(freshText.contains(language));
					String value = freshText.getText(language);
					assertNotNull(value);
				}
				long finish = System.nanoTime();
				System.out.println("getText(\"" + language + "\")=" + (finish - start) / 1000_000.0d + "ms");
			}
		}
	}

	public void measureEncodeDecode(String prefix, Function<TranslatableText, String> encode) throws IOException {
		int loops = 10;
		int size = 10_000;
		long fullTimeEncode = 0;
		long fullTimeGet = 0;
		long fullTimeDecode = 0;
		long minEncode = Long.MAX_VALUE;
		long maxGet = 0;
		long minGet = Long.MAX_VALUE;
		long maxEncode = 0;
		long minDecode = Long.MAX_VALUE;
		long maxDecode = 0;
		String[] languages = {"de", "nl", "en", "fr", "zh", "ja", "he", "el"};
		TranslatableText text = FieldTest.filter().textField(TextFilter.textEqualsFilter("Test.ID11")).executeExpectSingleton().getTranslatableText();
		String encodedValue = encode.apply(text);
		System.out.println(prefix + " text encoding:...");
		System.out.printf("  length=%,d UTF8=%,d\n", encodedValue.length(), encodedValue.getBytes(StandardCharsets.UTF_8).length);
		for (int n = 0; n < loops; n++) {
			long startTime = System.currentTimeMillis();
			List<String> encodedList = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				String encoded = encode.apply(text);
				encodedList.add(encoded);
			}
			long encodeDuration = System.currentTimeMillis() - startTime;

			startTime = System.currentTimeMillis();
			for (int i = 0; i < size; i++) {
				String value = encodedList.get(i);
				TranslatableText translatableText = new TranslatableText(value);
				for (String language :  languages) {
					if (StringUtils.isBlank(translatableText.getText(language))) {
						System.out.println("Error:" + language + " is empty");
					}
				}
			}
			long getTranslationDuration = System.currentTimeMillis() - startTime;

			startTime = System.currentTimeMillis();
			for (int i = 0; i < size; i++) {
				String value = encodedList.get(i);
				TranslatableText translatableText = new TranslatableText(value);
				Map<String, String> map = translatableText.getTranslationMap();
				assertEquals(7, map.size());
			}
			long decodeDuration = System.currentTimeMillis() - startTime;

			if (n == 0) {
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
		measureEncodeDecode("old", t -> t.createEncodedValue(0));
		measureEncodeDecode("new", t -> t.createEncodedValue(1));
		measureEncodeDecode("len", t -> t.createEncodedValue(2));
	}

	public static void fillDatabase() {
		String[] german = readLanguage("german");
		String[] english = readLanguage("english");
		String[] french = readLanguage("french");
		String[] dutch = readLanguage("dutch");
		String[] chinese = readLanguage("chinese");
		String[] japanese = readLanguage("japanese");
		String[] hebrew = readLanguage("hebrew");
		String[] greek = readLanguage("greek");

		TranslatableText germanyName = TranslatableText.create("Bundesrepublik Deutschland", "de")
				.setTranslation("federal republic of germany", "en")
				.setTranslation("República Federal de Alemania", "es");
		FieldTest germany = FieldTest.create()
				.setTextField("Country.germany").setTranslatableText(germanyName).save();

		FieldTest newField = null;
		for (int i=0; i<11; ++i) {
			TranslatableText translatableText = TranslatableText.create(english[i], "en")
					.setTranslation(german[i], "de")
					.setTranslation(french[i], "fr")
					.setTranslation(dutch[i], "nl")
					.setTranslation(chinese[i], "zh")
					.setTranslation(japanese[i], "ja")
					.setTranslation(hebrew[i], "he")
					.setTranslation(greek[i], "el")
					;
			newField = FieldTest.create()
					.setTextField("Test.ID" + (i+1))
					.setTranslatableText(translatableText)
					.setSingleReferenceField(germany)
					.save();
		}
		newField.setSingleReferenceField(germany).save();
	}

	public static String[] readLanguage(String language) {
		String[] result = new String[11];
		StringBuilder allLines = new StringBuilder();
		try (InputStream is = TranslatableTextDatabaseTest.class.getResourceAsStream(language + ".txt")) {
			Assert.assertNotNull(is);
			try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				int i = 0;
				while (i < result.length && (result[i] = rd.readLine()) != null) {
					allLines.append(result[i++]);
				}
			}
			result[10] = allLines.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
