/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

/**
 * Validates failover behavior of a remotely accessed secure @Stateless EJB using a thread authentication context.
 * @author Paul Ferraro
 */
public class ThreadAuthContextRemoteStatelessEJBFailoverTestCase extends AuthContextRemoteStatelessEJBFailoverTestCase {

    public ThreadAuthContextRemoteStatelessEJBFailoverTestCase() {
        super(task -> () -> AUTHENTICATION_CONTEXT.runCallable(task));
    }
}

