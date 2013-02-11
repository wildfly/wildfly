/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.subsystem.bridge.impl;

import java.util.List;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.model.test.ModelTestParser;
import org.jboss.as.subsystem.test.AbstractKernelServicesImpl;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.TestParser;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChildFirstClassLoaderKernelServicesFactory {

    public static KernelServices create(String mainSubsystemName, String extensionClassName, AdditionalInitialization additionalInit,
            List<ModelNode> bootOperations, ModelVersion legacyModelVersion, boolean persistXml) throws Exception {
        Extension extension = (Extension) Class.forName(extensionClassName).newInstance();

        ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.DOMAIN_SERVER, new RunningModeControl(RunningMode.ADMIN_ONLY));
        ModelTestParser testParser = new TestParser(mainSubsystemName, extensionRegistry);

        //TODO this should get serialized properly
        if (additionalInit == null) {
            additionalInit = AdditionalInitialization.MANAGEMENT;
        }
        return AbstractKernelServicesImpl.create(mainSubsystemName, additionalInit, extensionRegistry, bootOperations, testParser, extension, legacyModelVersion, false, persistXml);
    }
}
