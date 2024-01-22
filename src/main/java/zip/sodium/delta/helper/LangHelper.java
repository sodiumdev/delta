package zip.sodium.delta.helper;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;

import java.text.MessageFormat;
import java.util.Locale;

public final class LangHelper {
    private LangHelper() {}

    private static final TranslationRegistry REGISTRY = TranslationRegistry.create(Key.key("delta"));

    static {
        GlobalTranslator.translator().addSource(REGISTRY);
    }

    public static void register(Locale locale, String key, String value) {
        REGISTRY.register(key, locale, new MessageFormat(value));
    }
}
