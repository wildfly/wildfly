/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Verifies that the EJB access-log feature, i.e. its management resource and its XML namespace, is not available when
 * running at {@link Stability#DEFAULT} stability, but is available at {@link Stability#COMMUNITY} and
 * {@link Stability#PREVIEW}.
 */
@RunWith(Parameterized.class)
public class Ejb3AccessLogStabilityTestCase extends AbstractSubsystemTest {

    private static final PathAddress ACCESS_LOG_ADDRESS = PathAddress.pathAddress(EJB3Extension.SUBSYSTEM_PATH).append(PathElement.pathElement("service", "access-log"));
    private static final String DEFAULT_NAMESPACE = "urn:jboss:domain:ejb3:11.0";
    private static final String COMMUNITY_NAMESPACE = "urn:jboss:domain:ejb3:community:12.0";

    @Parameters(name = "{0}")
    public static List<Stability> parameters() {
        return List.of(Stability.DEFAULT, Stability.COMMUNITY, Stability.PREVIEW);
    }

    private final Stability stability;

    public Ejb3AccessLogStabilityTestCase(Stability stability) {
        super(EJB3Extension.SUBSYSTEM_NAME, new EJB3Extension(), stability);
        this.stability = stability;
    }

    private boolean accessLogEnabled() {
        return this.stability.enables(AccessLogResourceDefinition.STABILITY);
    }

    private AdditionalInitialization initialization() {
        return new AdditionalInitialization.ManagementAdditionalInitialization(this.stability);
    }

    @Test
    public void testResourceAvailability() throws Exception {
        // A configuration which does not use the access-log must always be accepted
        final KernelServices services = createKernelServicesBuilder(initialization()).setSubsystemXml(readResource("subsystem11.xml")).build();
        Assert.assertTrue("Subsystem boot failed: " + services.getBootError(), services.isSuccessfulBoot());

        final ModelNode response = services.executeOperation(Util.createAddOperation(ACCESS_LOG_ADDRESS));
        if (accessLogEnabled()) {
            assertEquals("access-log must be addable at " + this.stability + ": " + response, "success", response.get("outcome").asString());
            assertTrue(services.readWholeModel().get("subsystem", "ejb3", "service").hasDefined("access-log"));
        } else {
            assertEquals("access-log must not be addable at " + this.stability + ": " + response, "failed", response.get("outcome").asString());
            assertFalse(services.readWholeModel().get("subsystem", "ejb3", "service").hasDefined("access-log"));
        }
    }

    @Test
    public void testCommunityNamespaceAvailability() throws Exception {
        final String xml = readResource("subsystem-access-log-minimal.xml");
        assertTrue(xml.contains(COMMUNITY_NAMESPACE));
        if (accessLogEnabled()) {
            final KernelServices services = createKernelServicesBuilder(initialization()).setSubsystemXml(xml).build();
            Assert.assertTrue("Subsystem boot failed: " + services.getBootError(), services.isSuccessfulBoot());
            assertTrue(services.readWholeModel().get("subsystem", "ejb3", "service").hasDefined("access-log"));
        } else {
            try {
                createKernelServicesBuilder(initialization()).setSubsystemXml(xml);
                Assert.fail("The community namespace must not be accepted at " + this.stability);
            } catch (XMLStreamException expected) {
                // the namespace is unknown to the parser at this stability level
            }
        }
    }

    @Test
    public void testPersistedNamespace() throws Exception {
        if (!accessLogEnabled()) {
            return;
        }
        // Without access-log the default stability namespace must keep being used, so that existing configurations are not rewritten
        KernelServices services = createKernelServicesBuilder(initialization()).setSubsystemXml(readResource("subsystem11.xml")).build();
        Assert.assertTrue("Subsystem boot failed: " + services.getBootError(), services.isSuccessfulBoot());
        String persisted = services.getPersistedSubsystemXml();
        assertTrue("Unexpected namespace in persisted XML: " + persisted, persisted.contains(DEFAULT_NAMESPACE));
        assertFalse("Community namespace must not be persisted without access-log: " + persisted, persisted.contains(COMMUNITY_NAMESPACE));

        // With access-log the community namespace is required
        services = createKernelServicesBuilder(initialization()).setSubsystemXml(readResource("subsystem-access-log-minimal.xml")).build();
        Assert.assertTrue("Subsystem boot failed: " + services.getBootError(), services.isSuccessfulBoot());
        persisted = services.getPersistedSubsystemXml();
        assertTrue("Community namespace expected in persisted XML: " + persisted, persisted.contains(COMMUNITY_NAMESPACE));
        assertTrue("access-log element expected in persisted XML: " + persisted, persisted.contains("<access-log"));

        // Removing the access-log reverts to the default stability namespace
        final ModelNode response = services.executeOperation(Util.createRemoveOperation(ACCESS_LOG_ADDRESS));
        assertEquals("Removing access-log failed: " + response, "success", response.get("outcome").asString());
        persisted = services.getPersistedSubsystemXml();
        assertTrue("Default stability namespace expected once access-log is removed: " + persisted, persisted.contains(DEFAULT_NAMESPACE));
        assertFalse("Community namespace must not be persisted once access-log is removed: " + persisted, persisted.contains(COMMUNITY_NAMESPACE));
    }
}
