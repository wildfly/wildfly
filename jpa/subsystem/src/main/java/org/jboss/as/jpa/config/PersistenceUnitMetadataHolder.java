/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.config;

import java.util.List;

import jakarta.persistence.spi.PersistenceUnitInfo;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Holds the defined persistence units
 *
 * @author Scott Marlow
 */
public class PersistenceUnitMetadataHolder {

    /**
     * List<PersistenceUnitMetadataImpl> that represents the Jakarta Persistence persistent units
     */
    public static final AttachmentKey<PersistenceUnitMetadataHolder> PERSISTENCE_UNITS = AttachmentKey.create(PersistenceUnitMetadataHolder.class);

    private final List<PersistenceUnitMetadata> persistenceUnits;

    public List<PersistenceUnitMetadata> getPersistenceUnits() {
        return persistenceUnits;
    }

    public PersistenceUnitMetadataHolder(List<PersistenceUnitMetadata> persistenceUnits) {
        this.persistenceUnits = persistenceUnits;
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        for (PersistenceUnitInfo pu : persistenceUnits) {
            result.append(pu.toString());
        }
        return result.toString();
    }
}
