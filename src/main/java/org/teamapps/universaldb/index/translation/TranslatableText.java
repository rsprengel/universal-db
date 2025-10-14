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
import org.teamapps.universaldb.util.DataStreamUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class TranslatableText {

    protected final static String DELIMITER = "\n<=@#!=>\n";

    private String originalText;
    private final String originalLanguage;
    private String encodedValue;
    // current test class needs access to the translationMap
    protected Map<String, String> translationMap;

    // a translation text is consistent in the following cases
    // 1. encodedValue != null with first entry == originalLanguage/originalText and rest is the same as in translationMap
    // 2. encodedValue != null with first entry == originalLanguage/originalText and translationMap == null
    // 3. encodedValue == null
    // conclusion:
    // if originalLanguage == null then TranslatableText is null and the isNull() is true. You can't set any translation!
    // originalText is null when originalLanguage is null or it contains the correct value (even after initialization from encodedValue)
    // if originalLanguage != null then encodedValue is null and translationMap contains all translations or encodedValue is complete and translationMap is complete or null
    // if translationMap != null then it contains all translations (but not necessarily the original language) and original language is set
    // translatableMap should contain only translations and not the original language (but is not inconsistent otherwise)
    // encodedValue must have original language as first entry and should not duplicate a language (but is not inconsistent otherwise)

    public static boolean isTranslatableText(String encodedValue) {
        if (encodedValue == null || (encodedValue.startsWith(DELIMITER) && encodedValue.endsWith(DELIMITER))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNull(TranslatableText text) {
        return text==null || text.originalLanguage==null;
    }

    public static TranslatableText create(String originalText, String originalLanguage) {
        return new TranslatableText(originalText, originalLanguage);
    }

    public TranslatableText() {
        originalLanguage = null;
    }

    public TranslatableText(String originalText, String originalLanguage) {
        if (originalLanguage == null) {
            throw new RuntimeException("Error: no language for translatable text");
        }
        if (originalLanguage.length()!=2) {
            throw new RuntimeException("Error: language is not an iso code");
        }
        this.originalText = originalText;
        this.originalLanguage = originalLanguage;
        this.translationMap = new HashMap<>();
    }

    public TranslatableText(String encodedValue) {
        if (!isTranslatableText(encodedValue)) {
            throw new RuntimeException("Error: invalid translation encoding:" + encodedValue);
        }
        this.encodedValue = encodedValue;
        this.originalLanguage = parseOriginalValue();
    }

    public TranslatableText(DataInputStream dataInputStream) throws IOException {
        this(DataStreamUtil.readStringWithLengthHeader(dataInputStream));
    }

    public TranslatableText(String originalText, String originalLanguage, Map<String, String> translationMap) {
        if (originalLanguage==null || originalLanguage.length()!=2) {
            throw new RuntimeException("Error: invalid original language");
        }
        if (translationMap.keySet().stream().anyMatch(s -> s.length() != 2)) {
            throw new RuntimeException("Error: invalid translation map");
        }
        this.originalText = originalText;
        this.originalLanguage = originalLanguage;
        this.translationMap = translationMap;
    }

    public void normalize() {
        if (translationMap==null) {
            initMembersFromEncodedText();
        }
        encodedValue = createTranslationValue(originalText, originalLanguage, translationMap);
    }

    public boolean isEmpty() {
        return isNull(this) || StringUtils.isEmpty(originalText);
    }

    public String getText() {
        return originalText;
    }

    @Override
    public String toString() {
        return getText();
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public String getText(String language) {
        String translation = getTranslation(language);
        return translation != null ? translation : getText();
    }

    public String getText(String language, String defaultValue) {
        String translation = getTranslation(language);
        return translation != null ? translation : defaultValue;
    }

    public String getText(List<String> rankedLanguages) {
        String translation = getTranslation(rankedLanguages);
        return translation != null ? translation : getText();
    }

    // method with side effects
    public boolean isTranslation(Set<String> languages) {
        if (languages.contains(originalLanguage)) {
            return false;
        }
        Map<String, String> map = getTranslationMap();
        for (String language : languages) {
            if (map.containsKey(language)) {
                return true;
            }
        }
        return false;
    }

    public String getTranslation(String language) {
        if (StringUtils.equals(language, originalLanguage)) {
            return originalText;
        }
        if (translationMap != null) {
            return translationMap.get(language);
        } else {
            return translationLookup(language);
        }
    }

    public String getTranslation(List<String> rankedLanguages) {
        for (String language : rankedLanguages) {
            String translation = getTranslation(language);
            if (translation != null) {
                return translation;
            }
        }
        return null;
    }

    public Map.Entry<String, String> getTranslationEntry(List<String> rankedLanguages) {
        for (String language : rankedLanguages) {
            String translation = getTranslation(language);
            if (translation != null) {
                return new AbstractMap.SimpleEntry<>(language, translation);
            }
        }
        return null;
    }

    public TranslatableText setTranslation(String translation, String language) {
        if (translation == null || translation.isEmpty() || language == null || language.length() != 2) {
            return this;
        }
        if (StringUtils.equals(language, originalLanguage)) {
            originalText = translation;
        } else {
            getTranslationMap().put(language, translation);
        }
        encodedValue = null;
        return this;
    }

    public String translationLookup(String language) {
        return findTranslation(language);
    }

    // method with side effects
    public String getEncodedValue() {
        if (encodedValue != null) {
            return encodedValue;
        } else if (originalText!=null || translationMap!=null) {
            encodedValue = createTranslationValue(originalText, originalLanguage, translationMap);
            return encodedValue;
        } else {
            return null;
        }
    }

    // return originalLanguage
    // method with side effects
    private String parseOriginalValue() {
        if (encodedValue == null) {
            return null;
        }
        int pos = encodedValue.indexOf(DELIMITER);
        if (pos >= 0 && pos < encodedValue.length() - DELIMITER.length()) {
            int end = encodedValue.indexOf(DELIMITER, pos + 1);
            if (end > pos) {
                originalText = encodedValue.substring(pos +  DELIMITER.length() + 3, end);
                return encodedValue.substring(pos + DELIMITER.length(), pos + DELIMITER.length() + 2);
            }
        }
        return null;
    }

    private String findTranslation( String language) {
        if (language == null || language.length() != 2) {
            return null;
        }
        if (language.equalsIgnoreCase(originalLanguage)) {
            return originalText;
        }
        if (translationMap != null) {
            // note: if translationMap is set, then it is complete!
            return translationMap.getOrDefault(language, null);
        }
        if (encodedValue==null) {
            return null;
        }
        int pos = -1;
        char a = language.charAt(0);
        char b = language.charAt(1);
        while((pos = encodedValue.indexOf(DELIMITER, pos + 1)) >= 0 && pos < encodedValue.length() - DELIMITER.length()) {
            if (encodedValue.charAt(pos + DELIMITER.length()) == a && encodedValue.charAt(pos + DELIMITER.length() + 1) == b) {
                int end = encodedValue.indexOf(DELIMITER, pos + 1);
                if (end > pos) {
                    return encodedValue.substring(pos + DELIMITER.length() + 3, end);
                }
            }
        }
        return null;
    }

    // method with side effects
    public void writeValues(DataOutputStream dataOutputStream) throws IOException {
        String encodedValue = getEncodedValue();
        DataStreamUtil.writeStringWithLengthHeader(dataOutputStream, encodedValue);
    }

    // method with side effects
    protected Map<String, String> getTranslationMap() {
        if (translationMap != null) {
            return translationMap;
        } else {
            initMembersFromEncodedText();
            return translationMap;
        }
    }

    public boolean contains(String language) {
        if (translationMap==null && encodedValue!=null) {
            // @todo: or should we call initMembersFromEncodedText() here?
            return translationLookup(language)!=null;
        }
        if (StringUtils.equals(language, originalLanguage))
            return true;
        return translationMap.containsKey(language);
    }

    public boolean hasTranslations() {
        return !getTranslationMap().isEmpty();
    }

    public int translationsCount() {
        return getTranslationMap().size();
    }

    // method with side effects
    public List<String> getLanguages() {
        List<String> languages = new ArrayList<>(1 + (translationMap==null ? 0 : translationMap.size()));
        if (translationMap==null && encodedValue!=null) {
            initMembersFromEncodedText();
        }
        if (originalLanguage != null) {
            languages.add(originalLanguage);
        }
        if (translationMap!=null) {
            // @todo: here it is possible, that the original language is duplicated
            languages.addAll(translationMap.keySet());
        }
        return languages;
    }

    private void initMembersFromEncodedText() {
        translationMap = new HashMap<>();
        int pos = -1;
        boolean first = true;
        while((pos = encodedValue.indexOf(DELIMITER, pos + 1)) >= 0 && pos < encodedValue.length() - DELIMITER.length()) {
            int end = encodedValue.indexOf(DELIMITER, pos + 1);
            if (end > pos) {
                String language = encodedValue.substring(pos + DELIMITER.length(), pos + DELIMITER.length() + 2);
                String value = encodedValue.substring(pos +  DELIMITER.length() + 3, end);
                if (first) {
                    // note: originalLanguage is already set (final member)
                    originalText = value;
                    first = false;
                } else {
                    translationMap.put(language, value);
                    pos = end - 1;
                }
            }
        }
    }

    private static String createTranslationValue(String originalText, String originalLanguage, Map<String, String> translationsByLanguage) {
        StringBuilder sb = new StringBuilder();
        if (originalText != null && originalLanguage != null) {
            sb.append(DELIMITER).append(originalLanguage).append(":").append(originalText);
        }
        if (translationsByLanguage != null) {
            for (Map.Entry<String, String> entry : translationsByLanguage.entrySet()) {
                if (!entry.getKey().equals(originalLanguage)) {
                    sb.append(DELIMITER).append(entry.getKey()).append(":").append(entry.getValue());
                }
            }
        }
        sb.append(DELIMITER);
        return sb.toString();
    }

    // method with side effects
    @Override
    public boolean equals(Object o) {
        if (!equalsOriginal(o)) return false;
        TranslatableText that = (TranslatableText) o;
        if (encodedValue == null || encodedValue.equals(DELIMITER)) {
            return that.encodedValue == null || that.encodedValue.equals(DELIMITER);
        }
        return Objects.equals(getEncodedValue(), that.getEncodedValue());
    }

    // method with side effects
    @Override
    public int hashCode() {
        return Objects.hash(getEncodedValue());
    }

    public boolean equalsOriginal(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TranslatableText that = (TranslatableText) o;
        return Objects.equals(originalText, that.originalText) && Objects.equals(originalLanguage, that.originalLanguage);
    }

//    @Override
//    public int hashCode() {
//        if (originalLanguage==null && encodedValue!=null) {
//            computeMemberFromEncodedText();
//        }
//        return Objects.hash(originalText, originalLanguage);
//    }
//
}
