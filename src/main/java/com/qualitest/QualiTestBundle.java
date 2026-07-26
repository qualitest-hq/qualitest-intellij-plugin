package com.qualitest;

import com.intellij.DynamicBundle;
import kotlin.jvm.JvmStatic;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class QualiTestBundle {
    private static final String BUNDLE = "messages.QualiTestBundle";
    private static final DynamicBundle instance = new DynamicBundle(QualiTestBundle.class, BUNDLE);

    private QualiTestBundle() {
    }

    @JvmStatic
    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return instance.getMessage(key, params);
    }

    @JvmStatic
    public static Supplier<@Nls String> lazyMessage(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return instance.getLazyMessage(key, params);
    }
}
