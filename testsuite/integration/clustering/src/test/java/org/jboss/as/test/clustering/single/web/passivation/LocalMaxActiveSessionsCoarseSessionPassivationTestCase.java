/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.web.passivation;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;

/**
 * Validates the correctness of session passivation events for a distributed session manager using a local, passivating cache and SESSION granularity.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class LocalMaxActiveSessionsCoarseSessionPassivationTestCase extends LocalMaxActiveSessionsSessionPassivationTestCase {

    private static final String MODULE_NAME = LocalMaxActiveSessionsCoarseSessionPassivationTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, testable = false)
    public static Archive<?> deployment() {
        return getBaseDeployment(MODULE_NAME)
                .addAsWebInfResource(LocalMaxActiveSessionsSessionPassivationTestCase.class.getPackage(), "distributable-web-coarse.xml", "distributable-web.xml")
                ;
    }
}
