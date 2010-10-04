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

package org.jboss.as.model;

import org.jboss.msc.service.ServiceContainer;

/**
 * An update which modifies the server's system properties.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerSystemPropertyUpdate extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = 7462989455163230095L;

    private final AbstractPropertyUpdate propertyUpdate;

    /**
     * Construct a new instance.
     *
     * @param propertyUpdate the property update to apply
     */
    public ServerSystemPropertyUpdate(final AbstractPropertyUpdate propertyUpdate) {
        this.propertyUpdate = propertyUpdate;
    }

    /** {@inheritDoc} */
    @Override
    public boolean requiresRestart() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void applyUpdate(final ServerModel element) throws UpdateFailedException {
        propertyUpdate.applyUpdate(element.getSystemProperties());
    }

    /** {@inheritDoc} */
    @Override
    public <P> void applyUpdate(final ServiceContainer container, final UpdateResultHandler<Void, P> resultHandler, final P param) {
        try {
            propertyUpdate.applyUpdate(System.getProperties());
        } catch (UpdateFailedException e) {
            resultHandler.handleFailure(e, param);
            return;
        }
        resultHandler.handleSuccess(null, param);
    }

    /** {@inheritDoc} */
    @Override
    public ServerSystemPropertyUpdate getCompensatingUpdate(final ServerModel original) {
        return new ServerSystemPropertyUpdate(propertyUpdate.getCompensatingUpdate(original.getSystemProperties()));
    }
}
