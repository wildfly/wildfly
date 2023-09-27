/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
