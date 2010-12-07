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

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.services.path.AbstractPathService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class FileHandlerAdd extends AbstractHandlerAdd {

    private static final long serialVersionUID = 3144252544518106859L;

    private String relativeTo;

    private String path;

    private boolean append = true;

    protected FileHandlerAdd(final String name) {
        super(name);
    }

    protected AbstractHandlerElement<?> createElement(final String name) {
        final FileHandlerElement element = new FileHandlerElement(name);
        element.setPath(relativeTo, path);
        return element;
    }

    public String getRelativeTo() {
        return relativeTo;
    }

    public void setRelativeTo(final String relativeTo) {
        this.relativeTo = relativeTo;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(final boolean append) {
        this.append = append;
    }

    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        try {
            final BatchBuilder batchBuilder = updateContext.getServiceTarget();
            final FileHandlerService service = new FileHandlerService();
            final ServiceBuilder<Handler> serviceBuilder = batchBuilder.addService(LogServices.handlerName(getName()), service);
            final String relativeTo = this.relativeTo;
            if (relativeTo != null) {
                serviceBuilder.addDependency(AbstractPathService.pathNameOf(relativeTo), String.class, service.getRelativeToInjector());
            }
            service.setLevel(Level.parse(getLevelName()));
            final Boolean autoFlush = getAutoflush();
            if (autoFlush != null) service.setAutoflush(autoFlush.booleanValue());
            try {
                service.setEncoding(getEncoding());
            } catch (UnsupportedEncodingException e) {
                handler.handleFailure(e, param);
                return;
            }
            try {
                service.setPath(path);
            } catch (FileNotFoundException e) {
                handler.handleFailure(e, param);
                return;
            }
            service.setFormatterSpec(getFormatter());
            serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
            serviceBuilder.addListener(new UpdateResultHandler.ServiceStartListener<P>(handler, param));
            serviceBuilder.install();
        } catch (Throwable t) {
            handler.handleFailure(t, param);
            return;
        }
    }
}
