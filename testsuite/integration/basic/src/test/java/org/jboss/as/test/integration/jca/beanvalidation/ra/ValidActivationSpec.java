/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation.ra;


import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
