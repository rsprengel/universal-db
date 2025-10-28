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
import org.apache.commons.lang3.mutable.MutableInt;
import org.teamapps.universaldb.util.DataStreamUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class TranslatableText {

    private final static int MAX_VERSION = 2;
    protected final static String OLD_DELIMITER = "\n<=@#!=>\n";
    protected final static String DELIMITER = "\n<=@#~=>\n";
//    protected final static String DELIMITER = "\u200B";
    protected final static int VERSION_POSITION = DELIMITER.length();

    // this can be changed before first use
    private static int STANDARD_VERSION = 2;

    private final String originalLanguage;
    private String originalText;
    private String encodedValue;
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

    public static int getStandardWriteVersion() {
        return STANDARD_VERSION;
    }

    public static void setStandardWriteVersion(int version) {
        if (version >= 0 && version <= MAX_VERSION) {
            STANDARD_VERSION = version;
        }
    }

    public static boolean isTranslatableText(String encodedValue) {
	    return encodedValue == null ||
			    encodedValue.isEmpty() ||
			    (encodedValue.startsWith(OLD_DELIMITER) && encodedValue.endsWith(OLD_DELIMITER)) ||
			    (encodedValue.startsWith(DELIMITER) && encodedValue.length() >= DELIMITER.length() + 1);
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
        if (originalLanguage.length()!=2 && originalLanguage.length()!=3) {
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

    public void normalize() {
        if (translationMap==null) {
            initMembersFromEncodedText();
        }
        encodedValue = createEncodedValue();
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

	public String translationLookup(String language) {
		if (language == null || (language.length() != 2 && language.length() != 3)) {
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
		return findTranslation(language);
	}

	public String getTranslation(String language) {
        if (StringUtils.equals(language, originalLanguage)) {
            return originalText;
        }
        if (translationMap != null) {
            return translationMap.get(language);
        } else if (encodedValue!=null) {
            return findTranslation(language);
        }
        return null;
    }

    public String getTranslation(List<String> rankedLanguages) {
        if (rankedLanguages.isEmpty()) {
            return "";
        }
        if (rankedLanguages.getFirst().equals(originalLanguage)) {
            return originalText;
        }
        if (translationMap==null && encodedValue!=null) {
            initMembersFromEncodedText();
        }
        for (String language : rankedLanguages) {
            String translation = getTranslation(language);
            if (translation != null) {
                return translation;
            }
        }
        return null;
    }

    public Map.Entry<String, String> getTranslationEntry(List<String> rankedLanguages) {
        if (rankedLanguages.isEmpty()) {
            return null;
        }
        if (rankedLanguages.getFirst().equals(originalLanguage)) {
            return new AbstractMap.SimpleEntry<>(originalLanguage, originalText);
        }
        if (translationMap==null && encodedValue!=null) {
            initMembersFromEncodedText();
        }
        for (String language : rankedLanguages) {
            String translation = getTranslation(language);
            if (translation != null) {
                return new AbstractMap.SimpleEntry<>(language, translation);
            }
        }
        return null;
    }

    public TranslatableText setTranslation(String translation, String language) {
        if (translation == null || translation.isEmpty() || language == null || (language.length() != 2 && language.length() != 3)) {
            return this;
        }
        if (StringUtils.equals(language, originalLanguage)) {
			if (encodedValue!=null) {
				// we must create the translationMap here, because we must delete the encodedValue
				initMembersFromEncodedText();
			}
			originalText = translation;
        } else {
            getTranslationMap().put(language, translation);
        }
	    encodedValue = null;
        return this;
    }

    // method with side effects
    public String getEncodedValue() {
        if (encodedValue != null) {
            return encodedValue;
        } else if (originalText!=null || translationMap!=null) {
            encodedValue = createEncodedValue();
            return encodedValue;
        } else {
            return null;
        }
    }

    private static int parseVersion(String encodedValue) {
        if (encodedValue==null || encodedValue.length()<DELIMITER.length())
            return -1;
        if (encodedValue.startsWith(DELIMITER)) {
            if (VERSION_POSITION >= encodedValue.length()) {
                return -1;
            }
            return encodedValue.charAt(VERSION_POSITION) - '0';
        }
        return 0;
    }

    private static String getDelimiter(String encodedValue) {
        if (parseVersion(encodedValue)==0) {
            return OLD_DELIMITER;
        }
        return DELIMITER;
    }

    // return originalLanguage
    // method with side effects
    private String parseOriginalValue() {
        if (encodedValue==null)
            return null;
        int version = parseVersion(encodedValue);
        return switch (version) {
            case 0, 1 -> parseOriginalValueWithSeparator(version);
	        case 2 -> parseOriginalValueWithLengthEncoding();
            default -> throw new RuntimeException("Error: invalid translation encoding:" + encodedValue);
        };
    }

    private String parseOriginalValueWithLengthEncoding() {
        int pos=VERSION_POSITION+1;
        if (encodedValue.length()<VERSION_POSITION+3) {
            throw new RuntimeException("Error: language colon missing");
        }
        int len = (encodedValue.charAt(pos + 2) == ':' ? 2 : 3);
        originalText = readString(encodedValue, new MutableInt(pos+3));
        return encodedValue.substring(pos, pos + len);
    }

    private String parseOriginalValueWithSeparator(int version) {
        String delimiter;
        int pos;
        switch (version) {
            case 0:
                delimiter= OLD_DELIMITER;
                pos = OLD_DELIMITER.length();
                break;
            case 1:
                delimiter= DELIMITER;
                pos=DELIMITER.length()+1;
                break;
            default:
                return null;
        }
        int end = encodedValue.indexOf(delimiter, pos+1);
        if (end < 0) end = encodedValue.length();
        if (end == pos) {
            return null;
        }
        if (end > pos + 2) {
            int len = (encodedValue.charAt(pos + 2) == ':' ? 2 : 3);
            originalText = encodedValue.substring(pos + 3, end);
            return encodedValue.substring(pos, pos + len);
        }
        throw new RuntimeException("Error: language is not an iso code");
    }

    private String findTranslation( String language) {
		// intern method, this method does not search for the original language
	    // call it only, if language is valid (2 or 3 characters) and different from original language
        int version = parseVersion(encodedValue);
        if (version == 2) {
            return findTranslationLengthEncoded(language);
        } else {
            return findTranslationWithSeparator(language);
        }
    }

    private String findTranslationWithSeparator(String language) {
		// intern method, this method does not search for the original language
	    // call it only, if language is valid (2 or 3 characters) and different from original language
        String delimiter = getDelimiter(encodedValue);
        char a = language.charAt(0);
        char b = language.charAt(1);
		char c;
		if (language.length()==2) {
			c = ':';
		} else {
			c = language.charAt(2);
		}
        int pos = delimiter.length()+1;
        while((pos = encodedValue.indexOf(delimiter, pos + 1)) >= 0 && pos < encodedValue.length() - delimiter.length()) {
            if (encodedValue.charAt(pos + delimiter.length()) == a && encodedValue.charAt(pos + delimiter.length() + 1) == b && encodedValue.charAt(pos + delimiter.length() + 2) == c) {
                int end = encodedValue.indexOf(delimiter, pos + 1);
                if (end > pos) {
                    return encodedValue.substring(pos + delimiter.length() + 3, end);
                } else {
                    return encodedValue.substring(pos + delimiter.length() + 3);
                }
            }
        }
        return null;
    }

    private String findTranslationLengthEncoded( String language) {
		// intern method, this method does not search for the original language
	    // call it only, if language is valid (2 or 3 characters) and different from original language
        char a = language.charAt(0);
        char b = language.charAt(1);
		char c = language.length()==2 ? ':' : language.charAt(2);

        // skip original language
        // magic-number + (version=1) + (language=3) + base94length + ~ + text-length
        MutableInt pos = new MutableInt(DELIMITER.length() + 4 + encodedStringLength(originalText));
        while (pos.intValue() < encodedValue.length()) {
            boolean match = encodedValue.charAt(pos.intValue()) == a && encodedValue.charAt(pos.intValue() + 1) == b && encodedValue.charAt(pos.intValue() + 2) == c;
            pos.add(3);
            if (match) {
                return readString(encodedValue, pos);
            } else {
                skipString(encodedValue, pos);
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
    // note: the translationMap does not contain the original language
    protected Map<String, String> getTranslationMap() {
        if (translationMap != null) {
            return translationMap;
        } else if (encodedValue!=null) {
            initMembersFromEncodedText();
            return translationMap;
        } else {
            return Map.of();
        }
    }

    public boolean contains(String language) {
	    if (StringUtils.equals(language, originalLanguage))
		    return true;
        if (translationMap==null) {
            return (encodedValue!=null && findTranslation(language) != null);
            // @todo: or should we call initMembersFromEncodedText() here?
//            			initMembersFromEncodedText();
        }
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
        if (translationMap==null && encodedValue!=null) {
            initMembersFromEncodedText();
        }
		if (translationMap==null) {
			return List.of(originalLanguage);
		}
		List<String> result = new ArrayList<>();
		if (!translationMap.containsKey(originalLanguage))
	        result.add(originalLanguage);
		result.addAll(translationMap.keySet());
        return result;
    }

    private void initMembersFromEncodedText() {
		// we can assume, that originalLanguage and originalValue are already set correctly
	    // so we will skip the original value here
        translationMap = new HashMap<>();
        int version = parseVersion(encodedValue);
        if (version == 2) {
            createTranslationMapFromLengthEncodedValue(encodedValue);
        } else if (version >= 0) {
            String delimiter = getDelimiter(encodedValue);
            int pos = delimiter.length() + 1 + originalText.length();
            while ((pos = encodedValue.indexOf(delimiter, pos + 1)) >= 0) {
                int end = encodedValue.indexOf(delimiter, pos + delimiter.length());
                if (end < 0) end = encodedValue.length();
                if (end > pos + delimiter.length()) {
                    String language;
                    String value;
                    pos += delimiter.length();
                    if (encodedValue.charAt(pos + 2) == ':') {
                        language = encodedValue.substring(pos, pos + 2);
                    } else {
                        language = encodedValue.substring(pos, pos + 3);
                    }
                    value = encodedValue.substring(pos + 3, end);
                    if (!language.equals(originalLanguage)) {
                        translationMap.put(language, value);
                    } else {
                        // @todo: or should we ignore the new value of the original text?
                        originalText = value;
                    }
                    pos = end - 1;
                }
            }
        }
    }

    // for test classes
    protected String createEncodedValue(int version) {
        if (encodedValue==null) {
            if (STANDARD_VERSION == version) {
                return getEncodedValue();
            }
        } else if (encodedValue.length()>1 && encodedValue.charAt(VERSION_POSITION) + 48 == version) {
            return encodedValue;
        }
        if (version == 0) {
            return createOldEncodedValue();
        } else if (version == 2) {
            return createLengthEncodedValue();
        } else {
            return createSeparatedEncodedValue(1, DELIMITER);
        }
    }

    private String createOldEncodedValue() {
        StringBuilder sb = new StringBuilder();
        if (originalText != null && originalLanguage != null) {
            sb.append(OLD_DELIMITER).append(originalLanguage);
            if (originalLanguage.length()==2) {
                sb.append(':');
            }
            sb.append(originalText);
        }
        if (translationMap==null) {
            initMembersFromEncodedText();
        }
        for (Map.Entry<String, String> entry : translationMap.entrySet()) {
            if (!entry.getKey().equals(originalLanguage)) {
                sb.append(OLD_DELIMITER).append(entry.getKey());
                if (entry.getKey().length()==2) {
                    sb.append(':');
                }
                sb.append(entry.getValue());
            }
        }
        sb.append(OLD_DELIMITER);
        return sb.toString();
    }

    public static void writeString(StringBuilder sb, String text) {
        int length;
        if (text==null || (length=text.length()) == 0) {
            sb.append(" ~");
        } else {
            char[] buf = new char[16];
            int inx = 15;
            while (length > 0) {
                buf[inx--] = (char) (32 + (length % 94));
                length /= 94;
            }
            sb.append(String.valueOf(buf, inx + 1, 15 - inx)).append('~').append(text);
        }
    }

    public static int encodedStringLength(String text) {
        int length = text.length();
        if (length < 94) return length + 2;

        int result = length;
        while (length > 0) {
            result++;
            length /= 94;
        }
        return result+1;
    }

    private String readString(String encodedValue, MutableInt pos) {
        int len = 0;
        int begin=pos.intValue();
        for (; begin<encodedValue.length(); ++begin) {
            char c = encodedValue.charAt(begin);
            if (c == '~') break;
            if (c < ' ' || c > '~') {
                throw new RuntimeException("Error: parsing encoded TranslatableText at " + pos.intValue());
            }
            len = len * 94 + (c-32);
            if (len < 0) {
                throw new RuntimeException("Error: parsing encoded TranslatableText at " + pos.intValue());
            }
        }
        if (begin+len >= encodedValue.length()) {
            throw new RuntimeException("Error: parsing encoded TranslatableText at " + pos.intValue());
        }
        pos.setValue(begin+len+1);
        return encodedValue.substring(begin+1, pos.getValue());
    }

    private void skipString(String encodedValue, MutableInt pos) {
        int len = 0;
        int begin=pos.intValue();
        for (; begin<encodedValue.length(); ++begin) {
            char c = encodedValue.charAt(begin);
            if (c == '~') break;
            if (c < ' ' || c > '~') {
                throw new RuntimeException("Error: parsing encoded TranslatableText at " + pos.intValue());
            }
            len = len * 94 + (c-32);
        }
        pos.setValue(begin+len+1);
    }

    public String createLengthEncodedValue() {
        if (isNull(this)) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(DELIMITER).append('2').append(originalLanguage);
        if (originalLanguage.length()==2) {
            sb.append(':');
        }
        writeString(sb, originalText);
        if (translationMap==null) {
            initMembersFromEncodedText();
        }
        for (Map.Entry<String, String> entry : translationMap.entrySet()) {
            if (!entry.getKey().equals(originalLanguage)) {
                sb.append(entry.getKey());
                if (entry.getKey().length()==2) {
                    sb.append(':');
                }
                writeString(sb, entry.getValue());
            }
        }
        return sb.toString();
    }

    public void createTranslationMapFromLengthEncodedValue(String encodedIndexValue) {
        // we can assume, that originalLanguage and originalValue are already set correctly
        // so we will not parse the original value here
        translationMap = new HashMap<>();
        MutableInt pos = new MutableInt(DELIMITER.length()+1);
        while(pos.intValue() >= 0 && pos.intValue() < encodedIndexValue.length() - 1) {
            String language = encodedIndexValue.substring(pos.intValue(), pos.intValue() + (encodedIndexValue.charAt(pos.intValue() + 2) == ':' ?  2 : 3));
            pos.add(3);
            if (!Objects.equals(language, originalLanguage)) {
                String value = readString(encodedValue, pos);
                translationMap.put(language, value);
            } else {
                skipString(encodedValue, pos);
            }
        }
    }


    // method with side effects
    private String createSeparatedEncodedValue(int version, String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (originalText != null && originalLanguage != null) {
            sb.append(delimiter);
            if (version > 0) {
                sb.append((char) (48 + version));
            }
            sb.append(originalLanguage);
            if (originalLanguage.length()==2) {
                sb.append(':');
            }
            sb.append(originalText);
        }
        if (translationMap==null) {
            initMembersFromEncodedText();
        }
        for (Map.Entry<String, String> entry : translationMap.entrySet()) {
            if (!entry.getKey().equals(originalLanguage)) {
                sb.append(delimiter).append(entry.getKey());
                if (entry.getKey().length()==2) {
                    sb.append(':');
                }
                sb.append(entry.getValue());
            }
        }
        if (version == 0) {
            sb.append(delimiter);
        }
        return sb.toString();
    }

    private String createEncodedValue() {
	    return switch (STANDARD_VERSION) {
		    case 0 -> createSeparatedEncodedValue(STANDARD_VERSION, OLD_DELIMITER);
		    case 1 -> createSeparatedEncodedValue(STANDARD_VERSION, DELIMITER);
		    case 2 -> createLengthEncodedValue();
		    default -> "";
	    };
    }

    // method with side effects
    @Override
    public boolean equals(Object o) {
        if (!equalsOriginal(o)) return false;
        if (originalLanguage==null) return true;
        TranslatableText that = (TranslatableText) o;
        if (translationMap!=null && that.translationMap!=null) {
            return translationMap.equals(that.translationMap);
        }
        String thatEncoded = that.getEncodedValue();
        if (encodedValue!=null && that.encodedValue!=null && thatEncoded.charAt(VERSION_POSITION)==encodedValue.charAt(VERSION_POSITION)) {
            return Objects.equals(getEncodedValue(), thatEncoded);
        }
        // version of encoding is different
        return Objects.equals(getTranslationMap(), that.getTranslationMap());
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
