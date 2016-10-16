package org.jboss.as.test.smoke;

import java.io.File;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests whether properties we rely on in the tests were properly passed to JUnit.
 *
 * @author Ondrej Zizka
 */
@RunWith(Arquillian.class)
public class TestsuiteSanityTestCase {

    private static final String[] EXPECTED_PROPS = new String[]{"jbossas.ts.submodule.dir", "jbossas.ts.integ.dir", "jbossas.ts.dir", "jbossas.project.dir", "jboss.dist", "jboss.inst"};

    @Test
    public void testSystemProperties() throws Exception {

        for (String var : EXPECTED_PROPS) {
            String path = System.getProperty(var);
            Assert.assertNotNull("Property " + var + " is not set (in container).", path);
            File dir = new File(path);
            Assert.assertTrue("Directory " + dir.getAbsolutePath() + " doesn't exist, check Surefire's system property " + var, dir.exists());
        }

    }

    @Test
    @RunAsClient
    public void testSystemPropertiesClient() throws Exception {
        for (String var : EXPECTED_PROPS) {
            String path = System.getProperty(var);
            Assert.assertNotNull("Property " + var + " is not set (outside container).", path);
            File dir = new File(path);
            Assert.assertTrue("Directory " + dir.getAbsolutePath() + " doesn't exist, check Surefire's system property " + var, dir.exists());
        }
    }

}// class

