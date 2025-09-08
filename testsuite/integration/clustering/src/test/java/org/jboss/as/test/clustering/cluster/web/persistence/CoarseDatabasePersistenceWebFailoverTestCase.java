/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.persistence;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;

/**
 * @author Paul Ferraro
 */
@ServerSetup({ AbstractDatabasePersistenceWebFailoverTestCase.ServerSetupTask.class, CoarseDatabasePersistenceWebFailoverTestCase.ServerSetupTask.class })
public class CoarseDatabasePersistenceWebFailoverTestCase extends AbstractDatabasePersistenceWebFailoverTestCase {

    private static final String DEPLOYMENT_NAME = CoarseDatabasePersistenceWebFailoverTestCase.class.getSimpleName() + ".war";

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
        return getDeployment(DEPLOYMENT_NAME);
    }

    public CoarseDatabasePersistenceWebFailoverTestCase() {
        super(DEPLOYMENT_NAME);
    }

    public static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(NODE_1_2_3, createContainerConfigurationBuilder()
                .setupScript(createScriptBuilder()
                        .add("/subsystem=distributable-web/infinispan-session-management=database:add(cache-container=web, cache=database-persistence, granularity=SESSION)")
                        .build())
                .tearDownScript(createScriptBuilder()
                        .add("/subsystem=distributable-web/infinispan-session-management=database:remove")
                        .build())
                .build());
        }
    }
}
