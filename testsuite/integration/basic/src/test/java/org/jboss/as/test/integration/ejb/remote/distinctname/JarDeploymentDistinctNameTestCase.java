/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.distinctname;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;

/**
 * Tests that the distinct-name configured in the jboss-ejb3.xml of an EJB jar deployment is taken into
 * consideration during remote EJB invocations.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JarDeploymentDistinctNameTestCase extends DistinctNameTestCase {


    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "distinct-name-in-jboss-ejb3-xml";
    private static final String MODULE_NAME = "remote-ejb-distinct-name-jar-test-case";


    @Deployment(testable = false)
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(JarDeploymentDistinctNameTestCase.class.getPackage());
        jar.addAsManifestResource(JarDeploymentDistinctNameTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }


    @Override
    protected String getAppName() {
        return APP_NAME;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    protected String getDistinctName() {
        return DISTINCT_NAME;
    }

}
