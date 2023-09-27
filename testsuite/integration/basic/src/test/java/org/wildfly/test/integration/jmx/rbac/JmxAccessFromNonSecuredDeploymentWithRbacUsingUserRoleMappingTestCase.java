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
@ServerSetup({JmxAccessFromNonSecuredDeploymentWithRbacUsingUserRoleMappingTestCase.RbacWithUseIdenityRolesSetup.class})
public class JmxAccessFromNonSecuredDeploymentWithRbacUsingUserRoleMappingTestCase extends AbstractJmxAccessFromDeploymentWithRbacTest {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return AbstractJmxAccessFromDeploymentWithRbacTest.deploy(false);
    }

    public JmxAccessFromNonSecuredDeploymentWithRbacUsingUserRoleMappingTestCase() {
        super(false);
    }

    static class RbacWithUseIdenityRolesSetup extends EnableRbacSetupTask {
        public RbacWithUseIdenityRolesSetup() {
            super(false, false);
        }
    }
}
