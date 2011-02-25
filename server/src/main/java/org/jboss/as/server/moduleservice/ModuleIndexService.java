/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.server.moduleservice;

import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Service that caches the jandex index for system modules.
 *
 * @author Stuart Douglas
 */
public class ModuleIndexService implements Service<ModuleIndexService> {

    private final Map<ModuleIdentifier, CompositeIndex> indexes = new HashMap<ModuleIdentifier, CompositeIndex>();


    @Override
    public void start(StartContext context) throws StartException {
        // No point in throwing away the index once it is created.
        context.getController().compareAndSetMode(ServiceController.Mode.ON_DEMAND, ServiceController.Mode.ACTIVE);
    }

    @Override
    public void stop(StopContext context) {
        indexes.clear();
    }

    @Override
    public ModuleIndexService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public synchronized CompositeIndex getIndex(Module module) {
        CompositeIndex index = indexes.get(module.getIdentifier());
        if (index == null) {
            index = ModuleIndexBuilder.buildCompositeIndex(module);
            indexes.put(module.getIdentifier(), index);
        }
        return index;
    }

    public static void addService(final ServiceTarget serviceTarget) {
        Service<ModuleIndexService> service = new ModuleIndexService();
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(Services.JBOSS_MODULE_INDEX_SERVICE, service);
        serviceBuilder.install();
    }
}
