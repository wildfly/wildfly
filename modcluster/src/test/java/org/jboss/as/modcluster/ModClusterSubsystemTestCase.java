/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.modcluster.CommonAttributes.CONFIGURATION;
import static org.jboss.as.modcluster.CommonAttributes.MOD_CLUSTER_CONFIG;

import java.io.IOException;
import java.util.Set;

import junit.framework.Assert;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.msc.service.ServiceController;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jean-Frederic Clere.
 */
public class ModClusterSubsystemTestCase extends AbstractSubsystemBaseTest {

    public ModClusterSubsystemTestCase() {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension());
    }

    @Test
    public void testXsd10() throws Exception {
        standardSubsystemTest("subsystem_1_0.xml", false);
    }

    @Test
    public void testTransformers712() throws Exception {
        testTransformers_1_2_0("7.1.2.Final");
    }

    @Test
    public void testTransformers713() throws Exception {
        testTransformers_1_2_0("7.1.3.Final");
    }

    private void testTransformers_1_2_0(String version) throws Exception {
        String subsystemXml = readResource("subsystem-transform-no-reject.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:" + version)
                .configureReverseControllerCheck(null, new Undo71TransformModelFixer());

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
    public void testExpressionsAreRejected712() throws Exception {
        testExpressionsAreRejectedByVersion_1_2("7.1.2.Final");
    }

    @Test
    public void testExpressionsAreRejected713() throws Exception {
        testExpressionsAreRejectedByVersion_1_2("7.1.2.Final");
    }

    private void testExpressionsAreRejectedByVersion_1_2(String version) throws Exception {
        String subsystemXml = readResource("subsystem-transform-reject.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:" + version);

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
                                ChainedConfig.createBuilder(CommonAttributes.CAPACITY, CommonAttributes.WEIGHT, CommonAttributes.PROPERTY)
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
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.ADVERTISE,
                                        CommonAttributes.ADVERTISE_SOCKET, CommonAttributes.ADVERTISE_SOCKET,
                                        CommonAttributes.AUTO_ENABLE_CONTEXTS, CommonAttributes.FLUSH_PACKETS,
                                        CommonAttributes.PING,
                                        CommonAttributes.STICKY_SESSION, CommonAttributes.STICKY_SESSION_FORCE, CommonAttributes.STICKY_SESSION_REMOVE
                                ))
        );
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-transform-reject.xml");
    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }


    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    @Test
    public void testSSL() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new AdditionalInitialization())
                .setSubsystemXml(getSubsystemXml());
        KernelServices services = builder.build();
        ModelNode model = services.readWholeModel();
        ModelNode config = model.get(SUBSYSTEM, getMainSubsystemName()).get(MOD_CLUSTER_CONFIG, CONFIGURATION);
        ModelNode ssl = config.get(SSL, CONFIGURATION);
        Assert.assertEquals(ssl.get("ca-certificate-file").resolve().asString(), "/home/rhusar/client-keystore.jks");
        Assert.assertEquals(ssl.get("ca-revocation-url").resolve().asString(), "/home/rhusar/revocations");
        Assert.assertEquals(ssl.get("certificate-key-file").resolve().asString(), "/home/rhusar/client-keystore.jks");
        Assert.assertEquals(ssl.get("cipher-suite").resolve().asString(), "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_RC4_128_MD5,SSL_RSA_WITH_RC4_128_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        Assert.assertEquals(ssl.get("key-alias").resolve().asString(), "mykeyalias");
        Assert.assertEquals(ssl.get("password").resolve().asString(), "mypassword");
        Assert.assertEquals(ssl.get("protocol").resolve().asString(), "TLS");
        ServiceController<ModCluster> service = (ServiceController<ModCluster>) services.getContainer().getService(ModClusterService.NAME);
        ModClusterService modCluster = (ModClusterService) service.getService().getValue();
        ModClusterConfig modClusterConfig = modCluster.getConfig();
        Assert.assertTrue(modClusterConfig.isSsl());
        Assert.assertEquals(modClusterConfig.getSslKeyAlias(), "mykeyalias");
        Assert.assertEquals(modClusterConfig.getSslTrustStorePassword(), "mypassword");
        Assert.assertEquals(modClusterConfig.getSslKeyStorePassword(), "mypassword");
        Assert.assertEquals(modClusterConfig.getSslKeyStore(), "/home/rhusar/client-keystore.jks");
        Assert.assertEquals(modClusterConfig.getSslCiphers(), "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_RC4_128_MD5,SSL_RSA_WITH_RC4_128_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        Assert.assertEquals(modClusterConfig.getSslProtocol(), "TLS");
        Assert.assertEquals(modClusterConfig.getSslTrustStore(), "/home/rhusar/client-keystore.jks");
        Assert.assertEquals(modClusterConfig.getSslCrlFile(), "/home/rhusar/revocations");
    }


    /**
     * Checks that the attribute list only has one entry
     *
     */
    private static class OnlyOnePropertyConfig extends AttributesPathAddressConfig<OnlyOnePropertyConfig> {

        public OnlyOnePropertyConfig(String...attributes) {
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
}
