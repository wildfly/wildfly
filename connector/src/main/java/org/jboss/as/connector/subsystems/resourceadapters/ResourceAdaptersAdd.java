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

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.processors.ResourceAdaptersAttachingProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
public final class ResourceAdaptersAdd extends AbstractSubsystemAdd<ResourceAdaptersSubsystemElement> {

    private static final long serialVersionUID = -874698675049495644L;

    private ResourceAdapters resourceAdapters;

    public ResourceAdapters getDatasources() {
        return resourceAdapters;
    }

    public void setResourceAdapters(ResourceAdapters resourceAdapters) {
        this.resourceAdapters = resourceAdapters;
    }

    protected ResourceAdaptersAdd() {
        super(Namespace.CURRENT.getUriString());
    }

    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceTarget serviceTarget = updateContext.getServiceTarget();
        serviceTarget.addService( ConnectorServices.RESOURCEADAPTERS_SERVICE, new ResourceAdaptersService(resourceAdapters))
            .setInitialMode(Mode.ACTIVE)
            .install();
    }

    @Override
    protected void applyUpdateBootAction(final BootUpdateContext updateContext) {
        applyUpdate(updateContext, UpdateResultHandler.NULL, null);
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_RESOURCE_ADAPTERS, new ResourceAdaptersAttachingProcessor(resourceAdapters));
    }

    protected ResourceAdaptersSubsystemElement createSubsystemElement() {
        ResourceAdaptersSubsystemElement element = new ResourceAdaptersSubsystemElement();
        element.setResourceAdapters(resourceAdapters);
        return element;
    }

}
