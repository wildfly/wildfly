/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.web.expiration;

import org.infinispan.transaction.TransactionMode;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class CoarseSessionExpirationTestCase extends SessionExpirationTestCase {

    private static final String MODULE_NAME = CoarseSessionExpirationTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    static WebArchive getDeployment() {
        return getBaseDeployment(MODULE_NAME).addAsWebInfResource(SessionExpirationTestCase.class.getPackage(), "jboss-web-coarse.xml", "jboss-web.xml");
    }

    public CoarseSessionExpirationTestCase() {
        super(TransactionMode.TRANSACTIONAL);
    }
}
