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
package org.jboss.as.core.model.bridge.impl;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.AbstractKernelServicesImpl;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.LegacyModelInitializerEntry;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.TestParser;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.RestartMode;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLMapper;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChildFirstClassLoaderKernelServicesFactory {

    public static KernelServices create(List<ModelNode> bootOperations, boolean validateOperations, ModelVersion legacyModelVersion,
            List<LegacyModelInitializerEntry> modelInitializerEntries) throws Exception {

        TestModelType type = TestModelType.DOMAIN;
        XMLMapper xmlMapper = XMLMapper.Factory.create();
        TestParser testParser = TestParser.create(null, xmlMapper, type);
        ModelInitializer modelInitializer = null;
        if (modelInitializerEntries != null && modelInitializerEntries.size() > 0) {
            modelInitializer = new LegacyModelInitializer(modelInitializerEntries);
        }

        RunningModeControl runningModeControl = new HostRunningModeControl(RunningMode.ADMIN_ONLY, RestartMode.HC_ONLY);
        ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, runningModeControl);
        return AbstractKernelServicesImpl.create(ProcessType.HOST_CONTROLLER, runningModeControl, validateOperations, bootOperations, testParser, legacyModelVersion, type, modelInitializer, extensionRegistry, null);
    }

    private static class LegacyModelInitializer implements ModelInitializer {

        private final List<LegacyModelInitializerEntry> entries;

        LegacyModelInitializer(List<LegacyModelInitializerEntry> entries) {
            this.entries = entries;
        }

        @Override
        public void populateModel(Resource rootResource) {
            for (LegacyModelInitializerEntry entry : entries) {
                Resource parent = rootResource;
                if (entry.getParentAddress() != null && entry.getParentAddress().size() > 0) {
                    for (PathElement element : entry.getParentAddress()) {
                        parent = rootResource.getChild(element);
                        if (parent == null) {
                            throw new IllegalStateException("No parent at " + element);
                        }
                    }
                }
                Resource resource = Resource.Factory.create();
                if (entry.getModel() != null) {
                    resource.getModel().set(entry.getModel());
                }
                parent.registerChild(entry.getRelativeResourceAddress(), resource);
            }
        }

    }
}
