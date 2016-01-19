/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jacorb;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JacORBTransformersTestCase extends AbstractSubsystemTest {

    public JacORBTransformersTestCase() {
        super(JacORBExtension.SUBSYSTEM_NAME, new JacORBExtension());
    }

    //eap62=1.3
    //eap63=1.4
    //eap64=1.4

    @Test
    public void testTransformersEAP620() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1, 3));
    }

    @Test
    public void testTransformersEAP630() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_6_3_0, ModelVersion.create(1, 4));
    }

    @Test
    public void testTransformersEAP640() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_6_4_0, ModelVersion.create(1, 4));
    }


    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC)
                .setSubsystemXmlResource("subsystem-transform.xml");

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-jacorb:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.ADMIN_ONLY_HC, null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                //modelNode.get(JacORBSubsystemDefinitions.PERSISTENT_SERVER_ID.getName()).set(JacORBSubsystemDefinitions.PERSISTENT_SERVER_ID.getDefaultValue());
                modelNode.get(JacORBSubsystemDefinitions.INTEROP_CHUNK_RMI_VALUETYPES.getName()).set(JacORBSubsystemDefinitions.INTEROP_CHUNK_RMI_VALUETYPES.getDefaultValue());
                return modelNode;
            }
        });
    }

    @Test
    public void testTransformersSecurityIdentityEAP620() throws Exception {
        testTransformersSecurityIdentity(ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1, 3, 0));
    }

    @Test
    public void testTransformersSecurityIdentityEAP630() throws Exception {
        testTransformersSecurityIdentity(ModelTestControllerVersion.EAP_6_3_0, ModelVersion.create(1, 3, 0));
    }

    @Test
    public void testTransformersSecurityIdentityEAP640() throws Exception {
        testTransformersSecurityIdentity(ModelTestControllerVersion.EAP_6_4_0, ModelVersion.create(1, 4, 0));
    }


    private void testTransformersSecurityIdentity(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC)
                .setSubsystemXml(readResource("subsystem-security-identity.xml"));

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-jacorb:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.ADMIN_ONLY_HC, null);

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        ModelNode transformed = checkSubsystemModelTransformation(mainServices, modelVersion).get(SUBSYSTEM, JacORBExtension.SUBSYSTEM_NAME);
        Assert.assertEquals("identity", transformed.get("security").asString());
        List<ModelNode> properties = transformed.get(ModelDescriptionConstants.PROPERTIES).asList();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("some_value", properties.get(0).get("some_property").asString());
    }

    @Test
    public void testTransformersSecurityClientEAP620() throws Exception {
        testTransformersSecurityClient(ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1, 3, 0));
    }

    @Test
    public void testTransformersSecurityClientEAP630() throws Exception {
        testTransformersSecurityClient(ModelTestControllerVersion.EAP_6_3_0, ModelVersion.create(1, 3, 0));
    }

    @Test
    public void testTransformersSecurityClientEAP640() throws Exception {
        testTransformersSecurityClient(ModelTestControllerVersion.EAP_6_4_0, ModelVersion.create(1, 4, 0));
    }

    private void testTransformersSecurityClient(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {

        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-jacorb:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.ADMIN_ONLY_HC, null);

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, builder.parseXmlResource("subsystem-security-client.xml"), config);
        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Test
    public void testTransformersIORSettingsEAP620() throws Exception {
        testTransformersIORSettings(ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1, 3, 0));
    }

    @Test
    public void testTransformersIORSettingsEAP630() throws Exception {
        testTransformersIORSettings(ModelTestControllerVersion.EAP_6_3_0, ModelVersion.create(1, 3, 0));
    }

    private void testTransformersIORSettings(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {

        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-jacorb:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.ADMIN_ONLY_HC, null);

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion,
                builder.parseXmlResource("subsystem-ior-settings.xml"),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(JacORBSubsystemResource.INSTANCE.getPathElement(),
                                IORSettingsDefinition.INSTANCE.getPathElement()),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(JacORBSubsystemResource.INSTANCE.getPathElement(),
                                IORSettingsDefinition.INSTANCE.getPathElement(),
                                IORTransportConfigDefinition.INSTANCE.getPathElement()),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(JacORBSubsystemResource.INSTANCE.getPathElement(),
                                IORSettingsDefinition.INSTANCE.getPathElement(),
                                IORASContextDefinition.INSTANCE.getPathElement()),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(JacORBSubsystemResource.INSTANCE.getPathElement(),
                                IORSettingsDefinition.INSTANCE.getPathElement(),
                                IORSASContextDefinition.INSTANCE.getPathElement()),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE));
    }


}