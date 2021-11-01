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
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

import org.jboss.logging.Logger;

/**
 * BaseActivationSpec
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>
 */
public class BaseActivationSpec implements ActivationSpec {

    private static Logger log = Logger.getLogger(BaseActivationSpec.class);

    /**
     * This method may be called by a deployment tool to validate the overall activation configuration information provided by
     * the endpoint deployer.
     *
     * @throws InvalidPropertyException indicates invalid configuration property settings.
     */
    @SuppressWarnings(value = { "deprecation" })
    public void validate() throws InvalidPropertyException {
        log.debug("call validate");

    }

    /**
     * Get the associated <code>ResourceAdapter</code> object.
     *
     * @return the associated <code>ResourceAdapter</code> object.
     */
    public ResourceAdapter getResourceAdapter() {
        log.debug("call getResourceAdapter");
        return null;
    }

    /**
     * Associate this object with a <code>ResourceAdapter</code> object.
     *
     * @param ra <code>ResourceAdapter</code> object to be associated with.
     *
     * @throws ResourceException generic exception.
     */
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        log.debug("call setResourceAdapter");

    }

}
