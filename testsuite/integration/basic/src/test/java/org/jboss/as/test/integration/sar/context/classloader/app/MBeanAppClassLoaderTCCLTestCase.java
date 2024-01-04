/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.sar.context.classloader.app;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import java.io.BufferedReader;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

@RunWith(Arquillian.class)
@RunAsClient
public class MBeanAppClassLoaderTCCLTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    private JMXConnector connector;

    private static final String SAR_NAME = "test-app-tccl.sar";

    static Path outputPath = Path.of("target/app-cl" + System.currentTimeMillis() + "/tccl.properties");

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, SAR_NAME);
        // add the jboss-service.xml to the META-INF of the .sar
        sar.addAsManifestResource(MBeanAppClassLoaderTCCLCheckService.class.getPackage(), "tccl-app-test-service.xml", "jboss-service.xml");
        // add some (dummy) classes to the .sar. These classes will then be attempted to be loaded from the MBean class (which resides in a module)
        sar.addClasses(MBeanAppClassLoaderTCCLCheckService.class, MBeanAppClassLoaderTCCLCheckServiceMBean.class);
        sar.addAsManifestResource(createPermissionsXmlAsset(
                new FilePermission(outputPath.toAbsolutePath().toString(), "read"),
                new FilePermission(outputPath.toAbsolutePath().toString(), "write")
        ), "permissions.xml");

        return sar;
    }

    @After
    public void closeConnector() {
        IoUtils.safeClose(connector);
    }

    /**
     * Tests the MBean was deployed successfully and can be invoked. The fact that the MBean deployed successfully is a sign that the TCCL access from within the MBean code, worked fine
     *
     * @throws Exception
     */
    @Test
    public void testTCCLInMBeanInvocation() throws Exception {
        final MBeanServerConnection mBeanServerConnection = this.getMBeanServerConnection();
        final ObjectName mbeanObjectName = new ObjectName("wildfly:name=tccl-app-test-mbean");

        try {
            Files.createDirectories(outputPath.getParent());
            Files.createFile(outputPath);
            mBeanServerConnection.setAttribute(mbeanObjectName, new Attribute("File", outputPath.toAbsolutePath().toString()));
            mBeanServerConnection.getAttribute(mbeanObjectName, "File");
            mBeanServerConnection.invoke(mbeanObjectName, "method", new Object[0], new String[0]);

            // Undeploy so that the stop and destroy methods get called
            ModelNode removeOp = Util.createRemoveOperation(PathAddress.pathAddress("deployment", SAR_NAME));
            managementClient.getControllerClient().execute(removeOp);

            try (BufferedReader br = new BufferedReader(new FileReader(outputPath.toFile()))) {
                Properties props = new Properties();
                props.load(br);
                String appCl = props.getProperty(MBeanAppClassLoaderTCCLCheckService.APP_CL);
                Assert.assertNotNull(MBeanAppClassLoaderTCCLCheckService.APP_CL, appCl);

                checkProperty(props, MBeanAppClassLoaderTCCLCheckService.CONSTRUCTOR_TCCL, appCl);
                checkProperty(props, MBeanAppClassLoaderTCCLCheckService.CREATE_TCCL, appCl);
                checkProperty(props, MBeanAppClassLoaderTCCLCheckService.START_TCCL, appCl);
                checkProperty(props, MBeanAppClassLoaderTCCLCheckService.ATTR_WRITE_TCCL, appCl);
                checkProperty(props, MBeanAppClassLoaderTCCLCheckService.ATTR_READ_TCCL, appCl);
                checkProperty(props, MBeanAppClassLoaderTCCLCheckService.INVOKE_TCCL, appCl);
                checkProperty(props, MBeanAppClassLoaderTCCLCheckService.STOP_TCCL, appCl);
                checkProperty(props, MBeanAppClassLoaderTCCLCheckService.DESTROY_TCCL, appCl);
            }
        } finally {
            Files.delete(outputPath);
            Files.delete(outputPath.getParent());
        }
    }

    private void checkProperty(Properties props, String key, String expected) {
        Object value = props.get(key);
        Assert.assertNotNull(key, value);
        Assert.assertEquals(key, expected, value);
    }

    private MBeanServerConnection getMBeanServerConnection() throws IOException {
        connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials());
        return connector.getMBeanServerConnection();
    }
}