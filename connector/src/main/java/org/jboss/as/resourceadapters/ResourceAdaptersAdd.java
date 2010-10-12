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

package org.jboss.as.resourceadapters;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
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

    @Override
    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        final BatchBuilder builder = updateContext.getBatchBuilder();
        final ResourceAdaptersService raService = new ResourceAdaptersService(resourceAdapters);
        final BatchServiceBuilder<ResourceAdapters> serviceBuilder = builder.addService(
                ResourceAdaptersServices.RESOURCEADAPTERS_SERVICE, raService);
        serviceBuilder.setInitialMode(Mode.ON_DEMAND);

    }

    protected ResourceAdaptersSubsystemElement createSubsystemElement() {
        return new ResourceAdaptersSubsystemElement();
    }

}
