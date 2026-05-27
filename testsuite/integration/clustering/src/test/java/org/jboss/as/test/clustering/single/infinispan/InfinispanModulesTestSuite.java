/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan;

import org.infinispan.server.test.core.TestServer;
import org.infinispan.server.test.junit5.InfinispanServerExtension;
import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.single.infinispan.cdi.embedded.GreetingCacheManagerTestCase;
import org.jboss.as.test.clustering.single.infinispan.cdi.embedded.GreetingServiceTestCase;
import org.jboss.as.test.clustering.single.infinispan.cdi.remote.RemoteGreetingServiceTestCase;
import org.jboss.as.test.clustering.single.infinispan.query.ContainerManagedHotRodClientTestCase;
import org.jboss.as.test.clustering.single.infinispan.query.ContainerRemoteQueryTestCase;
import org.jboss.as.test.clustering.single.infinispan.query.HotRodClientTestCase;
import org.jboss.as.test.clustering.single.infinispan.query.RemoteQueryTestCase;
import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit suite class to support testable deployments - tests running in-container - typically to verify integration with
 * Infinispan Modules. This avoids the problem of having the Infinispan Server test driver JUnit extension be run e.g.
 * in-container. Using suite starts the Infinispan server prior to running the tests in-container, as a side effect makes
 * the execution pretty fast.
 *
 * @author Radoslav Husar
 * @since 27
 */
@Suite
@SelectClasses({
        HotRodClientTestCase.class,
        RemoteQueryTestCase.class,
        ContainerRemoteQueryTestCase.class,
        RemoteGreetingServiceTestCase.class,
        GreetingCacheManagerTestCase.class,
        GreetingServiceTestCase.class,
        ContainerManagedHotRodClientTestCase.class,
})
public class InfinispanModulesTestSuite {

    private static final String TEST_NAME = InfinispanModulesTestSuite.class.getName();
    private static final InfinispanServerExtension SERVER = InfinispanServerUtil.infinispanServerExtension();

    @BeforeSuite
    static void startInfinispanServer() {
        TestServer testServer = SERVER.getTestServer();
        testServer.initServerDriver();
        testServer.getDriver().prepare(TEST_NAME);
        testServer.beforeListeners();
        testServer.enhanceConfiguration();
        testServer.getDriver().start(TEST_NAME);
    }

    @AfterSuite
    static void stopInfinispanServer() {
        TestServer testServer = SERVER.getTestServer();
        if (testServer.isDriverInitialized()) {
            testServer.stopServerDriver(TEST_NAME);
            testServer.afterListeners();
        }
    }
}
