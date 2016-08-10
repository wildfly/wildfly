/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jca.beanvalidation.ra;


import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jboss.logging.Logger;

/**
 * Activation Spec
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ValidActivationSpec implements ActivationSpec {
    /**
     * The logger
     */
    private static Logger log = Logger.getLogger(ValidActivationSpec.class);

    /**
     * The resource adapter
     */
    private ResourceAdapter ra;

    @NotNull
    private Boolean myBooleanProperty;

    @NotNull
    @Size(min = 5)
    private String myStringProperty;

    /**
     * Default constructor
     */
    public ValidActivationSpec() {

    }

    /**
     * This method may be called by a deployment tool to validate the overall activation configuration information provided by
     * the endpoint deployer.
     *
     * @throws InvalidPropertyException indicates invalid onfiguration property settings.
     */
    public void validate() throws InvalidPropertyException {
        log.trace("validate()");
    }

    /**
     * @return the myBooleanProperty
     */
    public Boolean isMyBooleanProperty() {
        return myBooleanProperty;
    }

    /**
     * @param myBooleanProperty the myBooleanProperty to set
     */
    public void setMyStringProperty(String myProperty) {
        this.myStringProperty = myProperty;
    }

    /**
     * @return the myBooleanProperty
     */
    public String getMyStringProperty() {
        return myStringProperty;
    }

    /**
     * @param myBooleanProperty the myBooleanProperty to set
     */
    public void setMyBooleanProperty(Boolean myBooleanProperty) {
        this.myBooleanProperty = myBooleanProperty;
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
