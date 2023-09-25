/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.function.UnaryOperator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessIncrementorBean;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Validates failover behavior of a remotely accessed @Stateless EJB.
 * @author Paul Ferraro
 */
public class RemoteStatelessEJBFailoverTestCase extends AbstractRemoteStatelessEJBFailoverTestCase {
    private static final String MODULE_NAME = RemoteStatelessEJBFailoverTestCase.class.getSimpleName();

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

    public RemoteStatelessEJBFailoverTestCase() {
        super(() -> new RemoteEJBDirectory(MODULE_NAME), StatelessIncrementorBean.class, UnaryOperator.identity());
    }
}
