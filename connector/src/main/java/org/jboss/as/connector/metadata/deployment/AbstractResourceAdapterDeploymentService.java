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

package org.jboss.as.connector.metadata.deployment;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.mdr.NotFoundException;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A ResourceAdapterDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public abstract class AbstractResourceAdapterDeploymentService {

    private static final Logger log = Logger.getLogger("org.jboss.as.deployment.connector");

    protected final ResourceAdapterDeployment value;

    protected final InjectedValue<MetadataRepository> mdr = new InjectedValue<MetadataRepository>();

    protected final InjectedValue<ResourceAdapterDeploymentRegistry> registry = new InjectedValue<ResourceAdapterDeploymentRegistry>();

    protected final InjectedValue<JndiStrategy> jndiStrategy = new InjectedValue<JndiStrategy>();

    /** create an instance **/
    public AbstractResourceAdapterDeploymentService(ResourceAdapterDeployment value) {
        super();
        this.value = value;
    }

    public ResourceAdapterDeployment getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    public void start(StartContext context) throws StartException {
    }

    /**
     * Stop
     */
    public void stop(StopContext context) {
        if (value != null) {
            log.debugf("Undeploying: %s", value.getDeployment() != null ? value.getDeployment().getDeploymentName() : "");

            if (registry != null && registry.getValue() != null) {
                registry.getValue().unregisterResourceAdapterDeployment(value);
            }

            if (mdr != null && mdr.getValue() != null) {
                try {
                    mdr.getValue().unregisterResourceAdapter(value.getDeployment().getDeploymentName());
                } catch (Throwable t) {
                    log.warn("Exception during unregistering deployment", t);
                }
            }

            if (mdr != null && mdr.getValue() != null && value.getDeployment() != null
                    && value.getDeployment().getCfs() != null && value.getDeployment().getCfJndiNames() != null) {
                for (int i = 0; i < value.getDeployment().getCfs().length; i++) {
                    try {
                        String cf = value.getDeployment().getCfs()[i].getClass().getName();
                        String jndi = value.getDeployment().getCfJndiNames()[i];

                        mdr.getValue().unregisterJndiMapping(value.getDeployment().getURL().toExternalForm(), cf, jndi);
                    } catch (NotFoundException nfe) {
                        log.warn("Exception during JNDI unbinding", nfe);
                    }
                }
            }

            if (value.getDeployment() != null && value.getDeployment().getCfs() != null
                    && value.getDeployment().getCfJndiNames() != null) {
                try {
                    jndiStrategy.getValue().unbindConnectionFactories(value.getDeployment().getDeploymentName(), value
                            .getDeployment().getCfs(), value.getDeployment().getCfJndiNames());
                } catch (Throwable t) {
                    log.warn("Exception during JNDI unbinding", t);
                }
            }

            if (mdr != null && mdr.getValue() != null && value.getDeployment().getAos() != null
                    && value.getDeployment().getAoJndiNames() != null) {
                for (int i = 0; i < value.getDeployment().getAos().length; i++) {
                    try {
                        String ao = value.getDeployment().getAos()[i].getClass().getName();
                        String jndi = value.getDeployment().getAoJndiNames()[i];

                        mdr.getValue().unregisterJndiMapping(value.getDeployment().getURL().toExternalForm(), ao, jndi);
                    } catch (NotFoundException nfe) {
                        log.warn("Exception during JNDI unbinding", nfe);
                    }
                }
            }

            if (value.getDeployment() != null && value.getDeployment().getAos() != null
                    && value.getDeployment().getAoJndiNames() != null) {
                try {
                    jndiStrategy.getValue().unbindConnectionFactories(value.getDeployment().getDeploymentName(), value
                            .getDeployment().getAos(), value.getDeployment().getAoJndiNames());
                } catch (Throwable t) {
                    log.warn("Exception during JNDI unbinding", t);
                }
            }

            if (value.getDeployment() != null && value.getDeployment().getResourceAdapter() != null) {
                value.getDeployment().getResourceAdapter().stop();
            }
        }
    }

    public Injector<MetadataRepository> getMdrInjector() {
        return mdr;
    }

    public Injector<ResourceAdapterDeploymentRegistry> getRegistryInjector() {
        return registry;
    }

    public Injector<JndiStrategy> getJndiInjector() {
        return jndiStrategy;
    }
}
