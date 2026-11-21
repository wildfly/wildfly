/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.datasources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.connector._private.Capabilities;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Verifies that the datasource {@code validation-timeout-seconds} attribute, and the namespace introducing it, are
 * only available when the server runs at community stability or a less stable level.
 */
@RunWith(Parameterized.class)
public class DatasourcesValidationTimeoutStabilityTestCase extends AbstractSubsystemTest {

    private static final String COMMUNITY_NAMESPACE = "urn:jboss:domain:datasources:community:8.0";
    private static final String DEFAULT_NAMESPACE = "urn:jboss:domain:datasources:7.2";

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
                { Stability.DEFAULT, false },
                { Stability.COMMUNITY, true },
                { Stability.PREVIEW, true },
        });
    }

    private final Stability stability;
    private final boolean enabled;

    public DatasourcesValidationTimeoutStabilityTestCase(Stability stability, boolean enabled) {
        super(DataSourcesExtension.SUBSYSTEM_NAME, new DataSourcesExtension(), stability);
        this.stability = stability;
        this.enabled = enabled;
    }

    private AdditionalInitialization additionalInitialization() {
        return AdditionalInitialization.withCapabilities(this.stability,
                Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY + ".DsAuthCtxt",
                CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".test-store",
                NamingService.CAPABILITY_NAME,
                ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME);
    }

    private static String subsystemXml(String namespace, String timeout) {
        return "<subsystem xmlns=\"" + namespace + "\"><datasources>"
                + "<datasource jndi-name=\"java:jboss/datasources/ExampleDS\" pool-name=\"ExampleDS\" use-java-context=\"true\">"
                + "<connection-url>jdbc:h2:mem:test</connection-url><driver>h2</driver>"
                + timeout
                + "</datasource>"
                + "<drivers><driver name=\"h2\" module=\"com.h2database.h2\"><xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class></driver></drivers>"
                + "</datasources></subsystem>";
    }

    @Test
    public void testAttributeAvailability() throws Exception {
        KernelServices services = createKernelServicesBuilder(additionalInitialization())
                .setSubsystemXml(subsystemXml(DEFAULT_NAMESPACE, "")).build();
        assertTrue(services.isSuccessfulBoot());

        PathAddress address = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM)
                .append(DataSourceDefinition.PATH_DATASOURCE.getKey(), "ExampleDS");
        ModelNode write = Util.getWriteAttributeOperation(address, Constants.VALIDATION_TIMEOUT_SECONDS.getName(), 5);
        ModelNode result = services.executeOperation(write);
        assertEquals(this.enabled, "success".equals(result.get("outcome").asString()));
    }

    @Test
    public void testNamespaceParsing() throws Exception {
        String xml = subsystemXml(COMMUNITY_NAMESPACE, "<timeout><validation-timeout-seconds>5</validation-timeout-seconds></timeout>");
        if (this.enabled) {
            KernelServices services = createKernelServicesBuilder(additionalInitialization()).setSubsystemXml(xml).build();
            assertTrue(services.isSuccessfulBoot());
            assertTrue(services.getPersistedSubsystemXml().contains(COMMUNITY_NAMESPACE));
        } else {
            try {
                KernelServices services = createKernelServicesBuilder(additionalInitialization()).setSubsystemXml(xml).build();
                assertFalse(services.isSuccessfulBoot());
            } catch (Exception | AssertionError expected) {
                // the community namespace can not be parsed
            }
        }
    }

    @Test
    public void testPersistedNamespace() throws Exception {
        KernelServices services = createKernelServicesBuilder(additionalInitialization())
                .setSubsystemXml(subsystemXml(DEFAULT_NAMESPACE, "")).build();
        assertTrue(services.isSuccessfulBoot());
        String persisted = services.getPersistedSubsystemXml();
        assertEquals(persisted, this.enabled, persisted.contains(COMMUNITY_NAMESPACE));
        assertEquals(persisted, !this.enabled, persisted.contains(DEFAULT_NAMESPACE));
    }
}
