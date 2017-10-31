package org.jboss.as.test.integration.sar;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.java.permission.JndiPermission;

/**
 * Tests that MBean(s) binding to JNDI in their <code>start</code> lifecycle method do not hang up the deployment.
 *
 * @see https://developer.jboss.org/thread/251092
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JNDIBindingMBeanTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "multiple-jndi-binding-mbeans.sar");
        sar.addClasses(JNDIBindingService.class, JNDIBindingMBeanTestCase.class, JNDIBindingServiceMBean.class);
        sar.addAsManifestResource(JNDIBindingMBeanTestCase.class.getPackage(), "multiple-jndi-binding-mbeans-jboss-service.xml", "jboss-service.xml");
        sar.addAsManifestResource(createPermissionsXmlAsset(new JndiPermission("global/env/foo/-", "bind,unbind")), "permissions.xml");
        return sar;
    }

    /**
     * Makes sure that the MBeans that are expected to be up and running are accessible.
     *
     * @throws Exception
     * @see https://developer.jboss.org/thread/251092
     */
    @Test
    public void testMBeanStartup() throws Exception {
        // get mbean server
        final JMXConnector connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials());
        try {
            final MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();
            // check the deployed MBeans
            for (int i = 1; i <= 9; i++) {
                final String mbeanName = "jboss:name=mbean-startup-jndi-bind-" + i;
                final Object instance = mBeanServerConnection.getObjectInstance(new ObjectName(mbeanName));
                Assert.assertNotNull("No instance returned for MBean: " + mbeanName, instance);
            }
        } finally {
            connector.close();
        }
    }

}
