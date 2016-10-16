/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jca.annorar;

import org.jboss.logging.Logger;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.validation.constraints.NotNull;

/**
 * AnnoActivationSpec
 *
 * @version $Revision: $
 */
@Activation(messageListeners = {AnnoMessageListener.class,
        AnnoMessageListener1.class})
public class AnnoActivationSpec implements ActivationSpec {

    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("AnnoActivationSpec");

    /**
     * The resource adapter
     */
    private ResourceAdapter ra;

    /**
     * first
     */
    @ConfigProperty(defaultValue = "C", description = {"1st", "first"}, ignore = true, supportsDynamicUpdates = false, confidential = true)
    @NotNull
    private Character first;

    /**
     * second
     */
    private Double second;

    /**
     * Default constructor
     */
    public AnnoActivationSpec() {

    }

    /**
     * Set first
     *
     * @param first The value
     */
    public void setFirst(Character first) {
        this.first = first;
    }

    /**
     * Get first
     *
     * @return The value
     */
    public Character getFirst() {
        return first;
    }

    /**
     * Set second
     *
     * @param second The value
     */
    @ConfigProperty(defaultValue = "0.5", description = {"2nd", "second"}, ignore = false, supportsDynamicUpdates = true, confidential = false)
    public void setSecond(Double second) {
        this.second = second;
    }

    /**
     * Get second
     *
     * @return The value
     */
    public Double getSecond() {
        return second;
    }

    /**
     * This method may be called by a deployment tool to validate the overall
     * activation configuration information provided by the endpoint deployer.
     *
     * @throws InvalidPropertyException indicates invalid configuration property settings.
     */
    public void validate() throws InvalidPropertyException {
        log.trace("validate()");
    }

    /**
     * Get the resource adapter
     *
     * @return The handle
     */
    public ResourceAdapter getResourceAdapter() {
        log.trace("getResourceAdapter()");
        return ra;
    }

    /**
     * Set the resource adapter
     *
     * @param ra The handle
     */
    public void setResourceAdapter(ResourceAdapter ra) {
        log.trace("setResourceAdapter()");
        this.ra = ra;
    }

}
