/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.jmx.rbac;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({JmxAccessFromSecuredDeploymentWithRbacUsingIdentityRolesTestCase.RbacWithUseIdenityRolesSetup.class})
public class JmxAccessFromSecuredDeploymentWithRbacUsingIdentityRolesTestCase extends AbstractJmxAccessFromDeploymentWithRbacTest {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return AbstractJmxAccessFromDeploymentWithRbacTest.deploy(true);
    }

    static class RbacWithUseIdenityRolesSetup extends AbstractJmxAccessFromDeploymentWithRbacTest.EnableRbacSetupTask {
        public RbacWithUseIdenityRolesSetup() {
            super(true, true);
        }
    }
}
