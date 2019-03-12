/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertTrue;

/**
 * Test transformation between model versions
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class AgroalTransformersTestCase extends AbstractSubsystemBaseTest {

    public AgroalTransformersTestCase() {
        super(AgroalExtension.SUBSYSTEM_NAME, new AgroalExtension());
    }

    private static String getExtensionMavenGAV(ModelTestControllerVersion version) {
        if (version.isEap()) {
            return "org.jboss.eap:wildfly-datasources-agroal:" + version.getMavenGavVersion();
        }
        return "org.wildfly:wildfly-datasources-agroal:" + version.getMavenGavVersion();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        // Create a AdditionalInitialization.MANAGEMENT variant that has all the external capabilities used by the various configs used in this test class
        return AdditionalInitialization.withCapabilities(
                AbstractDataSourceDefinition.AUTHENTICATION_CONTEXT_CAPABILITY + ".secure-context",
                CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".test-store"
        );
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("agroal_2_0-full.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-agroal_2_0.xsd";
    }

    @Test
    public void testTransformers_2_0_0() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_2_0, AgroalExtension.VERSION_1_0_0, "agroal_2_0-transform.xml");
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion agroalVersion, String xmlResource) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, agroalVersion)
               .addMavenResourceURL(getExtensionMavenGAV(controllerVersion))
               .skipReverseControllerCheck()
               .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(agroalVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource(xmlResource);
        System.out.println("ops = " + ops);
        PathAddress subsystemAddress = PathAddress.pathAddress(SUBSYSTEM, AgroalExtension.SUBSYSTEM_NAME);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, agroalVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress, new FailedOperationTransformationConfig.NewAttributesConfig(
                        XADataSourceDefinition.RECOVERY_USERNAME_ATTRIBUTE,
                        XADataSourceDefinition.RECOVERY_PASSWORD_ATTRIBUTE,
                        XADataSourceDefinition.RECOVERY_AUTHENTICATION_CONTEXT,
                        XADataSourceDefinition.RECOVERY_CREDENTIAL_REFERENCE))
                .addFailedAttribute(subsystemAddress, new CorrectToFalse(
                        XADataSourceDefinition.RECOVERY))
        );
    }

    // --- //

    private static class CorrectToFalse extends FailedOperationTransformationConfig.NewAttributesConfig {

        public CorrectToFalse(AttributeDefinition... defs) {
            super(convert(defs));
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !ModelNode.FALSE.equals(attribute);
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return ModelNode.FALSE;
        }

    }

}
