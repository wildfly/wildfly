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

package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.jca.Version;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A ConnectorConfigService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
final class JcaConfigService implements Service<JcaSubsystemConfiguration> {

    private final JcaSubsystemConfiguration value;

    /** create an instance **/
    public JcaConfigService(JcaSubsystemConfiguration value) {
        super();
        this.value = value;
    }

    @Override
    public JcaSubsystemConfiguration getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.startingSubsystem("JCA", Version.FULL_VERSION);
        ROOT_LOGGER.tracef("config=%s", value);
    }

    @Override
    public void stop(StopContext context) {

    }

}
