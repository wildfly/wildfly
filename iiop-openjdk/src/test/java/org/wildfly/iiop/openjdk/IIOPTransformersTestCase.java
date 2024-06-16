/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.junit.Assert;
import org.junit.Test;


public class IIOPTransformersTestCase extends AbstractSubsystemBaseTest {

    public IIOPTransformersTestCase() {
        super(IIOPExtension.SUBSYSTEM_NAME, new IIOPExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-3.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-iiop-openjdk_3_0.xsd";
    }

    @Test
    public void testTransformers() throws Exception {
        String subsystemXml = "subsystem-iiop-transform.xml";
        ModelVersion modelVersion = IIOPExtension.CURRENT_MODEL_VERSION;
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXmlResource(subsystemXml);
        KernelServices mainServices = initialKernelServices(builder, ModelTestControllerVersion.EAP_8_0_0);

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    private KernelServices initialKernelServices(KernelServicesBuilder builder, ModelTestControllerVersion controllerVersion) throws Exception {
        String mavenGroupId = controllerVersion.getMavenGroupId();
        ModelVersion modelVersion = IIOPExtension.VERSION_3_0;
        String artifactId = "wildfly-iiop-openjdk";

        LegacyKernelServicesInitializer initializer = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, modelVersion);

        initializer.addMavenResourceURL(mavenGroupId + ":" + artifactId + ":" + controllerVersion.getMavenGavVersion())
//                .addMavenResourceURL("org.jboss.spec.javax.resource:jboss-connector-api_1.7_spec:2.0.0.Final-redhat-00001")
                .setExtensionClassName("org.wildfly.iiop.openjdk.IIOPExtension")
                .skipReverseControllerCheck()
                .dontPersistXml();
//                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        return mainServices;
    }
}