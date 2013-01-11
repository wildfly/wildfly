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

import junit.framework.Assert;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
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
    public void testTransformers_1_2_0() throws Exception {
        String subsystemXml = readResource("subsystem-no-expressions.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:7.1.2.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelNode legacySubsystem = checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                ModelNode loadMetrics = modelNode.get(MOD_CLUSTER_CONFIG, CONFIGURATION, CommonAttributes.DYNAMIC_LOAD_PROVIDER, CONFIGURATION, CommonAttributes.LOAD_METRIC);
                for (String key : loadMetrics.keys()) {
                    ModelNode capacity = loadMetrics.get(key, CommonAttributes.CAPACITY);
                    if (capacity.getType() == ModelType.DOUBLE && capacity.asString().equals("1.0")) {
                        //There is a bug in 7.1.2 where this attribute is of type int, but its default is a double with value = 1.0
                        capacity.set(1);
                    }
                }
                return modelNode;
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
    public void testExpressionsAreRejectedByVersion_1_2() throws Exception {
        String subsystemXml = readResource("subsystem.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:7.1.2.Final");

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
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.CAPACITY, CommonAttributes.WEIGHT
                                ))
                        .addFailedAttribute(custAddr,
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.CAPACITY, CommonAttributes.WEIGHT,
                                        CommonAttributes.CLASS))
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
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }


    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    @Override
    protected void validateModel(ModelNode model) {
        super.validateModel(model);
        ModelNode ssl = model.get(SUBSYSTEM, getMainSubsystemName()).get(MOD_CLUSTER_CONFIG, CONFIGURATION).get(SSL, CONFIGURATION);
        Assert.assertEquals(ssl.get("ca-certificate-file").resolve().asString(), "/home/rhusar/client-keystore.jks");
        Assert.assertEquals(ssl.get("ca-revocation-url").resolve().asString(), "/home/rhusar/revocations");
        Assert.assertEquals(ssl.get("certificate-key-file").resolve().asString(), "/home/rhusar/client-keystore.jks");
        Assert.assertEquals(ssl.get("cipher-suite").resolve().asString(), "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_RC4_128_MD5,SSL_RSA_WITH_RC4_128_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        Assert.assertEquals(ssl.get("key-alias").resolve().asString(), "mykeyalias");
        Assert.assertEquals(ssl.get("password").resolve().asString(), "mypassword");
        Assert.assertEquals(ssl.get("protocol").resolve().asString(), "TLS");
    }


}
