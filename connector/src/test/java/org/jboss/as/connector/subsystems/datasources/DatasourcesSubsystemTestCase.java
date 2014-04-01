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
package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.OperationFixer;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="stefano.maestri@redhat.com>Stefano Maestri</a>
 */
public class DatasourcesSubsystemTestCase extends AbstractSubsystemBaseTest {

    static final AttributeDefinition[] ALL_DS_ATTRIBUTES_REJECTED_1_1_0  = new AttributeDefinition[Constants.DATASOURCE_PROPERTIES_ATTRIBUTES.length + 2];
    static {
        System.arraycopy(Constants.DATASOURCE_PROPERTIES_ATTRIBUTES, 0, ALL_DS_ATTRIBUTES_REJECTED_1_1_0, 0, Constants.DATASOURCE_PROPERTIES_ATTRIBUTES.length);
        ALL_DS_ATTRIBUTES_REJECTED_1_1_0[Constants.DATASOURCE_PROPERTIES_ATTRIBUTES.length] = Constants.CONNECTABLE;
        ALL_DS_ATTRIBUTES_REJECTED_1_1_0[Constants.DATASOURCE_PROPERTIES_ATTRIBUTES.length + 1] = Constants.STATISTICS_ENABLED;
    }

    public DatasourcesSubsystemTestCase() {
        super(DataSourcesExtension.SUBSYSTEM_NAME, new DataSourcesExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //test configuration put in standalone.xml
        return readResource("datasources-minimal.xml");
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("datasources-full.xml");
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("datasources-full-expression.xml", "datasources-full.xml");
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    @Test
    public void testTransformerEAP600() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testTransformer1_1_0("datasources-full110.xml", ModelTestControllerVersion.EAP_6_0_0);
    }

    @Test
    public void testTransformerEAP601() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testTransformer1_1_0("datasources-full110.xml", ModelTestControllerVersion.EAP_6_0_1);
    }

