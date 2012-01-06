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

package org.jboss.as.capedwarf.deployment;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;

import java.util.List;
import java.util.Properties;

/**
 * Fix CapeDwarf JPA usage - atm we use Hibernate.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfJPAProcessor extends CapedwarfPersistenceProcessor {

    protected void modifyPersistenceInfo(DeploymentUnit unit, ResourceRoot resourceRoot, ResourceType type) {
        final PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
        if (holder != null) {
            final List<PersistenceUnitMetadata> pus = holder.getPersistenceUnits();
            if (pus != null && pus.isEmpty() == false) {
                for (PersistenceUnitMetadata pumd : pus) {
                    final String providerClassName = pumd.getPersistenceProviderClassName();
                    final boolean isProviderUndefined = (providerClassName == null || providerClassName.length() == 0);
                    if (isProviderUndefined || Configuration.getProviderModuleNameFromProviderClassName(providerClassName) == null) {
                        if (isProviderUndefined == false)
                            log.debug("Changing JPA configuration - " + providerClassName + " not yet supported.");

                        pumd.setPersistenceProviderClassName(Configuration.PROVIDER_CLASS_HIBERNATE); // TODO OGM usage
                        final Properties properties = pumd.getProperties();
                        if (properties.contains(DIALECT_PROPERTY_KEY) == false)
                            properties.put(DIALECT_PROPERTY_KEY, DEFAULT_DIALECT);
                    }
                }
            }
        }
    }

}
