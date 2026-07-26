package com.qualitest.javadoc;

import com.qualitest.QualiTestConstants;
import com.qualitest.config.QualiTestSettings;
import com.qualitest.scan.resolver.JavaDocResolver;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.javadoc.CustomJavadocTagProvider;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocTagValue;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将设置中的「分组」JavaDoc 标签登记为合法标签，避免 unknown tag 类检查红字（如 {@code @eo.groupName}）。
 */
public class QualiTestCustomJavadocTagProvider implements CustomJavadocTagProvider {

    @Override
    public @NotNull List<JavadocTagInfo> getSupportedTags() {
        Set<String> names = new LinkedHashSet<>();
        names.add(JavaDocResolver.tagNameFromFormat(QualiTestConstants.DEFAULT_GROUP_TAG));
        QualiTestSettings settings = QualiTestSettings.getInstance();
        if (settings != null) {
            names.add(JavaDocResolver.tagNameFromFormat(settings.getGroupTag()));
        }
        List<JavadocTagInfo> out = new ArrayList<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            out.add(new GroupTagJavadocInfo(name));
        }
        return out;
    }

    private static final class GroupTagJavadocInfo implements JavadocTagInfo {
        private final String name;

        GroupTagJavadocInfo(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isInline() {
            return false;
        }

        @Override
        public boolean isValidInContext(PsiElement element) {
            return element instanceof PsiMethod || element instanceof PsiClass;
        }

        @Override
        public @Nullable @Nls String checkTagValue(PsiDocTagValue value) {
            return null;
        }

        @Override
        public @Nullable PsiReference getReference(PsiDocTagValue value) {
            return null;
        }
    }
}
