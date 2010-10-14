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

import org.jboss.jca.core.naming.SimpleJndiStrategy;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.mdr.NotFoundException;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

/**
 * A ResourceAdapterDeploymentService.
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public final class ResourceAdapterDeploymentService implements Service<CommonDeployment> {

    private final CommonDeployment value;

    private final Value<MetadataRepository> mdr;

    public static final Logger log = Logger.getLogger("org.jboss.as.deployment.service");

    /** create an instance **/
    public ResourceAdapterDeploymentService(CommonDeployment value, Value<MetadataRepository> mdr) {
        this.value = value;
        this.mdr = mdr;
    }

    @Override
    public CommonDeployment getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {
        if (value != null) {
            try {
                mdr.getValue().unregisterResourceAdapter(value.getURL().toExternalForm());
            } catch (Throwable t) {
                log.warn("Exception during unregistering deployment", t);
            }
        }

        log.debug("Undeploying: " + value.getDeploymentName());

        if (mdr != null && mdr.getValue() != null && value.getCfs() != null && value.getJndiNames() != null) {
            for (int i = 0; i < value.getCfs().length; i++) {
                try {
                    String cf = value.getCfs()[i].getClass().getName();
                    String jndi = value.getJndiNames()[i];

                    mdr.getValue().unregisterJndiMapping(value.getURL().toExternalForm(), cf, jndi);
                } catch (NotFoundException nfe) {
                    log.warn("Exception during JNDI unbinding", nfe);
                }
            }
        }

        if (value.getCfs() != null && value.getJndiNames() != null) {
            try {
                (new SimpleJndiStrategy()).unbindConnectionFactories(value.getDeploymentName(), value.getCfs(),
                        value.getJndiNames());
            } catch (Throwable t) {
                log.warn("Exception during JNDI unbinding", t);
            }
        }

        if (value != null && value.getResourceAdapter() != null) {
            value.getResourceAdapter().stop();
        }
    }

}
