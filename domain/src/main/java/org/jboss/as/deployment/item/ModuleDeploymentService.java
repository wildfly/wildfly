/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment.item;

import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleDeploymentService implements Service<Module> {

    private final ModuleDeploymentItem moduleDeploymentItem;

    public ModuleDeploymentService(final ModuleDeploymentItem moduleDeploymentItem) {
        this.moduleDeploymentItem = moduleDeploymentItem;
    }

    public void start(final StartContext context) throws StartException {
//        final ModuleSpec spec = new ModuleSpec(moduleDeploymentItem.getIdentifier());
//        final ModuleContentLoader.Builder builder = ModuleContentLoader.build();
//        for (ModuleDeploymentItem.Resource resource : moduleDeploymentItem.getResources()) {
//            builder.add(resource.getRootName(), new VFSResourceLoader());
//        }
//        spec.setContentLoader(new ModuleContentLoader());
    }

    public void stop(final StopContext context) {
    }

    public Module getValue() throws IllegalStateException {
        return null;
    }
}
