/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.commands;


import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import javax.management.MBeanServer;

/**
 * Call MBean method dumpAllModuleInformation to get module information for local modules
 *
 * @author Brad Maxwell
 */
public class LocalModuleDependencies extends JdrCommand {

    private static String OUTPUT_FILE = "local-module-dependencies.txt";

    @Override
    public void execute() throws Exception {
        if(!this.env.isServerRunning())
            return;

        StringBuilder buffer = new StringBuilder();
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();

        // see if we can get rid of the -* number
        // jboss.modules:type=ModuleLoader,name=LocalModuleLoader-2  - String dumpAllModuleInformation
        // jboss.modules:type=ModuleLoader,name=ServiceModuleLoader-3 - String dumpAllModuleInformation

        ObjectName base = new ObjectName("jboss.modules:type=ModuleLoader,name=LocalModuleLoader-*");
        for(ObjectName localModuleLoader : platformMBeanServer.queryNames(base, null)) {
            buffer.append( (String) platformMBeanServer.invoke(localModuleLoader, "dumpAllModuleInformation", null, null) );
        }
        this.env.getZip().add(buffer.toString(), OUTPUT_FILE);
    }
}
