/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.config.MCMPHandlerConfiguration;
import org.jboss.modcluster.config.SSLConfiguration;
import org.jboss.msc.service.ServiceController;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
@RunWith(Parameterized.class)
public class ModClusterSubsystemParsingTestCase extends ClusteringSubsystemTest {

    private final ModClusterSchema schema;
    private final int expectedOperationCount;

    public ModClusterSubsystemParsingTestCase(ModClusterSchema schema, int expectedOperationCount) {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension(), String.format("subsystem_%d_%d.xml", schema.major(), schema.minor()));
        this.schema = schema;
        this.expectedOperationCount = expectedOperationCount;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
                { ModClusterSchema.MODCLUSTER_1_0, 13 },
                { ModClusterSchema.MODCLUSTER_1_1, 13 },
                { ModClusterSchema.MODCLUSTER_1_2, 15 },
                { ModClusterSchema.MODCLUSTER_2_0, 15 },
                { ModClusterSchema.MODCLUSTER_3_0, 15 },
        };
        return Arrays.asList(data);
    }

    /**
     * Tests that the xml is parsed into the correct operations.
     */
    @Test
    public void testParseSubsystem() throws Exception {
        List<ModelNode> operations = this.parse(this.getSubsystemXml());

        Assert.assertEquals(this.expectedOperationCount, operations.size());
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return String.format("schema/jboss-as-mod-cluster_%d_%d.xsd", schema.major(), schema.minor());
    }

    @Test
    public void testSubsystemWithSimpleLoadProvider() throws Exception {
        if (schema != ModClusterSchema.CURRENT) return;

        super.standardSubsystemTest("subsystem_2_0_simple-load-provider.xml");
    }

    @Ignore
    @Test
    public void testSSL() throws Exception {
        if (schema != ModClusterSchema.CURRENT) return;

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
        MCMPHandlerConfiguration mcmpHandlerConfiguration = (MCMPHandlerConfiguration) service.getValue();
        Assert.assertTrue(mcmpHandlerConfiguration.isSsl());
        SSLConfiguration sslConfig = (SSLConfiguration) service.getValue();
        Assert.assertEquals("mykeyalias", sslConfig.getSslKeyAlias());
        Assert.assertEquals("mypassword", sslConfig.getSslTrustStorePassword());
        Assert.assertEquals("mypassword", sslConfig.getSslKeyStorePassword());
        Assert.assertEquals("/home/rhusar/client-keystore.jks", sslConfig.getSslKeyStore());
        Assert.assertEquals("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_RC4_128_MD5,SSL_RSA_WITH_RC4_128_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA", sslConfig.getSslCiphers());
        Assert.assertEquals("TLSv1", sslConfig.getSslProtocol());
        Assert.assertEquals("/home/rhusar/client-keystore.jks", sslConfig.getSslTrustStore());
        Assert.assertEquals("/home/rhusar/revocations", sslConfig.getSslCrlFile());
    }

}
