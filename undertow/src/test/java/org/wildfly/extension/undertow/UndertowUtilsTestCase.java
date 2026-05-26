/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link UndertowUtils}.
 *
 * @author Rafael Rosa
 */
public class UndertowUtilsTestCase {

    @Test
    public void testNormalizePathWithoutLeadingSlash() {
        assertEquals("/wildfly-services", UndertowUtils.normalizePath("wildfly-services"));
    }

    @Test
    public void testNormalizePathWithLeadingSlash() {
        assertEquals("/wildfly-services", UndertowUtils.normalizePath("/wildfly-services"));
    }

    @Test
    public void testNormalizePathEmpty() {
        assertEquals("/", UndertowUtils.normalizePath(""));
    }

    @Test
    public void testNormalizePathNull() {
        assertEquals(null, UndertowUtils.normalizePath(null));
    }

    @Test
    public void testNormalizePathRoot() {
        assertEquals("/", UndertowUtils.normalizePath("/"));
    }

    @Test
    public void testNormalizePathComplex() {
        assertEquals("/api/v1/services", UndertowUtils.normalizePath("api/v1/services"));
        assertEquals("/api/v1/services", UndertowUtils.normalizePath("/api/v1/services"));
    }
}
