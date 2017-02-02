/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateful;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBLocator;

/**
 * A serializable EJB proxy which serializes the {@link EJBLocator} and the associated with the EJB proxy
 *
 * @author Jaikiran Pai
 */
class SerializableEJBProxy implements Serializable {

    private static final long serialVersionUID = 1L;

    private final EJBLocator<?> ejbLocator;

    /**
     * @param ejbProxy The EJB proxy
     * @throws IllegalArgumentException If the passed proxy is not an EJB proxy
     */
    SerializableEJBProxy(final Object ejbProxy) {
        // we hold on to the EJB locator and the EJB client context identifier
        this.ejbLocator = EJBClient.getLocatorFor(ejbProxy);
    }

    @SuppressWarnings("unused")
    private Object readResolve() throws ObjectStreamException {
        // recreate the proxy using the locator and the EJB client context identifier
        return EJBClient.createProxy(this.ejbLocator);
    }
}
