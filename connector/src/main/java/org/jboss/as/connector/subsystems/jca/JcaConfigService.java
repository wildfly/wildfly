/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        ROOT_LOGGER.startingSubsystem("Jakarta Connectors", Version.FULL_VERSION);
        ROOT_LOGGER.tracef("config=%s", value);
    }

    @Override
    public void stop(StopContext context) {

    }

}
