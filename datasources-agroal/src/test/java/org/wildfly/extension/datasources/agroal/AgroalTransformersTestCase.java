/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_7_2_0;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.datasources.agroal.AgroalTransformers.AGROAL_1_0;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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

public class AgroalTransformersTestCase extends AbstractSubsystemBaseTest {
    public AgroalTransformersTestCase() {
        super(AgroalExtension.SUBSYSTEM_NAME, new AgroalExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("agroal_2_0-transformers.xml");
    }

    protected String getSubsystemXml(final String subsystemFile) throws IOException {
        return readResource(subsystemFile);
    }

    @Test
    public void testTransformerEAP720() throws Exception {
        testTransformation(EAP_7_2_0, AGROAL_1_0);
    }

    @Test
    public void testRejectingTransformersEAP_7_2_0() throws Exception {
        PathAddress address = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, AgroalExtension.SUBSYSTEM_NAME);
        testRejectingTransformers(EAP_7_2_0, AGROAL_1_0, "agroal_2_0-reject.xml", new FailedOperationTransformationConfig()
                .addFailedAttribute(address.append(PathElement.pathElement("datasource", "datasource1")),
                        FailedOperationTransformationConfig.REJECTED_RESOURCE)
                .addFailedAttribute(address.append(PathElement.pathElement("xa-datasource", "datasource2")),
                        FailedOperationTransformationConfig.REJECTED_RESOURCE)
        );
    }

    private KernelServices buildKernelServices(ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        return this.buildKernelServices(this.getSubsystemXml(), controllerVersion, version, mavenResourceURLs);
    }

    private KernelServices buildKernelServices(String xml, ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(xml);

        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();
        assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());
        return services;
    }

    private static String getGav(final ModelTestControllerVersion controller){
        if (controller.isEap()) {
            return "org.jboss.eap:wildfly-datasources-agroal:" + controller.getMavenGavVersion();
        }
        return "org.wildfly:wildfly-datasources-agroal:" + controller.getMavenGavVersion();
    }

    private void testTransformation(final ModelTestControllerVersion controller, ModelVersion version) throws Exception {
        KernelServices services = this.buildKernelServices(controller, version, getGav(controller));
        checkSubsystemModelTransformation(services, version, null, false);
        ModelNode transformed = services.readTransformedModel(version);
        assertNotNull(transformed);
    }

    private void testRejectingTransformers(ModelTestControllerVersion controllerVersion, ModelVersion version, final String subsystemXmlFile, final FailedOperationTransformationConfig config) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, version)
                .addMavenResourceURL(getGav(controllerVersion))
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(version).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource(subsystemXmlFile);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version, ops, config);
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
    public void testSchema() throws Exception {
        //
    }
}
