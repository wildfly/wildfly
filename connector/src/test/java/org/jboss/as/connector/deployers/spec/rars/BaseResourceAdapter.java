/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2009, Red Hat Inc, and individual contributors
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
package org.jboss.as.connector.deployers.spec.rars;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.jboss.logging.Logger;

/**
 * BaseResourceAdapter
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>.
 * @version $Revision: $
 */
public class BaseResourceAdapter implements ResourceAdapter {
    private static Logger log = Logger.getLogger(BaseResourceAdapter.class);

    /**
     * This is called during the activation of a message endpoint.
     *
     * @param endpointFactory a message endpoint factory instance.
     * @param spec an activation spec JavaBean instance.
     * @throws ResourceException generic exception
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        log.debug("call endpointActivation");

    }

    /**
     * This is called when a message endpoint is deactivated.
     *
     * @param endpointFactory a message endpoint factory instance.
     * @param spec an activation spec JavaBean instance.
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        log.debug("call endpointDeactivation");

    }

    /**
     * This method is called by the application server during crash recovery.
     *
     * @param specs an array of ActivationSpec JavaBeans
     * @throws ResourceException generic exception
     * @return an array of XAResource objects
     */
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        log.debug("call getXAResources");
        return null;
    }

    /**
     * This is called when a resource adapter instance is bootstrapped.
     *
     * @param ctx a bootstrap context containing references
     * @throws ResourceAdapterInternalException indicates bootstrap failure.
     */
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        log.debug("call start");

    }

    /**
     * This is called when a resource adapter instance is undeployed or during application server shutdown.
     */
    public void stop() {
        log.debug("call stop");

    }

    /**
     * Hash code
     *
     * @return The hash
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Equals
     *
     * @param other The other object
     * @return True if equal; otherwise false
     */
    public boolean equals(Object other) {
        return super.equals(other);
    }
}
