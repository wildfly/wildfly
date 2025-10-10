/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for {@link ContainerConfig}.
 * <p>
 * Verifies that container image configurations can be loaded from a properties file.
 */
public class ContainerConfigTest {

    /**
     * Tests that container configuration can be loaded from a properties file.
     * <p>
     * This test:
     * <ul>
     *   <li>Sets the properties file location via system property</li>
     *   <li>Retrieves the Artemis container image configuration</li>
     *   <li>Verifies the image is not null and matches the expected value from the properties file</li>
     * </ul>
     */
    @Test
    public void testContainerConfig() {
        System.setProperty(ContainerConfig.PROPERTIES_FILE_PROPERTY_NAME,
                Thread.currentThread().getContextClassLoader().getResource("testsuite-config.properties").getPath());
        String artemisImage = ContainerConfig.ARTEMIS_BROKER.getImage();
        assertNotNull(artemisImage, "Artemis image not found in configuration!");
        assertEquals("quay.io/arkmq-org/activemq-artemis-broker:artemis.9999999", artemisImage);
    }
}
