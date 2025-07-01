/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke;

import java.io.File;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests whether properties we rely on in the tests were properly passed to JUnit.
 *
 * @author Ondrej Zizka
 */
@ExtendWith(ArquillianExtension.class)
public class TestsuiteSanityTestCase {

    private static final String[] EXPECTED_PROPS = new String[]{"jbossas.ts.submodule.dir", "jbossas.ts.integ.dir", "jbossas.ts.dir", "jbossas.project.dir", "jboss.dist", "jboss.inst"};

    @Test
    public void testSystemProperties() throws Exception {

        for (String var : EXPECTED_PROPS) {
            String path = System.getProperty(var);
            Assertions.assertNotNull(path, "Property " + var + " is not set (in container).");
            File dir = new File(path);
            Assertions.assertTrue(dir.exists(), "Directory " + dir.getAbsolutePath() + " doesn't exist, check Surefire's system property " + var);
        }

    }

    @Test
    @RunAsClient
    public void testSystemPropertiesClient() throws Exception {
        for (String var : EXPECTED_PROPS) {
            String path = System.getProperty(var);
            Assertions.assertNotNull(path, "Property " + var + " is not set (outside container).");
            File dir = new File(path);
            Assertions.assertTrue(dir.exists(), "Directory " + dir.getAbsolutePath() + " doesn't exist, check Surefire's system property " + var);
        }
    }

}// class

