/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
