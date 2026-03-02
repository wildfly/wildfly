/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.function.UnaryOperator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.wildfly.security.auth.client.AuthenticationContext;

/**
 * Validates failover behavior of a remotely accessed secure @Stateless EJB using a global authentication context.
 * @author Farah Juma
 * @author Paul Ferraro
 */
public class GlobalAuthContextRemoteStatelessEJBFailoverTestCase extends AuthContextRemoteStatelessEJBFailoverTestCase {
    private static AuthenticationContext previousContext;

    @BeforeAll
    static void before() {
        previousContext = AuthenticationContext.captureCurrent();
        AuthenticationContext.getContextManager().setGlobalDefault(AUTHENTICATION_CONTEXT);
    }

    @AfterAll
    static void after() {
        AuthenticationContext.getContextManager().setGlobalDefault(previousContext);
    }

    public GlobalAuthContextRemoteStatelessEJBFailoverTestCase() {
        super(UnaryOperator.identity());
    }
}
