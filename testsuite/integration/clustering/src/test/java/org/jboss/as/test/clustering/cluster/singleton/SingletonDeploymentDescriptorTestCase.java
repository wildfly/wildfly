/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.clustering.cluster.singleton.servlet.TraceServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Paul Ferraro
 */
public class SingletonDeploymentDescriptorTestCase extends SingletonDeploymentTestCase {

    private static final String MODULE_NAME = SingletonDeploymentDescriptorTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".ear";

    public SingletonDeploymentDescriptorTestCase() {
        super(MODULE_NAME, DEPLOYMENT_NAME);
    }

    @Deployment(name = SINGLETON_DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = SINGLETON_DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT_NAME);
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(TraceServlet.class.getPackage());
        ear.addAsModule(war);
        ear.addAsManifestResource(SingletonDeploymentDescriptorTestCase.class.getPackage(), "singleton-deployment.xml", "singleton-deployment.xml");
        return ear;
    }
}
