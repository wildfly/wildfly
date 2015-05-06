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

package org.jboss.as.jpa.config;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * Count and collection of the persistence units deployed with the application
 *
 * @author Scott Marlow
 */
public class PersistenceUnitsInApplication {
    public static final AttachmentKey<PersistenceUnitsInApplication> PERSISTENCE_UNITS_IN_APPLICATION = AttachmentKey.create(PersistenceUnitsInApplication.class);

    private int count = 0;
    private List<PersistenceUnitMetadataHolder> persistenceUnitMetadataHolderList = new ArrayList<PersistenceUnitMetadataHolder>(1);

    /**
     * Gets the number of persistence units deployed with the applicatino
     * @return
     */
    public int getCount() {
        return count;
    }

    /**
     * Increment the count of persistence units in application
     * @param incrementValue
     */
    public void increment(int incrementValue) {
        count += incrementValue;
    }

    /**
     * Track the passed Persistence units for the application
     *
     * @param persistenceUnitMetadataHolder
     */
    public void addPersistenceUnitHolder(PersistenceUnitMetadataHolder persistenceUnitMetadataHolder) {
        persistenceUnitMetadataHolderList.add(persistenceUnitMetadataHolder);
    }

    public List<PersistenceUnitMetadataHolder> getPersistenceUnitHolders() {
        return persistenceUnitMetadataHolderList;
    }
}
