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
import org.jboss.logmanager.Logger;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController;

import java.util.logging.Level;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class RootLoggerAdd extends AbstractLoggingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 4230922005791983261L;
    private final Level level;

    public RootLoggerAdd(final Level level) {
        this.level = level;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final RootLoggerService service = new RootLoggerService();
        service.setLevel(level);
        final BatchServiceBuilder<Logger> builder = updateContext.getBatchBuilder().addService(LogServices.ROOT_LOGGER, service);
        builder.setInitialMode(ServiceController.Mode.ACTIVE);
    }

    /** {@inheritDoc} */
    public RootLoggerRemove getCompensatingUpdate(LoggingSubsystemElement original) {
        return new RootLoggerRemove();
    }

    /** {@inheritDoc} */
    protected void applyUpdate(LoggingSubsystemElement element) throws UpdateFailedException {
        final RootLoggerElement logger = new RootLoggerElement();
        logger.setLevel(level);
        if(! element.setRootLogger(logger)) {
            throw new UpdateFailedException("Root logger already exists");
        }
    }

}
