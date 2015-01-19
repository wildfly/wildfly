/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.mod_cluster.CommonAttributes.CONFIGURATION;
import static org.wildfly.extension.mod_cluster.CommonAttributes.MOD_CLUSTER_CONFIG;

import java.io.IOException;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.msc.service.ServiceController;
import org.junit.Assert;
import org.junit.Test;

/**
 * Quick versions overview:
 * <p/>
 * AS version / model version / schema version
 * 7.1.1 / 1.1.0 / 1_0
 * 7.1.2 / 1.2.0 / 1_1
 * 7.1.3 / 1.2.0 / 1_1
 * 7.2.0 / 1.3.0 / 1_1
 * 8.0.0 / 2.0.0 / 1_2 (this should have been 2_0...)
 * 9.0.0 / 3.0.0 / 2_0
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
public class ModClusterSubsystemTestCase extends AbstractSubsystemBaseTest {

    public ModClusterSubsystemTestCase() {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension());
    }

    // --------------------------------------------------- Standard Subsystem tests

    @Test
    public void testXsd10() throws Exception {
        standardSubsystemTest("subsystem_1_0.xml", false);
    }

    @Test
    public void testXsd11() throws Exception {
        standardSubsystemTest("subsystem_1_1.xml", false);
    }

    @Test
    public void testXsd12() throws Exception {
        standardSubsystemTest("subsystem_1_2.xml", false);
    }

    @Test
    public void testSubsystemWithSimpleLoadProvider() throws Exception {
        super.standardSubsystemTest("subsystem_2_0_simple-load-provider.xml");
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_2_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-mod-cluster_2_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
            "/subsystem-templates/mod_cluster.xml"
        };
    }

    // --------------------------------------------------- Transformers for 1.2 & 1.3

    @Test
    public void testTransformers712() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.V7_1_2_FINAL, "1.2.1.Final");
    }

    @Test
    public void testTransformers713() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.V7_1_3_FINAL, "1.2.1.Final");
    }

    @Test
    public void testTransformersEAP600() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.EAP_6_0_0, "1.2.1.Final-redhat-1");
    }

    @Test
    public void testTransformersEAP601() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.EAP_6_0_1, "1.2.3.Final-redhat-1");
    }

    private void testTransformers_1_2_0(ModelTestControllerVersion controllerVersion, String modClusterJarVersion) throws Exception {
        String subsystemXml = readResource("subsystem_1_1.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.mod_cluster:mod_cluster-core:" + modClusterJarVersion)
                .configureReverseControllerCheck(null, new Undo71TransformModelFixer())
                .setExtensionClassName("org.jboss.as.modcluster.ModClusterExtension");
        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelNode legacySubsystem = checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                fixDefaultCapacity(modelNode.get(MOD_CLUSTER_CONFIG, CONFIGURATION, CommonAttributes.DYNAMIC_LOAD_PROVIDER, CONFIGURATION, CommonAttributes.LOAD_METRIC));
                fixDefaultCapacity(modelNode.get(MOD_CLUSTER_CONFIG, CONFIGURATION, CommonAttributes.DYNAMIC_LOAD_PROVIDER, CONFIGURATION, CommonAttributes.CUSTOM_LOAD_METRIC));
                return modelNode;
            }

            private void fixDefaultCapacity(ModelNode metrics) {
                for (String key : metrics.keys()) {
                    ModelNode capacity = metrics.get(key, CommonAttributes.CAPACITY);
                    if (capacity.getType() == ModelType.DOUBLE && capacity.asString().equals("1.0")) {
                        //There is a bug in 7.1.2 where this attribute is of type int, but its default is a double with value = 1.0
                        capacity.set(1);
                    }
                }
            }
        });

        ModelNode mainSessionCapacity = mainServices.readWholeModel().get(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME, MOD_CLUSTER_CONFIG, CONFIGURATION,
                CommonAttributes.DYNAMIC_LOAD_PROVIDER, CONFIGURATION, CommonAttributes.LOAD_METRIC, "sessions", CommonAttributes.CAPACITY);
        ModelNode legacySessionCapacity = legacySubsystem.get(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME, MOD_CLUSTER_CONFIG, CONFIGURATION,
                CommonAttributes.DYNAMIC_LOAD_PROVIDER, CONFIGURATION, CommonAttributes.LOAD_METRIC, "sessions", CommonAttributes.CAPACITY);
        Assert.assertEquals(ModelType.DOUBLE, mainSessionCapacity.getType());
        Assert.assertEquals(ModelType.INT, legacySessionCapacity.getType());
        Assert.assertFalse(mainSessionCapacity.asString().equals(legacySessionCapacity.asString()));
        Assert.assertEquals(mainSessionCapacity.asInt(), legacySessionCapacity.asInt());
    }

    @Test
    public void testTransformers720() throws Exception {
        testTransformers_1_3_0(ModelTestControllerVersion.V7_2_0_FINAL, "1.2.3.Final");
    }

    @Test
    public void testTransformersEAP610() throws Exception {
        testTransformers_1_3_0(ModelTestControllerVersion.EAP_6_1_0, "1.2.4.Final-redhat-1");
    }

    @Test
    public void testTransformersEAP611() throws Exception {
        testTransformers_1_3_0(ModelTestControllerVersion.EAP_6_1_1, "1.2.4.Final-redhat-1");
    }

    private void testTransformers_1_3_0(ModelTestControllerVersion controllerVersion, String modClusterJarVersion) throws Exception {
        String subsystemXml = readResource("subsystem_1_1.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 3, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.mod_cluster:mod_cluster-core:" + modClusterJarVersion)
                .configureReverseControllerCheck(null, new Undo71TransformModelFixer())
                .setExtensionClassName("org.jboss.as.modcluster.ModClusterExtension");
        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelNode legacySubsystem = checkSubsystemModelTransformation(mainServices, modelVersion);

        ModelNode mainSessionCapacity = mainServices.readWholeModel().get(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME, MOD_CLUSTER_CONFIG, CONFIGURATION,
                CommonAttributes.DYNAMIC_LOAD_PROVIDER, CONFIGURATION, CommonAttributes.LOAD_METRIC, "sessions", CommonAttributes.CAPACITY);
        ModelNode legacySessionCapacity = legacySubsystem.get(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME, MOD_CLUSTER_CONFIG, CONFIGURATION,
                CommonAttributes.DYNAMIC_LOAD_PROVIDER, CONFIGURATION, CommonAttributes.LOAD_METRIC, "sessions", CommonAttributes.CAPACITY);
        Assert.assertEquals(legacySessionCapacity.getType(), mainSessionCapacity.getType());
        Assert.assertTrue(mainSessionCapacity.asString().equals(legacySessionCapacity.asString()));
        Assert.assertEquals(mainSessionCapacity.asInt(), legacySessionCapacity.asInt());
    }

    @Test
    public void testTransformers800() throws Exception {
        testTransformers_2_0_0(ModelTestControllerVersion.WILDFLY_8_0_0_FINAL, "1.3.0.Final");
    }

    private void testTransformers_2_0_0(ModelTestControllerVersion controllerVersion, String modClusterJarVersion) throws Exception {
        String subsystemXml = getSubsystemXml();
        ModelVersion modelVersion = ModelVersion.create(2, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.wildfly:wildfly-mod_cluster-extension:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.mod_cluster:mod_cluster-core:" + modClusterJarVersion)
                .setExtensionClassName("org.wildfly.extension.mod_cluster.ModClusterExtension");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    // --------------------------------------------------- Expressions Rejected prior to 7.1.2 and 7.1.3

    @Test
    public void testExpressionsAreRejected712() throws Exception {
        testExpressionsAreRejectedByVersion_1_2(ModelTestControllerVersion.V7_1_2_FINAL, "1.2.1.Final");
    }

    @Test
    public void testExpressionsAreRejected713() throws Exception {
        testExpressionsAreRejectedByVersion_1_2(ModelTestControllerVersion.V7_1_3_FINAL, "1.2.1.Final");
    }

    @Test
    public void testExpressionsAreRejectedEAP600() throws Exception {
        testExpressionsAreRejectedByVersion_1_2(ModelTestControllerVersion.EAP_6_0_0, "1.2.1.Final-redhat-1");
    }

    @Test
    public void testExpressionsAreRejectedEAP601() throws Exception {
        testExpressionsAreRejectedByVersion_1_2(ModelTestControllerVersion.EAP_6_0_1, "1.2.3.Final-redhat-1");
    }

    private void testExpressionsAreRejectedByVersion_1_2(ModelTestControllerVersion controllerVersion, String modClusterJarVersion) throws Exception {
        String subsystemXml = readResource("subsystem_1_2.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.mod_cluster:mod_cluster-core:" + modClusterJarVersion)
                .setExtensionClassName("org.jboss.as.modcluster.ModClusterExtension");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        PathAddress rootAddr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME));
        PathAddress confAddr = rootAddr.append(PathElement.pathElement(MOD_CLUSTER_CONFIG, CONFIGURATION));
        PathAddress simpAddr = confAddr.append(PathElement.pathElement(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR, CONFIGURATION));
        PathAddress dynaAddr = confAddr.append(PathElement.pathElement(CommonAttributes.DYNAMIC_LOAD_PROVIDER, CONFIGURATION));
        PathAddress metrAddr = dynaAddr.append(PathElement.pathElement(CommonAttributes.LOAD_METRIC, "*"));
        PathAddress custAddr = dynaAddr.append(PathElement.pathElement(CommonAttributes.CUSTOM_LOAD_METRIC, "*"));
        PathAddress sslAddr = confAddr.append(PathElement.pathElement(CommonAttributes.SSL, CONFIGURATION));
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(subsystemXml),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(metrAddr,
                                ChainedConfig.createBuilder(CommonAttributes.CAPACITY, CommonAttributes.WEIGHT, CommonAttributes.PROPERTY, CommonAttributes.SESSION_DRAINING_STRATEGY)
                                    .addConfig(CapacityConfig.INSTANCE)
                                    .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.WEIGHT))
                                    .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.PROPERTY))
                                    .addConfig(new OnlyOnePropertyConfig(CommonAttributes.PROPERTY)).build())
                        .addFailedAttribute(custAddr,
                                ChainedConfig.createBuilder(CommonAttributes.CAPACITY, CommonAttributes.WEIGHT, CommonAttributes.CLASS)
                                    .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.WEIGHT, CommonAttributes.CLASS))
                                    .addConfig(CapacityConfig.INSTANCE).build())
                        .addFailedAttribute(dynaAddr,
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.DECAY, CommonAttributes.HISTORY))
                        .addFailedAttribute(simpAddr,
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.FACTOR))
                        .addFailedAttribute(sslAddr,
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                        CommonAttributes.CIPHER_SUITE, CommonAttributes.KEY_ALIAS,
                                        CommonAttributes.PROTOCOL))
                        .addFailedAttribute(confAddr,
                                ChainedConfig.createBuilder(CommonAttributes.ADVERTISE,
                                        CommonAttributes.ADVERTISE_SOCKET, CommonAttributes.ADVERTISE_SOCKET,
                                        CommonAttributes.AUTO_ENABLE_CONTEXTS, CommonAttributes.FLUSH_PACKETS,
                                        CommonAttributes.PING,
                                        CommonAttributes.STICKY_SESSION, CommonAttributes.STICKY_SESSION_FORCE, CommonAttributes.STICKY_SESSION_REMOVE,
                                        CommonAttributes.SESSION_DRAINING_STRATEGY)
                                        .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.ADVERTISE,
                                                CommonAttributes.ADVERTISE_SOCKET, CommonAttributes.ADVERTISE_SOCKET,
                                                CommonAttributes.AUTO_ENABLE_CONTEXTS, CommonAttributes.FLUSH_PACKETS,
                                                CommonAttributes.PING,
                                                CommonAttributes.STICKY_SESSION, CommonAttributes.STICKY_SESSION_FORCE, CommonAttributes.STICKY_SESSION_REMOVE,
                                                CommonAttributes.SESSION_DRAINING_STRATEGY))
                                        .addConfig(new NeverToDefaultConfig(CommonAttributes.SESSION_DRAINING_STRATEGY)).build())

        );
    }

    @Test
    public void testRejection720() throws Exception {
        testRejection1_3_0(ModelTestControllerVersion.V7_2_0_FINAL, "1.2.3.Final");
    }

    @Test
    public void testRejectionEAP610() throws Exception {
        testRejection1_3_0(ModelTestControllerVersion.EAP_6_1_0, "1.2.4.Final-redhat-1");
    }

    @Test
    public void testRejectionEAP611() throws Exception {
        testRejection1_3_0(ModelTestControllerVersion.EAP_6_1_1, "1.2.4.Final-redhat-1");
    }

    private void testRejection1_3_0(ModelTestControllerVersion controllerVersion, String modClusterJarVersion) throws Exception {
        String subsystemXml = readResource("subsystem_2_0.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 3, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.mod_cluster:mod_cluster-core:" + modClusterJarVersion)
                .setExtensionClassName("org.jboss.as.modcluster.ModClusterExtension");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        PathAddress rootAddr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME));
        PathAddress confAddr = rootAddr.append(PathElement.pathElement(MOD_CLUSTER_CONFIG, CONFIGURATION));
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(subsystemXml),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(confAddr,
                                ChainedConfig.createBuilder(CommonAttributes.SESSION_DRAINING_STRATEGY, CommonAttributes.STATUS_INTERVAL)
                                        .addConfig(new NeverToDefaultConfig(CommonAttributes.SESSION_DRAINING_STRATEGY))
                                        .addConfig(new StatusIntervalConfig(CommonAttributes.STATUS_INTERVAL))
                                        .build())

        );
    }

    /**
     * Tests that:
     * - status-interval is rejected if set to value other than 10.
     *
     * @throws Exception
     */
    @Test
    public void testRejections800() throws Exception {
        testRejections_2_0_0(ModelTestControllerVersion.WILDFLY_8_0_0_FINAL, "1.3.0.Final");
    }

    private void testRejections_2_0_0(ModelTestControllerVersion controllerVersion, String modClusterJarVersion) throws Exception {
        String subsystemXml = readResource("subsystem_2_0-reject.xml");

        ModelVersion modelVersion = ModelVersion.create(2, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.wildfly:wildfly-mod_cluster-extension:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.mod_cluster:mod_cluster-core:" + modClusterJarVersion)
                .setExtensionClassName("org.wildfly.extension.mod_cluster.ModClusterExtension");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME))
                .append(PathElement.pathElement(MOD_CLUSTER_CONFIG, CONFIGURATION));
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(subsystemXml),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(addr,
                                ChainedConfig.createBuilder(CommonAttributes.STATUS_INTERVAL)
                                        .addConfig(new StatusIntervalConfig(CommonAttributes.STATUS_INTERVAL))
                                        .addConfig(new ProxiesConfig(CommonAttributes.PROXIES))
                                        .build())
        );
    }

    @Test
    public void testSSL() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new AdditionalInitialization()).setSubsystemXml(getSubsystemXml());
        KernelServices services = builder.build();
        ModelNode model = services.readWholeModel();
        ModelNode config = model.get(SUBSYSTEM, getMainSubsystemName()).get(MOD_CLUSTER_CONFIG, CONFIGURATION);
        ModelNode ssl = config.get(SSL, CONFIGURATION);
        Assert.assertEquals("/home/rhusar/client-keystore.jks", ssl.get("ca-certificate-file").resolve().asString());
        Assert.assertEquals("/home/rhusar/revocations", ssl.get("ca-revocation-url").resolve().asString());
        Assert.assertEquals("/home/rhusar/client-keystore.jks", ssl.get("certificate-key-file").resolve().asString());
        Assert.assertEquals("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_RC4_128_MD5,SSL_RSA_WITH_RC4_128_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA", ssl.get("cipher-suite").resolve().asString());
        Assert.assertEquals("mykeyalias", ssl.get("key-alias").resolve().asString());
        Assert.assertEquals("mypassword", ssl.get("password").resolve().asString());
        Assert.assertEquals("TLSv1", ssl.get("protocol").resolve().asString());
        ServiceController<?> service = services.getContainer().getService(ContainerEventHandlerService.CONFIG_SERVICE_NAME);
        MCMPHandlerConfiguration sslConfig = (MCMPHandlerConfiguration) service.getValue();
        Assert.assertTrue(sslConfig.isSsl());
        Assert.assertEquals("mykeyalias", sslConfig.getSslKeyAlias());
        Assert.assertEquals("mypassword", sslConfig.getSslTrustStorePassword());
        Assert.assertEquals("mypassword", sslConfig.getSslKeyStorePassword());
        Assert.assertEquals("/home/rhusar/client-keystore.jks", sslConfig.getSslKeyStore());
        Assert.assertEquals("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_RC4_128_MD5,SSL_RSA_WITH_RC4_128_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA", sslConfig.getSslCiphers());
        Assert.assertEquals("TLSv1", sslConfig.getSslProtocol());
        Assert.assertEquals("/home/rhusar/client-keystore.jks", sslConfig.getSslTrustStore());
        Assert.assertEquals("/home/rhusar/revocations", sslConfig.getSslCrlFile());
    }

    /**
     * Checks that the attribute list only has one entry
     */
    private static class OnlyOnePropertyConfig extends AttributesPathAddressConfig<OnlyOnePropertyConfig> {

        public OnlyOnePropertyConfig(String... attributes) {
            super(attributes);
        }

        @Override
        protected ModelNode correctValue(ModelNode value, boolean isWriteAttribute) {
            if (value.getType() == ModelType.OBJECT) {
                Set<String> keys = value.keys();
                if (keys.size() > 1) {
                    String key = keys.iterator().next();
                    value.remove(key);
                }
            }
            return value;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            if (!attribute.isDefined()) {
                return false;
            }
            if (attribute.getType() == ModelType.OBJECT) {
                return attribute.keys().size() > 1;

            }
            return false;
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return false;
        }
    }


    private static class NeverToDefaultConfig extends AttributesPathAddressConfig<NeverToDefaultConfig> {
        public NeverToDefaultConfig(String... attributes) {
            super(attributes);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            if (!attribute.isDefined()) {
                return false;
            }
            return !attribute.asString().equals("DEFAULT");
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode("DEFAULT");
        }
    }

    private static class ProxiesConfig extends AttributesPathAddressConfig<ProxiesConfig> {
        public ProxiesConfig(String... attributes) {
            super(attributes);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.equals(new ModelNode());
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode();
        }
    }

    private static class StatusIntervalConfig extends AttributesPathAddressConfig<StatusIntervalConfig> {
        public StatusIntervalConfig(String... attributes) {
            super(attributes);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.equals(new ModelNode(10));
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(10);
        }
    }

    /**
     * Fixes model produced by a 7.2 (or later) controller that executes operations produced for 7.1
     * such that the model anomalies produced by the transform are removed.
     */
    private static class Undo71TransformModelFixer implements ModelFixer {

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            if (modelNode.getType() == ModelType.OBJECT) {
                for (Property property : modelNode.asPropertyList()) {
                    if (property.getName().equals(LoadMetricDefinition.CAPACITY.getName())) {
                        if (property.getValue().getType() == ModelType.INT) {
                            modelNode.get(property.getName()).set(property.getValue().asDouble());
                        }
                    } else if (property.getName().equals(LoadMetricDefinition.PROPERTY.getName())) {
                        if (property.getValue().getType() == ModelType.PROPERTY) {
                            Property child = property.getValue().asProperty();
                            ModelNode object = new ModelNode();
                            object.get(child.getName()).set(child.getValue());
                            modelNode.get(property.getName()).set(object);
                        }
                    } else if (property.getValue().isDefined()) {
                        modelNode.get(property.getName()).set(fixModel(property.getValue()));
                    }
                }
            }

            return modelNode;
        }
    }

    private static class CapacityConfig extends FailedOperationTransformationConfig.RejectExpressionsConfig {

        private static final CapacityConfig INSTANCE = new CapacityConfig();

        private CapacityConfig() {
            super(CommonAttributes.CAPACITY);
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return super.checkValue(attrName, attribute, isWriteAttribute)
                    || attribute.getType() == ModelType.DOUBLE
                    || attribute.getType() == ModelType.STRING;
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            ModelNode result = super.correctValue(toResolve, isWriteAttribute);
            if (result.equals(toResolve)
                    && (result.getType() == ModelType.DOUBLE || result.getType() == ModelType.STRING)) {
                result = new ModelNode((int) Math.round(result.asDouble()));
            }
            return result;
        }
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization.ManagementAdditionalInitialization() {
            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                super.setupController(controllerInitializer);

                controllerInitializer.addSocketBinding("modcluster", 0); // "224.0.1.105", "23364"
                controllerInitializer.addRemoteOutboundSocketBinding("proxy1", "localhost", 6666);
                controllerInitializer.addRemoteOutboundSocketBinding("proxy2", "localhost", 6766);
            }
        };
    }
}
