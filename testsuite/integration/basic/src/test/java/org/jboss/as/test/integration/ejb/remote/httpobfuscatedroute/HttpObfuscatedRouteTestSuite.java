/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.httpobfuscatedroute;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.arquillian.api.ServerSetup;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runners.Suite;

/**
 * Test suite for running some http invocation tests with Undertow obfuscated-session-route set to true.
 *
 * @author Flavia Rainone
 */
@RunAsClient
@Suite.SuiteClasses({HttpObfuscatedRouteTestSuite.HttpRemoteEJBJndiBasedInvocationTest.class,
                     HttpObfuscatedRouteTestSuite.HttpRemoteIdentityTestCase.class})
public class HttpObfuscatedRouteTestSuite {

    private static Throwable failure = null;

    @ServerSetup(UndertowObfuscatedSessionRouteServerSetup.class)
    public static class HttpRemoteEJBJndiBasedInvocationTest extends org.jboss.as.test.integration.ejb.remote.jndi.HttpRemoteEJBJndiBasedInvocationTestCase {
        @BeforeClass
        @AfterClass
        public static void checkFailure() throws Throwable {
            if (failure != null) {
                throw failure;
            }
        }
    }

    @ServerSetup(UndertowObfuscatedSessionRouteServerSetup.class)
    public static class HttpRemoteIdentityTestCase extends org.jboss.as.test.integration.ejb.remote.security.HttpRemoteIdentityTestCase {
        @BeforeClass
        @AfterClass
        public static void checkFailure() throws Throwable {
            if (failure != null) {
                throw failure;
            }
        }
    }
}