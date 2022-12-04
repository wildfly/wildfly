/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.single.infinispan;

import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.single.infinispan.cdi.embedded.GreetingCacheManagerTestCase;
import org.jboss.as.test.clustering.single.infinispan.cdi.embedded.GreetingServiceTestCase;
import org.jboss.as.test.clustering.single.infinispan.cdi.remote.RemoteGreetingServiceTestCase;
import org.jboss.as.test.clustering.single.infinispan.query.ContainerManagedHotRodClientTestCase;
import org.jboss.as.test.clustering.single.infinispan.query.HotRodClientTestCase;
import org.jboss.as.test.clustering.single.infinispan.query.ContainerRemoteQueryTestCase;
import org.jboss.as.test.clustering.single.infinispan.query.RemoteQueryTestCase;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * JUnit suite class to support testable deployments - tests running in-container - typically to verify integration with Infinispan Modules.
 * This avoids the problem of having the Infinispan Server test driver JUnit TestRule be run e.g. in-container.
 * Using suite in conjunction with the @ClassRule starts the Infinispan server prior to running the tests in-container,
 * as a side effect makes the execution pretty fast.
 * NOTE: This doesn't work with -Dtest=... ask the @RunWith(Suite.class) is not in effect thus the @ClassRule does not invoke!
 * For a workaround, to run a single test, comment out the undesired classes in this file.
 *
 * @author Radoslav Husar
 * @since 27
 */
@RunWith(Suite.class)
@SuiteClasses({
        HotRodClientTestCase.class,
        RemoteQueryTestCase.class,
        ContainerRemoteQueryTestCase.class,
        RemoteGreetingServiceTestCase.class,
        GreetingCacheManagerTestCase.class,
        GreetingServiceTestCase.class,
        ContainerManagedHotRodClientTestCase.class,
})
public class InfinispanModulesTestSuite {

    @ClassRule
    public static final TestRule INFINISPAN_SERVER_RULE = InfinispanServerUtil.INFINISPAN_SERVER_RULE;

}