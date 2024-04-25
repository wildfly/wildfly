/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.session;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.singleton.dependson.mdb.CallCounterInterface;
import org.jboss.as.test.module.util.ModuleBuilder;
import org.jboss.as.test.shared.ModuleUtils;

/**
 * @author baranowb
 */
public abstract class SetupModuleServerSetupTask implements ServerSetupTask {

    private final String moduleName;
    private volatile Runnable testModule;

    protected SetupModuleServerSetupTask(final String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public void setup(ManagementClient arg0, String arg1) throws Exception {
        testModule = ModuleBuilder.of(moduleName)
                .addClasses(CallCounterInterface.class, Trigger.class)
                .addDependencies(ModuleUtils.EE_DEPENDENCIES)
                .build();

    }


    @Override
    public void tearDown(ManagementClient arg0, String arg1) throws Exception {
        if (testModule != null) {
            testModule.run();
        }
    }
}
