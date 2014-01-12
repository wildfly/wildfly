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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
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
 * @author <a href="stefano.maestri@redhat.com>Stefano Maestri</a>
 */
public class DatasourcesSubsystemTestCase extends AbstractSubsystemBaseTest {

    static final AttributeDefinition[] ALL_DS_ATTRIBUTES_REJECTED_1_1_0  = new AttributeDefinition[Constants.DATASOURCE_PROPERTIES_ATTRIBUTES.length + 1];
    static {
        System.arraycopy(Constants.DATASOURCE_PROPERTIES_ATTRIBUTES, 0, ALL_DS_ATTRIBUTES_REJECTED_1_1_0, 0, Constants.DATASOURCE_PROPERTIES_ATTRIBUTES.length);
        ALL_DS_ATTRIBUTES_REJECTED_1_1_0[Constants.DATASOURCE_PROPERTIES_ATTRIBUTES.length] = Constants.CONNECTABLE;
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
        testTransformer1_1_2("datasources-full-expression110.xml", ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testTransformersEAP611() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testTransformer1_1_2("datasources-full-expression110.xml", ModelTestControllerVersion.EAP_6_1_1);
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

        checkSubsystemModelTransformation(mainServices, modelVersion);
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
                  .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null)
                  .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion);
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
    }


    private static FailedOperationTransformationConfig.ChainedConfig FAILED_TRANSFORMER_1_1_0 =
            FailedOperationTransformationConfig.ChainedConfig.createBuilder(ALL_DS_ATTRIBUTES_REJECTED_1_1_0)
            .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(Constants.DATASOURCE_PROPERTIES_ATTRIBUTES))
            .build();

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
            return attribute.asBoolean();
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(false);
        }
    }

}
