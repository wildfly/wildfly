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

package org.jboss.as.osgi.parser;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * OSGi subsystem update element.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public final class OSGiSubsystemUpdate extends AbstractSubsystemUpdate<OSGiSubsystemElement, Object> {

    private static final long serialVersionUID = -4542570180370773590L;

    private OSGiSubsystemState subsystemState = new OSGiSubsystemState();

    protected OSGiSubsystemUpdate() {
        super(OSGiExtension.NAMESPACE, true);
    }

    OSGiSubsystemUpdate(OSGiSubsystemState subsystemState) {
        super(OSGiExtension.NAMESPACE, true);
        this.subsystemState = subsystemState;
    }

    OSGiSubsystemState getSubsystemState() {
        return subsystemState;
    }

    @Override
    public AbstractSubsystemUpdate<OSGiSubsystemElement, ?> getCompensatingUpdate(OSGiSubsystemElement original) {
        return new OSGiSubsystemUpdate(original.getSubsystemState());
    }

    @Override
    public Class<OSGiSubsystemElement> getModelElementType() {
        return OSGiSubsystemElement.class;
    }

    @Override
    protected void applyUpdate(OSGiSubsystemElement element) throws UpdateFailedException {
        element.setSubsystemState(subsystemState);
    }

    @Override
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<Object, P> resultHandler, P param) {
        // [TODO] review what to do here
    }
}
