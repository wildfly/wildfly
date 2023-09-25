/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation.ra;

import java.io.Serializable;
import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.Referenceable;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;
import jakarta.validation.constraints.NotEmpty;

/**
 * Admin object implementation
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ValidAdminObjectImpl implements ValidAdminObjectInterface, ResourceAdapterAssociation, Referenceable, Serializable {
    /**
     * Serial version uid
     */
    private static final long serialVersionUID = 1L;

    /**
     * The resource adapter
     */
    private ResourceAdapter ra;

    /**
     * Reference
     */
    private Reference reference;

    /**
     * property
     */
    @NotEmpty
    private String aoProperty;

    /**
     * Default constructor
     */
    public ValidAdminObjectImpl() {

    }

    /**
     * Set property
     *
     * @param property The value
     */
    public void setAoProperty(String property) {
        this.aoProperty = property;
    }

    /**
     * Get property
     *
     * @return The value
     */
    public String getAoProperty() {
        return aoProperty;
    }

    /**
     * Get the resource adapter
     *
     * @return The handle
     */
    public ResourceAdapter getResourceAdapter() {
        return ra;
    }

    /**
     * Set the resource adapter
     *
     * @param ra The handle
     */
    public void setResourceAdapter(ResourceAdapter ra) {
        this.ra = ra;
    }

    /**
     * Get the Reference instance.
     *
     * @return Reference instance
     * @throws javax.naming.NamingException Thrown if a reference can't be obtained
     */
    @Override
    public Reference getReference() throws NamingException {
        return reference;
    }

    /**
     * Set the Reference instance.
     *
     * @param reference A Reference instance
     */
    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        int result = 77;
        if (aoProperty != null) { result += 13 * result + 11 * aoProperty.hashCode(); } else { result += 13 * result + 11; }
        return result;
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
        if (!(other instanceof ValidAdminObjectImpl)) { return false; }
        ValidAdminObjectImpl obj = (ValidAdminObjectImpl) other;
        boolean result = true;
        if (result) {
            if (aoProperty == null) { result = obj.getAoProperty() == null; } else { result = aoProperty.equals(obj.getAoProperty()); }
        }
        return result;
    }

}
