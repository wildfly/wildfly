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

import org.jboss.as.server.deployment.AttachmentKey;

import javax.persistence.spi.PersistenceUnitInfo;
import java.util.List;

/**
 * Holds the defined persistence units
 *
 * @author Scott Marlow
 */
public class PersistenceMetadataHolder {

    /**
     *  List<PersistenceMetadata> that represents the JPA persistent units
     */
    public static final AttachmentKey<PersistenceMetadataHolder> PERSISTENCE_UNITS = AttachmentKey.create(PersistenceMetadataHolder.class);

    private List<PersistenceUnitInfo> persistenceUnits;

    public List<PersistenceUnitInfo> getPersistenceUnits() {
        return persistenceUnits;
    }

    public PersistenceMetadataHolder setPersistenceUnits(List<PersistenceUnitInfo> persistenceUnits) {
        this.persistenceUnits = persistenceUnits;
        return this;
    }
}
