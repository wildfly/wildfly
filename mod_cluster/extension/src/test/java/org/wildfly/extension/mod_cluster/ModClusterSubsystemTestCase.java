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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.mod_cluster.CommonAttributes.CONFIGURATION;
import static org.wildfly.extension.mod_cluster.CommonAttributes.MOD_CLUSTER_CONFIG;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.msc.service.ServiceController;
import org.junit.Assert;
import org.junit.Test;

/**
 * Quick versions overview:
 * <p/>
 * AS version / model version / schema version
 * 7.3.0 / 1.4.0 / 1_2
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
