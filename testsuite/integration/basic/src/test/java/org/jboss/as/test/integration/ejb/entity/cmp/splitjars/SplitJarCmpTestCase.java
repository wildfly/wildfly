package org.jboss.as.test.integration.ejb.entity.cmp.splitjars;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.as.test.integration.ejb.entity.cmp.TableCleaner;
import org.jboss.as.test.integration.ejb.entity.cmp.simple.Simple;
import org.jboss.as.test.integration.ejb.entity.cmp.simple.SimpleCMPUnitTestCase;
import org.jboss.as.test.integration.ejb.entity.cmp.simple.SimpleHome;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.fail;

@RunWith(CmpTestRunner.class)
public class SplitJarCmpTestCase extends AbstractCmpTest {
    private static Logger log = Logger.getLogger(SplitJarCmpTestCase.class);

    private Simple simple;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "cmp2-lib.jar");
        libJar.addPackage(SimpleCMPUnitTestCase.class.getPackage());

        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "cmp2-ejb.jar");
        ejbJar.addAsManifestResource(SimpleCMPUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        ejbJar.addAsManifestResource(SimpleCMPUnitTestCase.class.getPackage(), "jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        ejbJar.addClass(SplitJarCmpTestCase.class);
        AbstractCmpTest.addDeploymentAssets(ejbJar);
        ejbJar.delete("META-INF/services/" + ServiceActivator.class.getName());

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "cmp2-split.ear");
        ear.addAsLibrary(libJar);
        ear.addAsModule(ejbJar);
        ear.addAsServiceProvider(ServiceActivator.class, TableCleaner.class);

        return ear;
    }

    private SimpleHome getSimpleHome() {
        try {
            return (SimpleHome) getInitialContext().lookup("java:app/cmp2-ejb/SimpleEJB!org.jboss.as.test.integration.ejb.entity.cmp.simple.SimpleHome");
        } catch (Exception e) {
            log.error("failed", e);
            fail("Exception in getSimpleHome: " + e.getMessage());
        }
        return null;
    }

    @Test
    public void testBooleanPrimitive() throws Exception {
        simple.getBooleanPrimitive();
    }



    public void setUpEjb() throws Exception {
        SimpleHome simpleHome = getSimpleHome();

        boolean wasCreated = false;
        try {
            simple = simpleHome.findByPrimaryKey("simple");
        } catch (Exception e) {
        }

        if (simple == null) {
            simple = simpleHome.create("simple");
            wasCreated = true;
        }

        simple.setBooleanPrimitive(true);
    }

    public void tearDownEjb() throws Exception {
        simple.remove();
    }
}
