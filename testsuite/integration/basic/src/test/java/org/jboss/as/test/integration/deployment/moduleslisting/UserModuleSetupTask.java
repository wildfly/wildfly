/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.moduleslisting;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.module.util.TestModule;

public class UserModuleSetupTask implements ServerSetupTask {
    private volatile TestModule testModule;

    @Override
    public void setup(ManagementClient arg0, String arg1) throws Exception {
        testModule = new TestModule(DeploymentModulesListTestCase.TEST_MODULE);
        testModule.create();
    }

    @Override
    public void tearDown(ManagementClient arg0, String arg1) throws Exception {
        if (testModule != null) {
            testModule.remove();
        }
    }
}
