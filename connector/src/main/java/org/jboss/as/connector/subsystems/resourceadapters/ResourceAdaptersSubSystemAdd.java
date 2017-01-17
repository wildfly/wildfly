/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.CopyOnWriteArrayListMultiMap;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Handler for adding the resource adapters subsystem.
 *
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 * @author John Bailey
 */
class ResourceAdaptersSubsystemAdd extends AbstractAddStepHandler {

    static final ResourceAdaptersSubsystemAdd INSTANCE = new ResourceAdaptersSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
        model.get(RESOURCEADAPTER_NAME);
    }


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final Resource subsystemResource = context.readResourceFromRoot(PathAddress.pathAddress(ResourceAdaptersExtension.SUBSYSTEM_PATH));
        ResourceAdaptersSubsystemService service = new ResourceAdaptersSubsystemService();
        CopyOnWriteArrayListMultiMap<String, ServiceName> value = service.getValue();
        for (Resource.ResourceEntry re : subsystemResource.getChildren(RESOURCEADAPTER_NAME)) {
            value.putIfAbsent(re.getModel().get(ARCHIVE.getName()).asString(), ConnectorServices.RA_SERVICE.append(re.getName()));
        }
        final ServiceBuilder<?> builder = context.getServiceTarget().addService(ConnectorServices.RESOURCEADAPTERS_SUBSYSTEM_SERVICE, service);
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();

    }
}
