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
package org.teamapps.universaldb;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.teamapps.datamodel.testdb1.FieldTest;
import org.teamapps.universaldb.context.UserContext;
import org.teamapps.universaldb.index.translation.TranslatableText;
import org.teamapps.universaldb.index.translation.TranslatableTextFilter;

import static org.junit.Assert.*;

public class TranslatableTextTest {

    @BeforeClass
    public static void init() throws Exception {
        TestBase.init();
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

        assertEquals("ID1", fieldTest.getTextField());
    }

	@Test
	public void testMultiText() {
		fillDatabase();
		UserContext context = UserContext.create("fr", "de");
		List<FieldTest> fieldTest1 = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("clairieres", context))
				.execute();
		assertEquals(3, fieldTest1.size());
		assertEquals("ID2", fieldTest1.getFirst().getTextField());

		assertEquals("ID6", FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("schlief", context))
				.executeExpectSingleton().getTextField());
	}

	@Test
	public void testMoreLanguages() {
		fillDatabase();
		UserContext context = UserContext.create("de", "nl", "en", "fr", "el", "zh", "ja", "he");
		List<FieldTest> fieldTest = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("Aankoopen", context))
				.execute();
		assertEquals(3, fieldTest.size());
		assertEquals("ID4", fieldTest.get(0).getTextField());
		assertEquals("ID6", fieldTest.get(1).getTextField());
		assertEquals("ID7", fieldTest.get(2).getTextField());

		fieldTest = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("כנף. משים והכו", context))
				.execute();
		assertEquals(1, fieldTest.size());
		assertEquals("ID5", fieldTest.getFirst().getTextField());

		fieldTest = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("後竊聽", context))
				.execute();
		assertEquals(8, fieldTest.size());
		assertEquals("ID0", fieldTest.getFirst().getTextField());

		fieldTest = FieldTest.filter()
				.translatableText(TranslatableTextFilter.termContainsFilter("第八章 第三章", context))
				.execute();
		assertEquals(10, fieldTest.size());
		assertEquals("ID0", fieldTest.getFirst().getTextField());

		assertEquals(3, fieldTest.stream().filter(s -> s.getTranslatableText().getText("ja").contains("第八章 第三章")).count());
	}

	public void fillDatabase() {
		String[] german = readLanguage("german");
		String[] english = readLanguage("english");
		String[] french = readLanguage("french");
		String[] dutch = readLanguage("dutch");
		String[] chinese = readLanguage("chinese");
		String[] japanese = readLanguage("japanese");
		String[] hebrew = readLanguage("hebrew");
		String[] greek = readLanguage("greek");

		for (int i=0; i<10; ++i) {
			TranslatableText translatableText = TranslatableText.create(english[i], "en")
					.setTranslation(german[i], "de")
					.setTranslation(french[i], "fr")
					.setTranslation(dutch[i], "nl")
					.setTranslation(chinese[i], "zh")
					.setTranslation(japanese[i], "ja")
					.setTranslation(hebrew[i], "he")
					.setTranslation(greek[i], "el")
					;
			FieldTest.create()
					.setTextField("ID" + i)
					.setTranslatableText(translatableText)
					.save();
		}
	}

	public static String[] readLanguage(String language) {
		String[] result = new String[10];
		System.out.println("read " +  language + " ...");
		try (InputStream is = TranslatableTextTest.class.getResourceAsStream(language + ".txt")) {
			Assert.assertNotNull(is);
			try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				int i = 0;
				while (i < result.length && (result[i++] = rd.readLine()) != null) {
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
