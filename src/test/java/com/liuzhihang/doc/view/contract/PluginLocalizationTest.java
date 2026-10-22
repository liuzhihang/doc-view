package com.liuzhihang.doc.view.contract;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginLocalizationTest extends TestCase {

    private static final String BASE_BUNDLE = "messages/DocViewBundle.properties";
    private static final String SIMPLIFIED_CHINESE_BUNDLE = "messages/DocViewBundle_zh_CN.properties";
    private static final Set<String> GENERATED_TEMPLATE_KEYS = Set.of(
            "template.spring.init", "template.dubbo.init");
    private static final Pattern ARGUMENT_INDEX = Pattern.compile("\\{(\\d+)(?:,[^{}]*)?}");
    private static final Set<String> MULTILINE_RULE_HELP_KEYS = Set.of(
            "settings.rules.title.order",
            "settings.rules.name.order",
            "settings.rules.desc.order",
            "settings.rules.required.fields",
            "settings.rules.required.parameters",
            "settings.rules.field.name.order");

    public void testEnglishBaseContainsOnlyEnglishUiMessages() throws Exception {
        Properties base = load(BASE_BUNDLE);
        Set<String> untranslated = new TreeSet<>();
        for (String key : base.stringPropertyNames()) {
            if (!GENERATED_TEMPLATE_KEYS.contains(key) && containsCjk(base.getProperty(key))) {
                untranslated.add(key);
            }
        }

        assertTrue("English base bundle still contains Chinese UI messages: " + untranslated,
                untranslated.isEmpty());
    }

    public void testSimplifiedChineseBundleCoversTheSameMessagesAndArguments() throws Exception {
        Properties base = load(BASE_BUNDLE);
        Properties simplifiedChinese = load(SIMPLIFIED_CHINESE_BUNDLE);
        assertEquals("The English and zh_CN bundles must define the same keys",
                new TreeSet<>(base.stringPropertyNames()), new TreeSet<>(simplifiedChinese.stringPropertyNames()));

        for (String key : base.stringPropertyNames()) {
            String baseMessage = base.getProperty(key);
            String chineseMessage = simplifiedChinese.getProperty(key);
            assertEquals("MessageFormat argument indexes differ for " + key,
                    argumentIndexes(baseMessage), argumentIndexes(chineseMessage));
            validateMessageFormat(key, baseMessage);
            validateMessageFormat(key, chineseMessage);
        }
    }

    public void testDefaultGeneratedTemplatesRemainByteStableInBothLocales() throws Exception {
        Properties base = load(BASE_BUNDLE);
        Properties simplifiedChinese = load(SIMPLIFIED_CHINESE_BUNDLE);

        assertEquals("Spring default template changed", "e85bc253e77e7a877633a22598f75f25f093afca99a5e4f9b899746069a5a645",
                sha256(base.getProperty("template.spring.init")));
        assertEquals("Dubbo default template changed", "d8b1833ee33f630c826237b4e68e410d86cf3ffbf03036b83f692eefa4f60f12",
                sha256(base.getProperty("template.dubbo.init")));
        for (String key : GENERATED_TEMPLATE_KEYS) {
            assertEquals("Generated template must not vary by IDE locale: " + key,
                    base.getProperty(key), simplifiedChinese.getProperty(key));
        }
    }

    public void testGenerationRuleHelpUsesReadableSectionsInBothLocales() throws Exception {
        Properties base = load(BASE_BUNDLE);
        Properties simplifiedChinese = load(SIMPLIFIED_CHINESE_BUNDLE);
        for (String key : MULTILINE_RULE_HELP_KEYS) {
            assertReadableRuleHelp(key, base.getProperty(key));
            assertReadableRuleHelp(key, simplifiedChinese.getProperty(key));
        }
        for (String key : Set.of("settings.rules.required.fixed")) {
            assertTrue("Required rules should remain a separate paragraph in English",
                    !base.getProperty(key).contains("\n") && !base.getProperty(key).isBlank());
            assertTrue("Required rules should remain a separate paragraph in zh_CN",
                    !simplifiedChinese.getProperty(key).contains("\n") && !simplifiedChinese.getProperty(key).isBlank());
        }
    }

    private static void assertReadableRuleHelp(String key, String message) {
        assertNotNull("Missing multiline rule help " + key, message);
        assertTrue("Rule help should use explicit numbered lines: " + key,
                message.contains("\n1."));
        assertTrue("Rule help should contain more than one line: " + key,
                message.contains("\n"));
        if (key.endsWith(".order")) {
            assertTrue("Rule help should separate sections with a blank line: " + key,
                    message.contains("\n\n"));
        }
    }

    private static Properties load(String resourceName) throws IOException {
        ClassLoader classLoader = PluginLocalizationTest.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new AssertionError("Missing localization resource " + resourceName);
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return properties;
        }
    }

    private static Set<Integer> argumentIndexes(String message) {
        Set<Integer> indexes = new TreeSet<>();
        Matcher matcher = ARGUMENT_INDEX.matcher(message);
        while (matcher.find()) {
            indexes.add(Integer.parseInt(matcher.group(1)));
        }
        return indexes;
    }

    private static void validateMessageFormat(String key, String message) {
        Set<Integer> expectedArguments = argumentIndexes(message);
        if (expectedArguments.isEmpty()) {
            return;
        }
        try {
            MessageFormat format = new MessageFormat(message, Locale.ROOT);
            int maxIndex = expectedArguments.stream().mapToInt(Integer::intValue).max().orElse(0);
            Object[] sentinels = new Object[maxIndex + 1];
            for (int index = 0; index < sentinels.length; index++) {
                sentinels[index] = "DOCVIEW_ARG_" + index + "_SENTINEL";
            }
            String formatted = format.format(sentinels);
            for (int index : expectedArguments) {
                assertTrue("MessageFormat did not substitute argument {" + index + "} for " + key
                                + "; output was: " + formatted,
                        formatted.contains((String) sentinels[index]));
            }
        } catch (IllegalArgumentException error) {
            throw new AssertionError("Invalid MessageFormat pattern for " + key, error);
        }
    }

    private static boolean containsCjk(String text) {
        return text.codePoints().anyMatch(codePoint -> {
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            return script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL;
        });
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte part : digest) {
                hex.append(String.format("%02x", part));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
