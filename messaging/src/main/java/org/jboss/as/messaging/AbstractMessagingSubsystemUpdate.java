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

package org.jboss.as.messaging;

import org.hornetq.core.config.Configuration;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateFailedException;

/**
 * The base class for messaging subsystem updates.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractMessagingSubsystemUpdate<R> extends AbstractSubsystemUpdate<MessagingSubsystemElement, R> {

    private static final long serialVersionUID = 4638051676467089281L;

    protected AbstractMessagingSubsystemUpdate() {
        super(Namespace.MESSAGING_1_0.getUriString());
    }

    protected AbstractMessagingSubsystemUpdate(boolean restart) {
        super(Namespace.MESSAGING_1_0.getUriString(), restart);
    }

    /** {@inheritDoc} */
    public Class<MessagingSubsystemElement> getModelElementType() {
        return MessagingSubsystemElement.class;
    }

    public abstract AbstractMessagingSubsystemUpdate<?> getCompensatingUpdate(MessagingSubsystemElement element);
}
