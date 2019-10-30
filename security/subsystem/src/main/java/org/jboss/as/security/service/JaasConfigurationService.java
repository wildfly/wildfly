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

package org.jboss.as.security.service;

import javax.security.auth.login.Configuration;

import org.jboss.as.security.SecurityExtension;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Jaas service to install a {@code Configuration}
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class JaasConfigurationService implements Service<Configuration> {

    public static final ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("jaas");

    private final Configuration configuration;

    public JaasConfigurationService(Configuration configuration) {
        this.configuration = configuration;
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        SecurityLogger.ROOT_LOGGER.debug("Starting JaasConfigurationService");

        // set new configuration
        synchronized(Configuration.class) {
            Configuration.setConfiguration(configuration);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop(StopContext context) {
        // Trigger a reload of configuration if anything else uses it.
        synchronized(Configuration.class) {
            Configuration.setConfiguration(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Configuration getValue() throws IllegalStateException, IllegalArgumentException {
        return configuration;
    }
}
