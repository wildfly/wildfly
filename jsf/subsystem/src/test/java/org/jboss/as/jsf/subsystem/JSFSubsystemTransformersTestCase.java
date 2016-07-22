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
package org.jboss.as.jsf.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * JSF subsystem transformer tests.
 *
 * @author <a href="fjuma@redhat.com">Farah Juma</a>
 */
public class JSFSubsystemTransformersTestCase extends AbstractSubsystemBaseTest {

    private static ModelVersion legacyVersion = ModelVersion.create(1, 0);

    public JSFSubsystemTransformersTestCase() {
        super(JSFExtension.SUBSYSTEM_NAME, new JSFExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/jsf-transformers.xml");
    }

    @Test
    public void testTransformersEAP700() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_0_0, legacyVersion, "/jsf-transformers.xml");
    }

    @Test
    public void testRejectTransformersEAP700() throws Exception {
        doRejectTest(ModelTestControllerVersion.EAP_7_0_0, legacyVersion);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, String subsystemXmlResource) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource(subsystemXmlResource);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL(String.format("%s:wildfly-jsf:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion, null);
    }

    private void doRejectTest(ModelTestControllerVersion controllerVersion, ModelVersion targetVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, targetVersion)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                .addMavenResourceURL(String.format("%s:wildfly-jsf:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(targetVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        List<ModelNode> ops = builder.parseXmlResource("/jsf-transformers-reject.xml");
        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JSFExtension.SUBSYSTEM_NAME));

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, targetVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(JSFResourceDefinition.DISALLOW_DOCTYPE_DECL_ATTR_NAME)
                )
        );
    }
}
