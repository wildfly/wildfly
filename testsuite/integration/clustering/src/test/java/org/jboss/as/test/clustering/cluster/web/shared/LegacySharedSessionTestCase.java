/*
 *
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.jboss.as.test.clustering.cluster.web.shared;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;

/**
 * Validates that web applications within an ear can share sessions if configured appropriately.
 * This test uses shared-session-config:1.0 where sessions are shared by default when WildFly is started with HA config.
 * @author Martin Simka
 */
@RunWith(Arquillian.class)
public class LegacySharedSessionTestCase extends AbstractSharedSessionTestCase {
    private static final String MODULE = LegacySharedSessionTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        return getDeployment(MODULE, "jboss-all_shared-session-config-1.0.xml");
    }
}
