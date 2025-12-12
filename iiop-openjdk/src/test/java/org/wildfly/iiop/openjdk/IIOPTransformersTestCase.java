/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk;

import java.io.IOException;
import java.util.Optional;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
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
        ModelVersion legacyModelVersion = IIOPExtension.VERSION_3_0;
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXmlResource(subsystemXml);
        KernelServices mainServices = initialKernelServices(builder, ModelTestControllerVersion.EAP_8_1_0);

        KernelServices legacyServices = mainServices.getLegacyServices(legacyModelVersion);
        Assert.assertNotNull(legacyServices);
        // Even though the legacy services can't understand the add op for the root resource,
        // we expect a successful boot, because mainServices will discard that add op when our
        // transformer rejects it and not use it to start the legacy services.
        // (NOTE: this is how the transformer test infrastructure works; a real domain controller
        // interacts with a legacy host by sending resources, not be sending ops to execute.)
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // The add operation for the subsystem root resource should have been rejected and discarded by main services
        // and not used to configure the legacy services. So check that the resource is not present on the legacy resource.
        ModelNode readChildrenNames = Util.createEmptyOperation(READ_CHILDREN_NAMES_OPERATION, PathAddress.EMPTY_ADDRESS);
        readChildrenNames.get(CHILD_TYPE, SUBSYSTEM);
        ModelNode response = mainServices.executeOperation(legacyModelVersion, mainServices.transformOperation(IIOPExtension.VERSION_3_0, readChildrenNames));
        if (response.hasDefined(RESULT)) {
            Optional<ModelNode> optional = response.get(RESULT).asList().stream().filter(name -> IIOPExtension.SUBSYSTEM_NAME.equals(name.asString())).findFirst();
            Assert.assertTrue(IIOPExtension.SUBSYSTEM_NAME + " resource should not be present on the legacy controller ", optional.isEmpty());
        }

        // TODO if https://issues.redhat.com/browse/WFCORE-7422-7422 is ever implemented use it validate resource transformation.
        // The above validates add operation transformation.
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