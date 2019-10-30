/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jipijapa.plugin.spi;

import javax.persistence.EntityManagerFactory;

/**
 * Persistence unit service
 *
 * @author Scott Marlow
 */
public interface PersistenceUnitService {
    /**
     * get the entity manager factory that represents the persistence unit service.  This corresponds to a
     * persistence unit definition in a persistence.xml
     *
     * @return EntityManagerFactory or {@code null} if this service has not been started or has been stopped
     */
    EntityManagerFactory getEntityManagerFactory();

    /**
     * Gets the scoped name of this persistence unit.
     *
     * @return the name
     */
    String getScopedPersistenceUnitName();
}
