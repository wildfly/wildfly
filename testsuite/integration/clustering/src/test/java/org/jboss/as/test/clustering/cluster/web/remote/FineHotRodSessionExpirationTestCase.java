/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Paul Ferraro
 */
@ServerSetup({ InfinispanServerSetupTask.class })
public class FineHotRodSessionExpirationTestCase extends AbstractHotRodSessionExpirationTestCase {

    private static final String MODULE_NAME = FineHotRodSessionExpirationTestCase.class.getSimpleName();

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
        return getBaseDeployment(MODULE_NAME)
                .addAsWebInfResource(FineHotRodSessionExpirationTestCase.class.getPackage(), "jboss-all_fine.xml", "jboss-all.xml")
                .addAsWebInfResource(FineHotRodSessionExpirationTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                ;
    }
}
