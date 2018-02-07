package org.jboss.as.test.integration.hibernate.cache;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.hibernate.cache.entity.Employee;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.logging.Logger;

import static org.jboss.as.cli.Util.DEPLOYMENT_NAME;

/**
 * Test uses JMXConnector to check the lifespan and max-idle attribute values of a specific local-cache defined for hibernate cache-container
 *
 * Test for [ JBEAP-10453 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(EntityInvalidationServerSetupTask.class)
public class EntityInvalidationCacheRefTestCase {

    private static final Logger log = Logger.getLogger(EntityInvalidationCacheRefTestCase.class.getName());

    private static final String ARCHIVE_NAME = "EntityInvalidationCacheRefTestCase";
    private static final String OBJECT_NAME = "jboss.as.expr:subsystem=infinispan,cache-container=hibernate,local-cache=entity-invalidation,component=expiration";

    @Deployment(name = DEPLOYMENT_NAME)
    public static JavaArchive createJarArchive() {
        return ShrinkWrap
                .create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addPackage(Employee.class.getPackage())
                .addAsResource("org/jboss/as/test/integration/hibernate/cache/persistence.xml", "META-INF/persistence.xml");
    }

    @Test
    @RunAsClient
    public void testReferenceEntityInvalidationCache() throws Exception {
        JMXConnector connector = null;
        try {
            JMXServiceURL address = new JMXServiceURL("service:jmx:remote+http://localhost:9990");
            connector = JMXConnectorFactory.connect(address, null);
            MBeanServerConnection mbs = connector.getMBeanServerConnection();

            // find MBean with the object name related to newly created local-cache with component=expiration
            ObjectInstance instance = mbs.getObjectInstance(new ObjectName(OBJECT_NAME));
            Assert.assertEquals(mbs.getAttribute(instance.getObjectName(), "lifespan").toString(), "300000");
            Assert.assertEquals(mbs.getAttribute(instance.getObjectName(), "maxIdle").toString(), "120000");
            return;
        } finally {
            connector.close();
        }
    }
}
