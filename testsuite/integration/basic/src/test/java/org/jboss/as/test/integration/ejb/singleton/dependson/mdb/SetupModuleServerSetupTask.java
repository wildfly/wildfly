/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ModuleUtils;

/**
 * @author baranowb
 */
public class SetupModuleServerSetupTask implements ServerSetupTask {

    private volatile TestModule testModule;

    @Override
    public void setup(ManagementClient arg0, String arg1) throws Exception {
        testModule = ModuleUtils.createTestModuleWithEEDependencies(Constants.TEST_MODULE_NAME);
        testModule.addResource("module.jar").addClass(CallCounterInterface.class);
        testModule.create();
    }

    @Override
    public void tearDown(ManagementClient arg0, String arg1) throws Exception {
        if (testModule != null) {
            testModule.remove();
        }
    }

}
