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
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MODULE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_GROUP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ResourceAdaptersSubsystemTestCase extends AbstractSubsystemBaseTest {

    public ResourceAdaptersSubsystemTestCase() {
        // FIXME ResourceAdaptersSubsystemTestCase constructor
        super(ResourceAdaptersExtension.SUBSYSTEM_NAME, new ResourceAdaptersExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("empty-resourceadapters.xml");
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("resource-adapters-pool.xml", null, true);
    }


    @Test
    public void testFullConfigXa() throws Exception {
        standardSubsystemTest("resource-adapters-xapool.xml", null, true);
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("resource-adapters-pool-expression.xml", "resource-adapters-pool.xml", true);
    }

    @Test
    public void testExpressionConfigXa() throws Exception {
        standardSubsystemTest("resource-adapters-xapool-expression.xml", "resource-adapters-xapool.xml", true);
    }

    @Test
    public void testTransformerAS712() throws Exception {
        testTransformer("resource-adapters-xapool.xml", ModelTestControllerVersion.V7_1_2_FINAL, ModelVersion.create(1, 1, 0));
    }

    @Test
    public void testTransformerAS713() throws Exception {
        testTransformer("resource-adapters-xapool.xml", ModelTestControllerVersion.V7_1_3_FINAL, ModelVersion.create(1, 1, 0));
    }

    @Test
    public void tesExpressionsAS712() throws Exception {
        //this file contain expression for all supported fields except bean-validation-groups and recovery-plugin-properties
        // for a limitation in test suite not permitting to have expression in type LIST or OBJECT for legacyServices
        testTransformer("resource-adapters-xapool-expression2.xml", ModelTestControllerVersion.V7_1_2_FINAL, ModelVersion.create(1, 1, 0));
    }

    @Test
    public void testExpressionsAS713() throws Exception {
        //this file contain expression for all supported fields except bean-validation-groups and recovery-plugin-properties
        // for a limitation in test suite not permitting to have expression in type LIST or OBJECT for legacyServices
        testTransformer("resource-adapters-xapool-expression2.xml", ModelTestControllerVersion.V7_1_3_FINAL, ModelVersion.create(1, 1, 0));
    }

    @Test
    public void testTransformerAS72() throws Exception {
        testRejectingTransformer("resource-adapters-pool-20.xml", ModelTestControllerVersion.V7_2_0_FINAL, ModelVersion.create(1, 2, 0));
    }

    @Test
    public void testExpressionsAS72() throws Exception {
        //this file contain expression for all supported fields except bean-validation-groups and recovery-plugin-properties
        // for a limitation in test suite not permitting to have expression in type LIST or OBJECT for legacyServices
        testTransformer("resource-adapters-xapool-expression.xml", ModelTestControllerVersion.V7_2_0_FINAL, ModelVersion.create(1, 2, 0));
    }
    /**
     * Tests transformation of model from current to passed one
     *
     * @throws Exception
     */
    private void testTransformer(String subsystemXml, ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension")
                .addOperationValidationResolve("add", PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, mainSubsystemName),
                        PathElement.pathElement("resource-adapter", "*"),
                        PathElement.pathElement("connection-definitions", "*")))
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class))
                .configureReverseControllerCheck(null, new ModelFixer() {

                    @Override
                    public ModelNode fixModel(ModelNode modelNode) {
                        //Replace the value used in the xml
                        if (modelNode.get(Constants.RESOURCEADAPTER_NAME).hasDefined("myRA")) {
                            if (modelNode.get(Constants.RESOURCEADAPTER_NAME).get("myRA").get(Constants.CONNECTIONDEFINITIONS_NAME).get("poolName").get(Constants.ENLISTMENT.getName()).isDefined())
                                modelNode.get(Constants.RESOURCEADAPTER_NAME).get("myRA").get(Constants.CONNECTIONDEFINITIONS_NAME).get("poolName").get(Constants.ENLISTMENT.getName()).set(false);
                            if (modelNode.get(Constants.RESOURCEADAPTER_NAME).get("myRA").get(Constants.CONNECTIONDEFINITIONS_NAME).get("poolName").get(Constants.SHARABLE.getName()).isDefined())
                                modelNode.get(Constants.RESOURCEADAPTER_NAME).get("myRA").get(Constants.CONNECTIONDEFINITIONS_NAME).get("poolName").get(Constants.SHARABLE.getName()).set(false);

                        }
                        return modelNode;

                    }
                });

        KernelServices mainServices = builder.build();
        org.junit.Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        org.junit.Assert.assertTrue(legacyServices.isSuccessfulBoot());
        org.junit.Assert.assertNotNull(legacyServices);


        checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer() {

                            @Override
                            public ModelNode fixModel(ModelNode modelNode) {
                                //Replace the value used in the xml
                                if (modelNode.get(Constants.RESOURCEADAPTER_NAME).get("myRA").get(Constants.CONNECTIONDEFINITIONS_NAME).get("poolName").isDefined()) {
                                    if(! modelNode.get(Constants.RESOURCEADAPTER_NAME).get("myRA").get(Constants.CONNECTIONDEFINITIONS_NAME).get("poolName").hasDefined(Constants.APPLICATION.getName()))
                                        modelNode.get(Constants.RESOURCEADAPTER_NAME).get("myRA").get(Constants.CONNECTIONDEFINITIONS_NAME).get("poolName").get(Constants.APPLICATION.getName()).set(false);
                                }

                                return modelNode;

                            }
                        });

    }

    /**
     * Tests transformation of model from current to passed one
     *
     * @throws Exception
     */
    private void testRejectingTransformer(String subsystemXml, ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
        //.setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension")
                .addOperationValidationResolve("add", PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, mainSubsystemName),
                        PathElement.pathElement("resource-adapter", "*"),
                        PathElement.pathElement("connection-definitions", "*")))
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        org.junit.Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        org.junit.Assert.assertTrue(legacyServices.isSuccessfulBoot());
        org.junit.Assert.assertNotNull(legacyServices);

        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(ResourceAdaptersExtension.SUBSYSTEM_PATH);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress.append(PathElement.pathElement(RESOURCEADAPTER_NAME)),
                        new FailedOperationTransformationConfig.AttributesPathAddressConfig(WM_SECURITY.getName(), WM_SECURITY_MAPPING_REQUIRED.getName(),
                                WM_SECURITY_DOMAIN.getName(), MODULE.getName()) {
                            @Override
                            protected boolean isAttributeWritable(String attributeName) {
                                return false;
                            }

                            @Override
                            protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
                                if (attribute.isDefined()) {
                                    return true;
                                } else {
                                    return false;
                                }
                            }

                            @Override
                            protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
                                return new ModelNode();
                            }


                        })

        );
 }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    //TODO: remove this special method as soon as RA will have a unique name in DMR marshalled in xml
    protected KernelServices standardSubsystemTest(final String configId, final String configIdResolvedModel, boolean compareXml) throws Exception {
        final AdditionalInitialization additionalInit = createAdditionalInitialization();


        // Parse the subsystem xml and install into the first controller
        final String subsystemXml = configId == null ? getSubsystemXml() : getSubsystemXml(configId);
        final KernelServices servicesA = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
        Assert.assertTrue("Subsystem boot failed!", servicesA.isSuccessfulBoot());
        //Get the model and the persisted xml from the first controller
        final ModelNode modelA = servicesA.readWholeModel();
        validateModel(modelA);

        // Test marshaling
        final String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();


        // validate the the normalized xmls
        String normalizedSubsystem = normalizeXML(subsystemXml);

        if (compareXml) {
            compareXml(configId, normalizedSubsystem, normalizeXML(marshalled));
        }

        //Install the persisted xml from the first controller into a second controller
        final KernelServices servicesB = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
        final ModelNode modelB = servicesB.readWholeModel();

        //WE CANT DO THAT FOR RA BECAUSE THEIR NAME IS GENERATED ON THE FLY W/ archive/module concatenated to a counter.
        //IT'S AN ISSUE WM_SECURITY_MAPPING_TO BE FIXED IN SUBSYSTEM
        //Make sure the models from the two controllers are identical
        //compare(modelA, modelB);

        // Test the describe operation
        final ModelNode operation = createDescribeOperation();
        final ModelNode result = servicesB.executeOperation(operation);
        Assert.assertTrue("the subsystem describe operation has to generate a list of operations to recreate the subsystem",
                !result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
        final List<ModelNode> operations = result.get(ModelDescriptionConstants.RESULT).asList();
        servicesB.shutdown();

        final KernelServices servicesC = super.createKernelServicesBuilder(additionalInit).setBootOperations(operations).build();
        final ModelNode modelC = servicesC.readWholeModel();

        compare(modelB, modelC);

        assertRemoveSubsystemResources(servicesC, getIgnoredChildResourcesForRemovalTest());

        if (configIdResolvedModel != null) {
            final String subsystemResolvedXml = getSubsystemXml(configIdResolvedModel);
            final KernelServices servicesD = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemResolvedXml).build();
            Assert.assertTrue("Subsystem w/ reolved xml boot failed!", servicesD.isSuccessfulBoot());
            final ModelNode modelD = servicesD.readWholeModel();
            validateModel(modelD);
            resolveandCompareModel(modelA, modelD);
        }

        return servicesA;
    }


}
