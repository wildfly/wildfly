package org.jboss.as.test.integration.sar.context.classloader;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanTrustPermission;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.jboss.as.test.integration.sar.context.classloader.mbean.MBeanInAModuleService;
import org.jboss.as.test.integration.sar.context.classloader.mbean.MBeanInAModuleServiceMBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests that the MBean instance lifecycle has the correct TCCL set. The TCCL is expected to be the classloader of the deployment through which the MBean was deployed.
 *
 * @author: Jaikiran Pai
 * @see https://issues.jboss.org/browse/WFLY-822
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MBeanTCCLTestCase {

    private static final String EAR_NAME = "tccl-mbean-test-app";
    private static final String SAR_NAME = "tccl-mbean-test-sar";

    @ContainerResource
    private ManagementClient managementClient;

    private JMXConnector connector;

    /**
     * .ear
     * |
     * |---- META-INF/jboss-deployment-structure.xml
     * |
     * |---- .sar
     * |      |
     * |      |--- META-INF/jboss-service.xml (deploys the MBean whose class resides in a separate JBoss module)
     * |      |
     * |      |--- ClassA, ClassB, ClassC, ClassD (all of which will be attempted to be loaded from the MBean class which resides in a different JBoss module than this deployment)
     * |
     * |
     * |---- .jar (configured as a JBoss Module in jboss-deployment-structure.xml of the .ear)
     * |      |
     * |      |---- MBean class (which relies on TCCL to load the classes present in the .sar deployment)
     *
     * @return
     */
    @Deployment
    public static Archive createDeployment() {
        // create a .sar which will contain a jboss-service.xml. The actual MBean class will reside in a module which will be added as a dependency on the .sar
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, SAR_NAME + ".sar");
        // add the jboss-service.xml to the META-INF of the .sar
        sar.addAsManifestResource(MBeanInAModuleService.class.getPackage(), "tccl-test-service.xml", "jboss-service.xml");
        // add some (dummy) classes to the .sar. These classes will then be attempted to be loaded from the MBean class (which resides in a module)
        sar.addClasses(ClassAInSarDeployment.class, ClassBInSarDeployment.class, ClassCInSarDeployment.class, ClassDInSarDeployment.class);

        // now create a plain .jar containing the MBean class. This jar will be configured as a JBoss Module, in the jboss-deployment-structure.xml of the .ear to which this
        // .jar will be added
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jar-containing-mbean-class.jar");
        jar.addClasses(MBeanInAModuleService.class, MBeanInAModuleServiceMBean.class);

        // create the .ear with the .sar and the .jar and the jboss-deployment-structure.xml
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear");
        ear.addAsModule(sar);
        ear.addAsModule(jar);
        ear.addAsManifestResource(MBeanTCCLTestCase.class.getPackage(), "jboss-deployment-structure.xml", "jboss-deployment-structure.xml");

        ear.addAsManifestResource(createPermissionsXmlAsset(
                // mbean [wildfly:name=tccl-test-mbean] needs the following permission
                new MBeanTrustPermission("register"),
                // MBeanInAModuleService#testClassLoadByTCCL() needs the following permission
                new RuntimePermission("getClassLoader")),
                "permissions.xml");

        return ear;
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
        final ObjectName mbeanObjectName = new ObjectName("wildfly:name=tccl-test-mbean");
        final int num1 = 3;
        final int num2 = 4;
        // invoke the operation on MBean
        final Integer sum = (Integer) mBeanServerConnection.invoke(mbeanObjectName, "add", new Object[]{num1, num2}, new String[]{Integer.TYPE.getName(), Integer.TYPE.getName()});
        Assert.assertEquals("Unexpected return value from MBean: " + mbeanObjectName, num1 + num2, (int) sum);
    }

    private MBeanServerConnection getMBeanServerConnection() throws IOException {
        connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials());
        return connector.getMBeanServerConnection();
    }
}