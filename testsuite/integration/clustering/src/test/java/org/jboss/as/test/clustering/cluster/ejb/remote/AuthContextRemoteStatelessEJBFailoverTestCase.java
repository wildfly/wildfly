/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.concurrent.Callable;
import java.util.function.UnaryOperator;

import org.jboss.as.test.clustering.cluster.ejb.remote.bean.SecureStatelessIncrementorBean;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;

/**
 * Validates failover behavior of a remotely accessed secure @Stateless EJB.
 * @author Paul Ferraro
 */
public abstract class AuthContextRemoteStatelessEJBFailoverTestCase extends AbstractRemoteStatelessEJBFailoverTestCase {

    static final AuthenticationContext AUTHENTICATION_CONTEXT = AuthenticationContext.captureCurrent().with(
            MatchRule.ALL.matchAbstractType("ejb", "jboss"),
            AuthenticationConfiguration.empty().useName("user1").usePassword("password1")
        );

    public AuthContextRemoteStatelessEJBFailoverTestCase(String moduleName, UnaryOperator<Callable<Void>> configurator) {
        super(() -> new RemoteEJBDirectory(moduleName), SecureStatelessIncrementorBean.class, configurator);
    }
}

