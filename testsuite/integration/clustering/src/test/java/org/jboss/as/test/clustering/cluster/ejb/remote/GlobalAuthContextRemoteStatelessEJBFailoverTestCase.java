/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.function.UnaryOperator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.wildfly.security.auth.client.AuthenticationContext;

/**
 * Validates failover behavior of a remotely accessed secure @Stateless EJB using a global authentication context.
 * @author Farah Juma
 * @author Paul Ferraro
 */
public class GlobalAuthContextRemoteStatelessEJBFailoverTestCase extends AuthContextRemoteStatelessEJBFailoverTestCase {
    private static AuthenticationContext previousContext;

    @BeforeClass
    public static void before() {
        previousContext = AuthenticationContext.captureCurrent();
        AuthenticationContext.getContextManager().setGlobalDefault(AUTHENTICATION_CONTEXT);
    }

    @AfterClass
    public static void after() {
        AuthenticationContext.getContextManager().setGlobalDefault(previousContext);
    }

    public GlobalAuthContextRemoteStatelessEJBFailoverTestCase() {
        super(UnaryOperator.identity());
    }
}
