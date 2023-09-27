/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.subsystem.test.AdditionalInitialization.MANAGEMENT;

/**
 * Tests parsing of XML files with all elements and attributes
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class SubsystemFullParsingTestCase extends AbstractSubsystemTest {

    public SubsystemFullParsingTestCase() {
        super(AgroalExtension.SUBSYSTEM_NAME, new AgroalExtension());
    }

    private static AdditionalInitialization createAdditionalInitialization() {
        // Create a AdditionalInitialization.MANAGEMENT variant that has all the external capabilities used by the various configs used in this test class
        return AdditionalInitialization.withCapabilities(
                AbstractDataSourceDefinition.AUTHENTICATION_CONTEXT_CAPABILITY + ".secure-context",
                CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".test-store"
        );
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        parseXmlResource("agroal_2_0-full.xml");
    }

    @SuppressWarnings("SameParameterValue")
    private void parseXmlResource(String xmlResource) throws Exception {
        KernelServicesBuilder kernelBuilder = createKernelServicesBuilder(createAdditionalInitialization());
        KernelServices services = kernelBuilder.build();

        for (ModelNode op : kernelBuilder.parseXmlResource(xmlResource)) {
            services.executeOperation(op);
        }

        // Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(AgroalExtension.SUBSYSTEM_NAME));

        // for debug purposes: System.out.println( model ); System.out.println( services.getPersistedSubsystemXml() );

        ModelNode marshaledModel = createKernelServicesBuilder(MANAGEMENT).setSubsystemXml(services.getPersistedSubsystemXml()).build().readWholeModel();

        Assert.assertEquals(model, marshaledModel);
    }
}
