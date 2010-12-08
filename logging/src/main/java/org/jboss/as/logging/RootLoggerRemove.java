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
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceNotFoundException;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class RootLoggerRemove extends AbstractLoggingSubsystemUpdate<Void> {

    private static final long serialVersionUID = -9178350859833986971L;

    public RootLoggerRemove() {
    }

    /**
     * {@inheritDoc}
     */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceController<?> service;
        try {
            service = updateContext.getServiceRegistry().getRequiredService(LogServices.ROOT_LOGGER);
        } catch (ServiceNotFoundException e) {
            resultHandler.handleFailure(e, param);
            return;
        }
        service.setMode(ServiceController.Mode.REMOVE);
        service.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
    }

    /**
     * {@inheritDoc}
     */
    public RootLoggerAdd getCompensatingUpdate(LoggingSubsystemElement original) {
        final RootLoggerElement loggerElement = original.getRootLogger();
        if (loggerElement == null) {
            return null;
        }
        final RootLoggerAdd add = new RootLoggerAdd();
        add.setLevelName(loggerElement.getLevel());
        return add;
    }

    /**
     * {@inheritDoc}
     */
    protected void applyUpdate(LoggingSubsystemElement element) throws UpdateFailedException {
        final AbstractLoggerElement<?> logger = element.clearRootLogger();
        if (logger == null) {
            throw new UpdateFailedException("Root logger not defined");
        }
    }
}
