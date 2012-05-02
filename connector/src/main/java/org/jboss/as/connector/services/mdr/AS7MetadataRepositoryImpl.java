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

package org.jboss.as.connector.services.mdr;

import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.core.mdr.SimpleMetadataRepository;
import org.jboss.jca.core.spi.mdr.AlreadyExistsException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An AS7' implementation of MetadataRepository
 *
 * @author Stefano Maestri (c) 2011 Red Hat Inc.
 */
public class AS7MetadataRepositoryImpl extends SimpleMetadataRepository implements AS7MetadataRepository {

    private final Map<String, IronJacamar> ironJacamarMetaData = new HashMap<String, IronJacamar>();

    @Override
    public void registerResourceAdapter(String uniqueId, File root, Connector md, IronJacamar ijmd) throws AlreadyExistsException {
        super.registerResourceAdapter(uniqueId, root, md, ijmd);
        if (ijmd != null) {
            ironJacamarMetaData.put(uniqueId, ijmd);
        }
    }

    @Override
    public IronJacamar getIronJcamarMetaData(String uniqueId) {
        return ironJacamarMetaData.get(uniqueId);
    }

    @Override
    public Set<String> getResourceAdaptersWithIronJacamarMetadata() {
        return ironJacamarMetaData.keySet();
    }

}
