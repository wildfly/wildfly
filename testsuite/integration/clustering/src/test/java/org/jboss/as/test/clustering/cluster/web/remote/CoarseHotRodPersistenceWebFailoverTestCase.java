/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@ServerSetup({ InfinispanServerSetupTask.class, AbstractHotRodPersistenceWebFailoverTestCase.ServerSetupTask.class })
public class CoarseHotRodPersistenceWebFailoverTestCase extends AbstractHotRodPersistenceWebFailoverTestCase {

    private static final String DEPLOYMENT_NAME = CoarseHotRodPersistenceWebFailoverTestCase.class.getSimpleName() + ".war";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_3)
    public static Archive<?> deployment3() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        return getDeployment(DEPLOYMENT_NAME, "distributable-web-coarse.xml");
    }

    public CoarseHotRodPersistenceWebFailoverTestCase() {
        super(DEPLOYMENT_NAME);
    }
}
