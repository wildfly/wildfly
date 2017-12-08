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
package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_SHORT_RUNNING;
import static org.jboss.as.connector.subsystems.jca.JcaDistributedWorkManagerDefinition.DWmParameters.ELYTRON_ENABLED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.spi.ClusteringDefaultRequirement;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="stefano.maestri@redhat.com>Stefano Maestri</a>
 */
public class JcaSubsystemTestCase extends AbstractSubsystemBaseTest {

    public JcaSubsystemTestCase() {
        super(JcaExtension.SUBSYSTEM_NAME, new JcaExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jca.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-jca_5_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/jca.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getName());
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("jca-full.xml");
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("jca-full-expression.xml", "jca-full.xml");
    }

    /** WFLY-2640 and WFLY-8141 */
    @Test
    public void testCCMHandling() throws Exception {
        String xml = readResource("jca-minimal.xml");
        final KernelServices services = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(xml).build();
        assertTrue("Subsystem boot failed!", services.isSuccessfulBoot());
        //Get the model and the persisted xml from the first controller
        final ModelNode model = services.readWholeModel();

        // ccm is present despite not being in xml
        ModelNode ccm = model.get("subsystem", "jca", "cached-connection-manager", "cached-connection-manager");
        assertTrue(ccm.isDefined());
        assertTrue(ccm.hasDefined("install")); // only true because readWholeModel reads defaults

        // Because it exists we can do a write-attribute
        PathAddress ccmAddress = PathAddress.pathAddress("subsystem", "jca").append("cached-connection-manager", "cached-connection-manager");
        ModelNode writeOp = Util.getWriteAttributeOperation(ccmAddress, "install", true);
        services.executeForResult(writeOp);

        ModelNode readOp = Util.getReadAttributeOperation(ccmAddress, "install");
        ModelNode result = services.executeForResult(readOp);
        assertTrue(result.asBoolean());

        ModelNode removeOp = Util.createRemoveOperation(ccmAddress);
        services.executeForResult(removeOp);

        // Read still works despite removal, but now the attributes are back to defaults
        result = services.executeForResult(readOp);
        assertFalse(result.asBoolean());

        // Write attribute works despite removal
        services.executeForResult(writeOp);

        ModelNode addOp = Util.createAddOperation(ccmAddress);
        addOp.get("debug").set(true);
        services.executeForFailure(addOp); // already exists, with install=true

        // Reset to default state and now we can add
        ModelNode undefineOp = Util.createEmptyOperation("undefine-attribute", ccmAddress);
        undefineOp.get("name").set("install");
        services.executeForResult(undefineOp);
        result = services.executeForResult(readOp);
        assertFalse(result.asBoolean());

        // Now add works
        services.executeForResult(addOp);

        ModelNode readOp2 = Util.getReadAttributeOperation(ccmAddress, "debug");
        result = services.executeForResult(readOp2);
        assertTrue(result.asBoolean());

        // Cant' add again
        services.executeForFailure(addOp);

        // Remove and re-add
        services.executeForResult(removeOp);
        result = services.executeForResult(readOp2);
        assertFalse(result.asBoolean());
        services.executeForResult(addOp);
        result = services.executeForResult(readOp2);
        assertTrue(result.asBoolean());
    }

    @Test
    public void testTransformerEAP62() throws Exception {
        testTransformer(ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1, 2, 0), "jca-full.xml");
    }

    @Test
    public void testTransformerEAP62WithExpressions() throws Exception {
        testTransformer(ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1, 2, 0), "jca-full-expression.xml");
    }

    @Test
    public void testTransformerEAP7() throws Exception {
        testTransformer7(ModelTestControllerVersion.EAP_7_0_0, ModelVersion.create(4, 0, 0), "jca-full.xml");
    }

    @Test
    public void testTransformerEAP7Elytron() throws Exception {
        testRejectingTransformerElytronEnabled(ModelTestControllerVersion.EAP_7_0_0, ModelVersion.create(4, 0, 0), "jca-full-elytron.xml");
    }
    /**
     * Tests transformation of model from 1.2.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testTransformer(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, String xmlResourceName) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.as:jboss-as-threads:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.jca.JcaExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource(xmlResourceName);


        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, xmlOps,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER, PathElement.pathElement(WORKMANAGER_SHORT_RUNNING)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE));
    }

    /**
     * Tests transformation of model from 1.2.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testTransformer7(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, String xmlResourceName) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXmlResource(xmlResourceName);

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL(controllerVersion.getMavenGroupId() + ":wildfly-connector:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL(controllerVersion.getCoreMavenGroupId() + ":wildfly-threads:" + controllerVersion.getCoreVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.jca.JcaExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class))
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                //These two are true in the original model but get removed by the transformers, so they default to false. Set them to true
                //modelNode.get(Constants.TRACER, Constants.TRACER). add(new ModelNode(Constants.TRACER));
                //.add(Constants.TRACER);
                modelNode.get(Constants.TRACER, Constants.TRACER, TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().getName()).set(true);
                return modelNode;

            }
        });

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    /**
     * Tests transformation of model from 1.2.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testRejectingTransformerElytronEnabled(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, String xmlResourceName) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL(controllerVersion.getMavenGroupId() + ":wildfly-connector:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL(controllerVersion.getCoreMavenGroupId() + ":wildfly-threads:" + controllerVersion.getCoreVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.jca.JcaExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource(xmlResourceName);


        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, xmlOps,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER),
                                new FailedOperationTransformationConfig.NewAttributesConfig(ELYTRON_ENABLED.getAttribute()))
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaWorkManagerDefinition.PATH_WORK_MANAGER),
                                new FailedOperationTransformationConfig.NewAttributesConfig(ELYTRON_ENABLED.getAttribute())));
    }

    /**
     * Tests transformation of model from 1.2.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testTransformerWF(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, String xmlResourceName) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXmlResource(xmlResourceName);

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.wildfly:wildfly-connector:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.wildfly:wildfly-threads:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.jca.JcaExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class))
                //.skipReverseControllerCheck();
        .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                //These two are true in the original model but get removed by the transformers, so they default to false. Set them to true
                //modelNode.get(Constants.TRACER, Constants.TRACER). add(new ModelNode(Constants.TRACER));
                //.add(Constants.TRACER);
                modelNode.get(Constants.TRACER, Constants.TRACER, TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().getName()).set(true);
                return modelNode;

            }
        });

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }


}
