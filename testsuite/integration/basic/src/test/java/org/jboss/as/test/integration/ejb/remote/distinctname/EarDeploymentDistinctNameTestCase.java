/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.distinctname;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;

/**
 * Tests that the distinct-name configured in the jboss-app.xml of an EAR deployment is taken into
 * consideration during remote EJB invocations.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarDeploymentDistinctNameTestCase extends DistinctNameTestCase {

    private static final String APP_NAME = "remote-ejb-distinct-name";
    private static final String DISTINCT_NAME = "distinct-name-in-jboss-app-xml";
    private static final String MODULE_NAME = "remote-ejb-distinct-name-ear-test-case";

    @Deployment(testable = false)
    public static EnterpriseArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(EarDeploymentDistinctNameTestCase.class.getPackage());

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        ear.addAsModule(jar);
        ear.addAsManifestResource(EarDeploymentDistinctNameTestCase.class.getPackage(), "jboss-app.xml", "jboss-app.xml");

        return ear;
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
