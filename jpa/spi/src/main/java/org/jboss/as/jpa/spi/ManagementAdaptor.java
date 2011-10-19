/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.spi;

import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * Defines how JPA management is performed in AS7.
 * The first target of this interface will be Hibernate but other implementations
 * could be factored in.
 *
 * @author Scott Marlow
 */
public interface ManagementAdaptor {

    void register(final ManagementResourceRegistration jpaSubsystemDeployments, final PersistenceUnitServiceRegistry persistenceUnitRegistry);

    Resource createPersistenceUnitResource(final String persistenceUnitName, final String providerLabel);

    /**
     * Get the short identification string that represents the management adaptor (e.g Hibernate)
     *
     * @return id label
     */
    String getIdentificationLabel();

}
