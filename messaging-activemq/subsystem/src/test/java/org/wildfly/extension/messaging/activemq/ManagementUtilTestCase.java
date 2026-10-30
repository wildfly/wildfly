/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.apache.activemq.artemis.utils.XmlProvider.ARTEMIS_DISABLE_XXE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jboss.as.controller.OperationFailedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ManagementUtilTestCase {

    @Before
    @After
    public void clearProperty() {
        System.clearProperty(ARTEMIS_DISABLE_XXE_PROPERTY);
    }

    @Test
    public void testPropertyAbsent_setsTrue() throws OperationFailedException {
        ManagementUtil.configureXxeSystemProperty();
        assertEquals("true", System.getProperty(ARTEMIS_DISABLE_XXE_PROPERTY));
    }

    @Test
    public void testPropertyAlreadyTrue_accepted() throws OperationFailedException {
        System.setProperty(ARTEMIS_DISABLE_XXE_PROPERTY, "true");
        ManagementUtil.configureXxeSystemProperty();
        assertEquals("true", System.getProperty(ARTEMIS_DISABLE_XXE_PROPERTY));
    }

    @Test
    public void testPropertyAlreadyFalse_accepted() throws OperationFailedException {
        System.setProperty(ARTEMIS_DISABLE_XXE_PROPERTY, "false");
        ManagementUtil.configureXxeSystemProperty();
        assertEquals("false", System.getProperty(ARTEMIS_DISABLE_XXE_PROPERTY));
    }

    @Test
    public void testPropertyInvalidValue_throws() {
        System.setProperty(ARTEMIS_DISABLE_XXE_PROPERTY, "invalid");
        try {
            ManagementUtil.configureXxeSystemProperty();
            fail("Expected OperationFailedException for invalid property value");
        } catch (OperationFailedException expected) {
            // expected
        }
    }

    @Test
    public void testPropertyEmptyValue_throws() {
        System.setProperty(ARTEMIS_DISABLE_XXE_PROPERTY, "");
        try {
            ManagementUtil.configureXxeSystemProperty();
            fail("Expected OperationFailedException for empty property value");
        } catch (OperationFailedException expected) {
            // expected
        }
    }
}
