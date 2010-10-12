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

package org.jboss.as.connector;

import org.jboss.jca.Version;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A ConnectorConfigService.
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
final class ConnectorConfigService implements Service<ConnectorSubsystemConfiguration> {

    private final ConnectorSubsystemConfiguration value;

    private static final Logger log = Logger.getLogger("org.jboss.as.connector");

    /** create an instance **/
    public ConnectorConfigService(ConnectorSubsystemConfiguration value) {
        this.value = value;
    }

    @Override
    public ConnectorSubsystemConfiguration getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.infof("Starting JCA Subsystem (%s)", Version.FULL_VERSION);
        log.infof("config=%s", value);
    }

    @Override
    public void stop(StopContext context) {

    }

}
