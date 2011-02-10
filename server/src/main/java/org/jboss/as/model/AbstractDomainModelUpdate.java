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

import java.util.List;

/**
 * An update to the domain model.
 *
 * @param <R> the type of result that is returned by this update type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractDomainModelUpdate<R> extends AbstractModelUpdate<DomainModel, R> {

    private static final long serialVersionUID = 6933648919212398600L;

    /**
     * Construct a new instance.
     */
    protected AbstractDomainModelUpdate() {
    }

    /** {@inheritDoc} */
    @Override
    public final Class<DomainModel> getModelElementType() {
        return DomainModel.class;
    }

    /** {@inheritDoc} */
    @Override
    protected abstract void applyUpdate(final DomainModel element) throws UpdateFailedException;

    /** {@inheritDoc} */
    @Override
    public abstract AbstractDomainModelUpdate<?> getCompensatingUpdate(final DomainModel original);

    /** {@inheritDoc} */
    @Override
    public abstract AbstractServerModelUpdate<R> getServerModelUpdate();

    /**
     * Returns the server names of the servers
     * associated with the given host model that would be affected by this
     * update.
     *
     * @param domainModel the domain model as it exists following
     *       {@link #applyUpdate(AbstractModelElement application of this update)     *
     * @param hostModel the host model
     * @return the list of affected server names, or an empty set if no
     *         servers are affected
     * @throws UpdateFailedException if the state of the host model is such
     *              that this update is incompatible
     */
    public abstract List<String> getAffectedServers(final DomainModel domainModel, final HostModel hostModel) throws UpdateFailedException;
}
