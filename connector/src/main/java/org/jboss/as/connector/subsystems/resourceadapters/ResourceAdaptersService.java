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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.Activations;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A ResourceAdaptersService.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
public final class ResourceAdaptersService implements Service<ResourceAdaptersService.ModifiableResourceAdaptors> {

    private final ModifiableResourceAdaptors value = new ModifiableResourceAdaptors();

    @Override
    public ModifiableResourceAdaptors getValue() throws IllegalStateException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        SUBSYSTEM_RA_LOGGER.debugf("Starting ResourceAdapters Service");
    }

    @Override
    public void stop(StopContext context) {
        SUBSYSTEM_RA_LOGGER.debugf("Stopping ResourceAdapters Service");
    }

    public static final class ModifiableResourceAdaptors implements Activations {

        private static final long serialVersionUID = 9096011997958619051L;
        private final List<Activation> resourceAdapters = new CopyOnWriteArrayList<>();

        /**
         * Get the resourceAdapters.
         *
         * @return the resourceAdapters.
         */
        @Override
        public List<Activation> getActivations() {
            return Collections.unmodifiableList(resourceAdapters);
        }

        public boolean addActivation(Activation ra) {
            return resourceAdapters.add(ra);
        }

        public boolean removeActivation(Activation ra) {
            return resourceAdapters.remove(ra);
        }

    }

}
