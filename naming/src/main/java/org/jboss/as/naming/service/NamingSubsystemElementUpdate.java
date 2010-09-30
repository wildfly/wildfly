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

package org.jboss.as.naming.service;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * Update the naming subsystem.
 *
 * @author Emanuel Muckenhuber
 */
public class NamingSubsystemElementUpdate extends AbstractSubsystemUpdate<NamingSubsystemElement, Void> {

    private static final long serialVersionUID = -3536246934818035526L;

    private boolean supportEvents = true;
    private boolean bindAppContext;
    private boolean bindModuleContext;
    private boolean bindCompContext;

    public NamingSubsystemElementUpdate(boolean supportEvents, boolean bindAppContext, boolean bindModuleContext, boolean bindCompContext) {
        super(NamingExtension.NAMESPACE, true);
        this.supportEvents = supportEvents;
        this.bindAppContext = bindAppContext;
        this.bindModuleContext = bindModuleContext;
        this.bindCompContext = bindCompContext;
    }

    /** {@inheritDoc} */
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<NamingSubsystemElement, ?> getCompensatingUpdate(NamingSubsystemElement original) {
        return new NamingSubsystemElementUpdate(
                original.isSupportEvents(),
                original.isBindAppContext(),
                original.isBindModuleContext(),
                original.isBindModuleContext());
    }

    /** {@inheritDoc} */
    public Class<NamingSubsystemElement> getModelElementType() {
        return NamingSubsystemElement.class;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(NamingSubsystemElement element) throws UpdateFailedException {
        element.setSupportEvents(supportEvents);
        element.setBindAppContext(bindAppContext);
        element.setBindModuleContext(bindModuleContext);
        element.setBindCompContext(bindCompContext);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // TODO Auto-generated method stub
    }

}
