/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.xts;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.xts.XTSSubsystemDefinition.ASYNC_REGISTRATION;
import static org.jboss.as.xts.XTSSubsystemDefinition.DEFAULT_CONTEXT_PROPAGATION;
import static org.jboss.as.xts.XTSSubsystemDefinition.HOST_NAME;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class XTSSubsystemTestCase extends AbstractSubsystemBaseTest {
    // These are named for the last version supporting a given model version
    private static final ModelVersion EAP6_4_0 = ModelVersion.create(1, 0, 0);
    private static final ModelVersion EAP7_1_0 = ModelVersion.create(2, 0, 0);
    private static final ModelVersion EAP7_2_0 = ModelVersion.create(3, 0, 0);

    public XTSSubsystemTestCase() {
        super(XTSExtension.SUBSYSTEM_NAME, new XTSExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource(String.format("subsystem-%s.xml", EAP7_2_0));
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-xts_3_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
            "/subsystem-templates/xts.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Test
    public void testTransformersEAP_6_4_0() throws Exception {
        // EAP 6.4.0 still uses 1.0.0 XTS subsystem version
        testTransformation(ModelTestControllerVersion.EAP_6_4_0, EAP6_4_0, "jboss-as-xts");
    }

    @Test
    public void testTransformersEAP_7_0_0() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_7_0_0, EAP7_1_0, "wildfly-xts");
    }

    @Test
    public void testTransformersEAP_7_1_0() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_7_1_0, EAP7_1_0, "wildfly-xts");
    }


    @Test
    public void testTransformersFailedEAP_6_4_0() throws Exception {
        testRejectTransformation(ModelTestControllerVersion.EAP_6_4_0, EAP6_4_0, "jboss-as-xts");
    }


    @Test
    public void testTransformersFailedEAP_7_0_0() throws Exception {
        testRejectTransformation(ModelTestControllerVersion.EAP_7_0_0, EAP7_1_0, "wildfly-xts");
    }

    @Test
    public void testTransformersFailedEAP_7_1_0() throws Exception {
        testRejectTransformation(ModelTestControllerVersion.EAP_7_1_0, EAP7_1_0, "wildfly-xts");
    }


    private void testTransformation(ModelTestControllerVersion controllerVersion, ModelVersion version, String artifact) throws Exception {
        String subsystemXml = readResource(String.format("subsystem-%s.xml", version));
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
            .setSubsystemXml(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
            .addMavenResourceURL(String.format("%s:%s:%s",
                controllerVersion.getMavenGroupId(), artifact, controllerVersion.getMavenGavVersion()))
            .setExtensionClassName(XTSExtension.class.getName())
            .configureReverseControllerCheck(createAdditionalInitialization(), null)
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, version);
    }

    private void testRejectTransformation(ModelTestControllerVersion controllerVersion, ModelVersion version, String artifact) throws Exception {
        FailedOperationTransformationConfig config = createFailedConfig(version);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
            .addMavenResourceURL(String.format("%s:%s:%s", controllerVersion.getMavenGroupId(), artifact, controllerVersion.getMavenGavVersion()))
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("subsystem-3.0.0.xml"); // the latest version
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version, ops, config);
    }

    private FailedOperationTransformationConfig createFailedConfig(ModelVersion version) {

        ChainedConfig chainedConfig;
        if (version == EAP6_4_0) {
            final AttributesPathAddressConfig hostConfig = new AttributesPathAddressConfig(HOST_NAME.getName()) {
                private static final String DEFAULT_HOST = "default-host";
                @Override
                protected boolean isAttributeWritable(String attributeName) {
                    return true;
                }

                @Override
                protected boolean checkValue(String attrName, ModelNode attribute, boolean isGeneratedWriteAttribute) {
                    // If it is not equal to 'default-host' we will change it
                    return !attribute.asString().equals(DEFAULT_HOST);
                }

                @Override
                protected ModelNode correctValue(ModelNode toResolve, boolean isGeneratedWriteAttribute) {
                    return new ModelNode(DEFAULT_HOST);
                }
            };
            chainedConfig = ChainedConfig.createBuilder(ASYNC_REGISTRATION, DEFAULT_CONTEXT_PROPAGATION, HOST_NAME)
                    .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(ASYNC_REGISTRATION, DEFAULT_CONTEXT_PROPAGATION))
                    .addConfig(hostConfig)
                    .build();

        } else if (version == EAP7_1_0) {
            chainedConfig = ChainedConfig.createBuilder(ASYNC_REGISTRATION)
                    .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(ASYNC_REGISTRATION))
                    .build();

        } else {
            throw new IllegalStateException();
        }

        return new FailedOperationTransformationConfig()
                .addFailedAttribute(PathAddress.pathAddress(SUBSYSTEM, XTSExtension.SUBSYSTEM_NAME), chainedConfig);
    }
}