    @Test
    public void tesExpressionsEAP600() throws Exception {
        //this file contain expression for all supported fields except reauth-plugin-properties, exception-sorter-properties,
        // stale-connection-checker-properties, valid-connection-checker-properties, recovery-plugin-properties
        // for a limitation in test suite not permitting to have expression in type LIST or OBJECT for legacyServices
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testRejectTransformers1_1_0("datasources-full-expression110.xml", ModelTestControllerVersion.EAP_6_0_0);
    }
    @Test
    public void testTransformerAS712() throws Exception {
        testRejectTransformers1_1_0("datasources-full-expression110.xml", ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformerAS713() throws Exception {
        testRejectTransformers1_1_0("datasources-full-expression110.xml", ModelTestControllerVersion.V7_1_3_FINAL);
    }


    @Test
    public void testExpressionsEAP601() throws Exception {
        //this file contain expression for all supported fields except reauth-plugin-properties, exception-sorter-properties,
        // stale-connection-checker-properties, valid-connection-checker-properties, recovery-plugin-properties
        // for a limitation in test suite not permitting to have expression in type LIST or OBJECT for legacyServices
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testRejectTransformers1_1_0("datasources-full-expression110.xml", ModelTestControllerVersion.EAP_6_0_1);
    }

    @Test
    public void testTransformersEAP610() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testTransformer1_1_2("datasources-full110.xml", ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testTransformersEAP611() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testTransformer1_1_2("datasources-full110.xml", ModelTestControllerVersion.EAP_6_1_1);
    }


    @Test
    public void testRejectingTransformersEAP620() throws Exception {
        testRejectTransformers1_2_0("datasources-full-expression.xml", ModelTestControllerVersion.EAP_6_2_0);
    }

    /**
     * Tests transformation of model from 1.1.1 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testTransformer1_1_0(String subsystemXml, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class))
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, new ModelFixer() {
                    @Override
                    public ModelNode fixModel(ModelNode modelNode) {
                        //Replace the value used in the xml
                        modelNode.get(Constants.XA_DATASOURCE).get("complexXaDs_Pool").remove(Constants.JTA.getName());
                        return modelNode;

                    }
                }, REVERSE_CONTROLLER);

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion, XA_JTA_MODEL_FIXER);

        ModelNode enable = Util.createEmptyOperation("enable", PathAddress.pathAddress(Constants.XA_DATASOURCE, "complexXaDs_Pool"));
        TransformedOperation transformed = mainServices.transformOperation(modelVersion, enable);
        Assert.assertFalse(transformed.getFailureDescription(), transformed.rejectOperation(SUCCESS));

    }

    private void testTransformer1_1_2(String subsystemXml, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 2); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null,controllerVersion,  modelVersion)
                  .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                  .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension")
                  .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null, REVERSE_CONTROLLER)
                  .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion, XA_JTA_MODEL_FIXER);
    }

    private void testTransformer1_2_0(String subsystemXml, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null,controllerVersion,  modelVersion)
                  .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                  .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension")
                  .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null)
                  .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion, XA_JTA_MODEL_FIXER);
    }

    public void testRejectTransformers1_1_0(String subsystemXml, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress.append(JdbcDriverDefinition.PATH_DRIVER),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(Constants.DRIVER_MINOR_VERSION, Constants.DRIVER_MAJOR_VERSION) {
                            @Override
                            protected boolean isAttributeWritable(String attributeName) {
                                return false;
                            }
                        })
                .addFailedAttribute(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE), FAILED_TRANSFORMER_1_1_0)
                .addFailedAttribute(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE), FAILED_TRANSFORMER_1_1_0)
        );

        checkCanEnableAndDisable(mainServices, modelVersion,
                PathAddress.pathAddress(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE)),
                PathAddress.pathAddress(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE)));
    }

    public void testRejectTransformers1_1_1(String subsystemXml, ModelTestControllerVersion controllerVersion) throws Exception {
            ModelVersion modelVersion = ModelVersion.create(1, 1, 1); //The old model version
            //Use the non-runtime version of the extension which will happen on the HC
            KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

            // Add legacy subsystems
            builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                    .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                    .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension")
                    .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

            KernelServices mainServices = builder.build();
            assertTrue(mainServices.isSuccessfulBoot());
            KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
            assertNotNull(legacyServices);
            assertTrue(legacyServices.isSuccessfulBoot());

            List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE),FAILED_TRANSFORMER_1_1_1)
                .addFailedAttribute(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE), FAILED_TRANSFORMER_1_1_1)
        );

        checkCanEnableAndDisable(mainServices, modelVersion,
                PathAddress.pathAddress(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE)),
                PathAddress.pathAddress(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE)));
    }

    public void testRejectTransformers1_2_0(String subsystemXml, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE),FAILED_TRANSFORMER_1_2_0)
                .addFailedAttribute(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE), FAILED_TRANSFORMER_1_2_0)
        );

        checkCanEnableAndDisable(mainServices, modelVersion,
                PathAddress.pathAddress(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE)),
                PathAddress.pathAddress(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE)));
    }

    /**
     * For the reverse controller checks, the 'working' tests are booted with statistics-enabled="true" (anything else gets rejected).
     * Since statistics-enabled isn't in the legacy model, it is removed from the boot ops. In the current model the default is true.
     * So correct the relevant add operations to include statistics-enabled="true" again for the model comparison to work.
     *
     */
    private static final OperationFixer REVERSE_CONTROLLER = new OperationFixer() {
		@Override
		public ModelNode fixOperation(ModelNode operation) {
			if (operation.get(OP).asString().equals(ModelDescriptionConstants.ADD)){
				PathAddress addr = PathAddress.pathAddress(operation.get(OP_ADDR));
				if (addr.size() == 2 && addr.getElement(0).getKey().equals(ModelDescriptionConstants.SUBSYSTEM)){
					String type = addr.getElement(1).getKey();
					if (type.equals(DataSourceDefinition.PATH_DATASOURCE.getKey()) || type.equals(XaDataSourceDefinition.PATH_XA_DATASOURCE.getKey())){
						operation.get(Constants.STATISTICS_ENABLED.getName()).set(true);
						return operation;
					}
				}
			}
			return operation;
		}
	};

    private static FailedOperationTransformationConfig.ChainedConfig FAILED_TRANSFORMER_1_1_0 =
            FailedOperationTransformationConfig.ChainedConfig.createBuilder(ALL_DS_ATTRIBUTES_REJECTED_1_1_0)
                    .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(Constants.DATASOURCE_PROPERTIES_ATTRIBUTES))
                    .addConfig(new SetToTrue(Constants.STATISTICS_ENABLED)).
                    build();

    private void checkCanEnableAndDisable(KernelServices services, ModelVersion modelVersion, PathAddress...pathAddresses) throws OperationFailedException {
        final ModelNode success = new ModelNode();
        success.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.SUCCESS);
        success.get(ModelDescriptionConstants.RESULT);
        success.protect();

        for (PathAddress address : pathAddresses) {
            ModelNode enable = Util.createEmptyOperation("enable", address);
            TransformedOperation transformedEnable = services.transformOperation(modelVersion, enable);
            Assert.assertNotNull(transformedEnable.getTransformedOperation());
            Assert.assertFalse(transformedEnable.getFailureDescription(), transformedEnable.rejectOperation(success));

            ModelNode disable = Util.createEmptyOperation("disable", address);
            TransformedOperation transformedDisable = services.transformOperation(modelVersion, disable);
            Assert.assertNotNull(transformedDisable.getTransformedOperation());
            Assert.assertFalse(transformedDisable.getFailureDescription(), transformedDisable.rejectOperation(success));
        }

    }


