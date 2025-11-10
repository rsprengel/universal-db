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

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface TranslatableTextIf {
	String OLD_DELIMITER = "\n<=@#!=>\n";
	String DELIMITER = "\n<=@#~=>\n";

	Supplier<Integer> getVersion = TranslatableTextLengthEncoding::getStandardWriteVersion;
	BiFunction<String, String, TranslatableTextIf> translatableTextFactory = TranslatableTextLengthEncoding::new;
	Supplier<TranslatableTextIf> translatableTextSupplier = TranslatableTextLengthEncoding::new;
	Function<String, TranslatableTextIf> translatableTextFromEncodedValue = TranslatableTextLengthEncoding::new;

//	Supplier<Integer> getVersion = TranslatableTextDelimiterEncoding::getStandardWriteVersion;
//	BiFunction<String, String, TranslatableTextIf> translatableTextFactory = TranslatableTextDelimiterEncoding::new;
//	Supplier<TranslatableTextIf> translatableTextSupplier = TranslatableTextDelimiterEncoding::new;
//	Function<String, TranslatableTextIf> translatableTextFromEncodedValue = TranslatableTextDelimiterEncoding::new;

//	Supplier<Integer> getVersion = OriginalTranslatableText::getStandardWriteVersion;
//	BiFunction<String, String, TranslatableTextIf> translatableTextFactory = OriginalTranslatableText::new;
//	Supplier<TranslatableTextIf> translatableTextSupplier = OriginalTranslatableText::new;
//	Function<String, TranslatableTextIf> translatableTextFromEncodedValue = OriginalTranslatableText::new;

//	Supplier<Integer> getVersion = TranslatableText::getStandardWriteVersion;
//	BiFunction<String, String, TranslatableTextIf> translatableTextFactory = TranslatableText::new;
//	Supplier<TranslatableTextIf> translatableTextSupplier = TranslatableText::new;
//	Function<String, TranslatableTextIf> translatableTextFromEncodedValue = TranslatableText::new;

	static boolean isTranslatableText(String encodedValue) {
		return encodedValue == null ||
				encodedValue.isEmpty() ||
				(encodedValue.startsWith(OLD_DELIMITER) && encodedValue.endsWith(OLD_DELIMITER)) ||
				(encodedValue.startsWith(DELIMITER) && encodedValue.length() >= DELIMITER.length() + 1);
	}

	static boolean isNull(TranslatableTextIf text) {
		return text==null || text.getOriginalLanguage()==null;
	}

	static int getVersion() {
		return getVersion.get();
	}

	static TranslatableTextIf create() {
		return translatableTextSupplier.get();
	}

	static TranslatableTextIf create(String originalText, String originalLanguage) {
		return translatableTextFactory.apply(originalText, originalLanguage);
	}

	static TranslatableTextIf create(String encodedValue) {
		return translatableTextFromEncodedValue.apply(encodedValue);
	}

	String getText();

	String getOriginalLanguage();

	String getText(String language);

	String getText(List<String> rankedLanguages);

	boolean isTranslation(Set<String> languages);

	String getTranslation(String language);

	String getTranslation(List<String> rankedLanguages);

	TranslatableTextIf setTranslation(String translation, String language);

	String translationLookup(String language);

	String getEncodedValue();

	void writeValues(DataOutputStream dataOutputStream) throws IOException;

	Map<String, String> getTranslationMap();

	// methods for new API
	boolean contains(String language);
	List<String> getLanguages();
	String getText(String language, String defaultValue);
	boolean hasTranslations();
	int translationsCount();
	boolean isEmpty();
	void normalize();
	boolean equalsOriginal(Object o);



}