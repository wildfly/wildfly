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

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * @author Emanuel Muckenhuber
 */
public class HandlerRemoveUpdate extends AbstractLoggingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 4158226899360272190L;

    private final String name;

    public HandlerRemoveUpdate(String name) {
        super();
        this.name = name;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<LoggingSubsystemElement, ?> getCompensatingUpdate(LoggingSubsystemElement original) {
        return new HandlerAddUpdate(original.getHandler(name));
    }

    /** {@inheritDoc} */
    protected void applyUpdate(LoggingSubsystemElement element) throws UpdateFailedException {
        AbstractHandlerElement<?> handler = element.removeHandler(name);
        if(handler == null) {
            throw new UpdateFailedException("failed to remove handler " + name);
        }
    }

}
