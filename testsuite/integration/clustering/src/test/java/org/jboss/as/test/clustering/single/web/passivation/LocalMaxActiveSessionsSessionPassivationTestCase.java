/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.web.passivation;

import java.time.Duration;

import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Validates the correctness of session activation/passivation events for a distributed session manager using a local, passivating cache.
 * @author Paul Ferraro
 */
public abstract class LocalMaxActiveSessionsSessionPassivationTestCase extends LocalSessionPassivationTestCase {

    static WebArchive getBaseDeployment(String moduleName) {
        WebArchive war = LocalSessionPassivationTestCase.getBaseDeployment(moduleName);
        war.addAsWebInfResource(LocalMaxActiveSessionsSessionPassivationTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        return war;
    }

    public LocalMaxActiveSessionsSessionPassivationTestCase() {
        super(Duration.ZERO);
    }
}
