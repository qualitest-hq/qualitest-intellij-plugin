package com.qualitest.scan.resolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JavaDocResolverTest {

    @Test
    public void sanitizeGroupTagValue_removesTrailingMarginStar() {
        assertEquals("管理端.商城.商品SKU", JavaDocResolver.sanitizeGroupTagValue("管理端.商城.商品SKU *"));
    }

    @Test
    public void sanitizeGroupTagValue_removesNextLineMarginStar() {
        assertEquals("管理端.商城.商品SKU", JavaDocResolver.sanitizeGroupTagValue("管理端.商城.商品SKU\n *"));
    }

    @Test
    public void sanitizeGroupTagValue_removesClosingCommentMarker() {
        assertEquals("管理端.商城.商品SKU", JavaDocResolver.sanitizeGroupTagValue("管理端.商城.商品SKU */"));
    }

    @Test
    public void sanitizeGroupTagValue_stopsAtNextBlockTag() {
        assertEquals("管理端.商城.商品SKU", JavaDocResolver.sanitizeGroupTagValue("管理端.商城.商品SKU\n *\n * @author demo"));
    }

    @Test
    public void sanitizeGroupTagValue_returnsNullForBlank() {
        assertNull(JavaDocResolver.sanitizeGroupTagValue("   "));
        assertNull(JavaDocResolver.sanitizeGroupTagValue(null));
    }

    @Test
    public void sanitizeDocText_removesTrailingMarginStar() {
        assertEquals("管理端.商城.商品SKU", JavaDocResolver.sanitizeDocText("管理端.商城.商品SKU *"));
    }

    @Test
    public void sanitizeDocText_removesClosingCommentMarker() {
        assertEquals("管理端.商城.商品SKU", JavaDocResolver.sanitizeDocText("管理端.商城.商品SKU */"));
    }

    @Test
    public void tagNameFromFormat_parsesDefaultGroupTag() {
        assertEquals("api.group", JavaDocResolver.tagNameFromFormat("api.group {group}"));
        assertEquals("eo.groupName", JavaDocResolver.tagNameFromFormat("eo.groupName"));
    }
}
