/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation.ra;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Resource adapter
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ValidResourceAdapter implements ResourceAdapter, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    /**
     * property
     */
    @NotNull
    @Min(3)
    private Integer raProperty;
    /**
     * The activations by activation spec
     */
    private Map<ValidActivationSpec, ValidActivation> activations = new HashMap<ValidActivationSpec, ValidActivation>();

    /**
     * Default constructor
     */
    public ValidResourceAdapter() {
    }

    /**
     * Set property
     *
     * @param property The value
     */
    public void setRaProperty(Integer property) {
        this.raProperty = property;
    }

    /**
     * Get property
     *
     * @return The value
     */
    public Integer getRaProperty() {
        return raProperty;
    }

    /**
     * This is called during the activation of a message endpoint.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     * @throws jakarta.resource.ResourceException generic exception
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        ValidActivation activation = new ValidActivation(this, endpointFactory, (ValidActivationSpec) spec);
        activations.put((ValidActivationSpec) spec, activation);
        activation.start();

    }

    /**
     * This is called when a message endpoint is deactivated.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        ValidActivation activation = activations.remove(spec);
        if (activation != null) { activation.stop(); }

    }

    /**
     * This is called when a resource adapter instance is bootstrapped.
     *
     * @param ctx A bootstrap context containing references
     * @throws jakarta.resource.spi.ResourceAdapterInternalException indicates bootstrap failure.
     */
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
    }

    /**
     * This is called when a resource adapter instance is undeployed or during application server shutdown.
     */
    public void stop() {
    }

    /**
     * This method is called by the application server during crash recovery.
     *
     * @param specs An array of ActivationSpec JavaBeans
     * @return An array of XAResource objects
     * @throws jakarta.resource.ResourceException generic exception
     */
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return null;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return 31 + 7 * raProperty;
    }

    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param other The reference object with which to compare.
     * @return true if this object is the same as the obj argument, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (!(other instanceof ValidResourceAdapter)) { return false; }
        ValidResourceAdapter obj = (ValidResourceAdapter) other;
        boolean result = true;
        if (result) {
            result = raProperty.equals(obj.getRaProperty());
        }
        return result;
    }

}
