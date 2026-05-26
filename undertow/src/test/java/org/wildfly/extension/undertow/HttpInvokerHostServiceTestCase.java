/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link HttpInvokerHostService}.
 *
 * @author Rafael Rosa
 */
public class HttpInvokerHostServiceTestCase {

    @Test
    public void testNormalizePathWithoutLeadingSlash() {
        assertEquals("/wildfly-services", HttpInvokerHostService.normalizePath("wildfly-services"));
    }

    @Test
    public void testNormalizePathWithLeadingSlash() {
        assertEquals("/wildfly-services", HttpInvokerHostService.normalizePath("/wildfly-services"));
    }

    @Test
    public void testNormalizePathEmpty() {
        assertEquals("/", HttpInvokerHostService.normalizePath(""));
    }

    @Test
    public void testNormalizePathNull() {
        assertEquals(null, HttpInvokerHostService.normalizePath(null));
    }

    @Test
    public void testNormalizePathRoot() {
        assertEquals("/", HttpInvokerHostService.normalizePath("/"));
    }

    @Test
    public void testNormalizePathComplex() {
        assertEquals("/api/v1/services", HttpInvokerHostService.normalizePath("api/v1/services"));
        assertEquals("/api/v1/services", HttpInvokerHostService.normalizePath("/api/v1/services"));
    }
}
