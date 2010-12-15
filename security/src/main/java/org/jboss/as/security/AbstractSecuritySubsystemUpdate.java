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

package org.jboss.as.security;

import org.jboss.as.model.AbstractSubsystemUpdate;

/**
 * The abstract Security subsystem update
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public abstract class AbstractSecuritySubsystemUpdate<R> extends AbstractSubsystemUpdate<SecuritySubsystemElement, R> {

    private static final long serialVersionUID = -6961024928692043760L;

    /** {@inheritDoc} */
    protected AbstractSecuritySubsystemUpdate() {
        super(Namespace.CURRENT.getUriString());
    }

    /** {@inheritDoc} */
    protected AbstractSecuritySubsystemUpdate(boolean requireRestart) {
        super(Namespace.CURRENT.getUriString(), requireRestart);
    }

    /** {@inheritDoc} */
    public Class<SecuritySubsystemElement> getModelElementType() {
        return SecuritySubsystemElement.class;
    }

}
