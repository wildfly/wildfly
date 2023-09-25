/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.concurrent.Callable;
import java.util.function.UnaryOperator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.SecureStatelessIncrementorBean;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.shrinkwrap.api.Archive;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;

/**
 * Validates failover behavior of a remotely accessed secure @Stateless EJB.
 * @author Paul Ferraro
 */
public abstract class AuthContextRemoteStatelessEJBFailoverTestCase extends AbstractRemoteStatelessEJBFailoverTestCase {
    private static final String MODULE_NAME = AuthContextRemoteStatelessEJBFailoverTestCase.class.getSimpleName();

    static final AuthenticationContext AUTHENTICATION_CONTEXT = AuthenticationContext.captureCurrent().with(
            MatchRule.ALL.matchAbstractType("ejb", "jboss"),
            AuthenticationConfiguration.empty().useName("user1").usePassword("password1")
        );

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

    public AuthContextRemoteStatelessEJBFailoverTestCase(UnaryOperator<Callable<Void>> configurator) {
        super(() -> new RemoteEJBDirectory(MODULE_NAME), SecureStatelessIncrementorBean.class, configurator);
    }
}

