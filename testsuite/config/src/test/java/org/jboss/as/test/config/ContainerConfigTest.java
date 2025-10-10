package org.jboss.as.test.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for verifying container configuration properties.
 * <p>
 * This test class validates that container images used in the test suite
 * are properly configured and accessible through the {@link ContainerConfig} utility.
 * </p>
 */
public class ContainerConfigTest {

    /**
     * Tests that the Artemis container image is properly configured.
     * <p>
     * This test:
     * <ul>
     *   <li>Sets the system property pointing to the configuration file</li>
     *   <li>Retrieves the Artemis image configuration</li>
     *   <li>Verifies the image is not null</li>
     *   <li>Validates the image matches the expected Artemis broker version</li>
     * </ul>
     * </p>
     */
    @Test
    public void testContainerConfig() {
        System.setProperty(PropertiesFileConfigSource.PROPERTIES_FILE_PROPERTY_NAME,
                Thread.currentThread().getContextClassLoader().getResource("testsuite-config.properties").getFile());
        String artemisImage = ContainerConfig.getArtemisImage();
        assertNotNull(artemisImage, "Artemis image not found in configuration!");
        assertEquals("quay.io/arkmq-org/activemq-artemis-broker:artemis.9999999", artemisImage);
    }
}
