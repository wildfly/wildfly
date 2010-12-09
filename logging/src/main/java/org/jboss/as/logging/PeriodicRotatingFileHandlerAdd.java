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

package org.jboss.as.logging;

import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class PeriodicRotatingFileHandlerAdd extends FileHandlerAdd {

    private static final long serialVersionUID = 3144252544518106859L;

    private String suffix;

    public PeriodicRotatingFileHandlerAdd(final String name) {
        super(name);
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(final String suffix) {
        this.suffix = suffix;
    }

    protected AbstractHandlerElement<?> createElement(final String name) {
        final PeriodicRotatingFileHandlerElement element = new PeriodicRotatingFileHandlerElement(name);
        element.setSuffix(suffix);
        element.setPath(getRelativeTo(), getPath());
        return element;
    }

    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        try {
            final BatchBuilder batchBuilder = updateContext.getServiceTarget();
            final PeriodicRotatingFileHandlerService service = new PeriodicRotatingFileHandlerService();
            final ServiceBuilder<Handler> serviceBuilder = batchBuilder.addService(LogServices.handlerName(getName()), service);
            final String relativeTo = getRelativeTo();
            if (relativeTo != null) {
                serviceBuilder.addDependency(AbstractPathService.pathNameOf(relativeTo), String.class, service.getRelativeToInjector());
            }
            service.setLevel(Level.parse(getLevelName()));
            final Boolean autoFlush = getAutoflush();
            if (autoFlush != null) service.setAutoflush(autoFlush.booleanValue());
            service.setEncoding(getEncoding());
            service.setPath(getPath());
            service.setFormatterSpec(getFormatter());
            service.setSuffix(suffix);
            serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
            serviceBuilder.addListener(new UpdateResultHandler.ServiceStartListener<P>(handler, param));
            serviceBuilder.install();
        } catch (Throwable t) {
            handler.handleFailure(t, param);
        }
    }
}
