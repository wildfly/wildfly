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
package org.jboss.as.webservices;

import org.jboss.as.model.AbstractSubsystemUpdate;

/**
 * The abstract webservices subsystem update.
 *
 * @author alessio.soldano@jboss.com
 * @since 09-Nov-2010
 */
public abstract class AbstractWSSubsystemUpdate<R> extends AbstractSubsystemUpdate<WSSubsystemElement, R> {

    private static final long serialVersionUID = -7867682603415042763L;

    protected AbstractWSSubsystemUpdate() {
        super(Namespace.CURRENT.getUriString());
    }

    protected AbstractWSSubsystemUpdate(boolean requireRestart) {
        super(Namespace.CURRENT.getUriString(), requireRestart);
    }

    /** {@inheritDoc} */
    public Class<WSSubsystemElement> getModelElementType() {
        return WSSubsystemElement.class;
    }

}
