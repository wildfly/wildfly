/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Validates failover behavior of a remotely accessed secure @Stateless EJB using a thread authentication context.
 * @author Paul Ferraro
 */
public class ThreadAuthContextRemoteStatelessEJBFailoverTestCase extends AuthContextRemoteStatelessEJBFailoverTestCase {
    private static final String MODULE_NAME = ThreadAuthContextRemoteStatelessEJBFailoverTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment(MODULE_NAME);
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment(MODULE_NAME);
    }

    public ThreadAuthContextRemoteStatelessEJBFailoverTestCase() {
        super(MODULE_NAME, task -> () -> AUTHENTICATION_CONTEXT.runCallable(task));
    }
}