    private static class NonWritableChainedConfig extends FailedOperationTransformationConfig.ChainedConfig {

        public NonWritableChainedConfig(List<AttributesPathAddressConfig<?>> configs, String[] attributes) {
            // FIXME NonWritableChainedConfig constructor
            super(configs, attributes);
        }

        public static Builder createBuilder(final String...attributes) {
            return new Builder() {
                ArrayList<AttributesPathAddressConfig<?>> list = new ArrayList<FailedOperationTransformationConfig.AttributesPathAddressConfig<?>>();
                @Override
                public ChainedConfig build() {
                    return new NonWritableChainedConfig(list, attributes);
                }

                @Override
                public Builder addConfig(AttributesPathAddressConfig<?> cfg) {
                    list.add(cfg);
                    return this;
                }
            };
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            //TODO use the same old behaviour of not writable attributes (this is actually due to missing functionality in ChainedConfig)
            return false;
        }
    }


    private static FailedOperationTransformationConfig.ChainedConfig FAILED_TRANSFORMER_1_1_1 = NonWritableChainedConfig.createBuilder(
            Constants.CONNECTABLE.getName())
            .addConfig(new RejectExpressionsAndSetToTrue(Constants.CONNECTABLE))
            .addConfig(new SetToTrue(Constants.STATISTICS_ENABLED))
            .build();

    private static FailedOperationTransformationConfig.ChainedConfig FAILED_TRANSFORMER_1_2_0 = NonWritableChainedConfig.createBuilder(
            Constants.CONNECTABLE.getName(), Constants.STATISTICS_ENABLED.getName())
            .addConfig(new RejectExpressionsAndSetToTrue(Constants.CONNECTABLE))
            .addConfig(new SetToTrue(Constants.STATISTICS_ENABLED))
            .build();

    private static class RejectExpressionsAndSetToTrue extends FailedOperationTransformationConfig.RejectExpressionsConfig {

        public RejectExpressionsAndSetToTrue(AttributeDefinition... attributes) {
            // FIXME RejectExpressionsAndSetToTrue constructor
            super(attributes);
        }

        public RejectExpressionsAndSetToTrue(String... attributes) {
            // FIXME RejectExpressionsAndSetToTrue constructor
            super(attributes);
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            if (super.checkValue(attrName, attribute, isWriteAttribute)) {
                return true;
            }
            try {
                return attribute.isDefined() && attribute.asBoolean();
            } catch (IllegalArgumentException e) {
                throw e;
            }
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(false);
        }
    }


    private static ModelFixer XA_JTA_MODEL_FIXER = new ModelFixer() {

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            //xa-datasource wrongly had jta in the model before 1.3.0. It never gets set via our parser,
            //but appears in the model as true since defaults are included
            if (modelNode.hasDefined("xa-data-source")) {
                for (Property prop : modelNode.get("xa-data-source").asPropertyList()) {
                    String name = prop.getName();
                    modelNode.get("xa-data-source", name).remove("jta");
                }
            }
            return modelNode;
        }
    };

    private static class SetToTrue extends FailedOperationTransformationConfig.AttributesPathAddressConfig<SetToTrue> {
        public SetToTrue(AttributeDefinition... attributes) {
            super(convert(attributes));
        }

        public SetToTrue(String... attributes) {
            super(attributes);
        }

        @Override
		public boolean expectFailed(ModelNode operation) {
        	//Override the AttributesPathAddressConfig.expectFailed() method to totally turn off checking for these methods
        	//The issue is that statistics-enabled must be true, however :enable and :disable don't have this attribute,
        	//and the checkValue method does not have access to the operation name

        	String name = operation.get(OP).asString();
        	if (name.equals("enable") || name.equals("disable")) {
        		return false;
        	}
			return super.expectFailed(operation);
		}

		@Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            //Fix if undefined or false
            return !attribute.isDefined() || !attribute.asBoolean();
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(true);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }
    }

    private static final ModelNode SUCCESS;
    static
    {
        SUCCESS = new ModelNode();
        SUCCESS.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.SUCCESS);
        SUCCESS.get(ModelDescriptionConstants.RESULT);
        SUCCESS.protect();
    }

}
