/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * A description of a JNDI binding associated with a component.  The binding
 * may refer to the component itself, a view thereof, or an injection used by
 * the component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BindingDescription {

    private static final String JAVA = "java:";

    private BindingSourceDescription referenceSourceDescription;
    private boolean dependency;
    private final String bindingName;
    private String bindingType;
    private String description;
    private Resource.AuthenticationType authenticationType = Resource.AuthenticationType.CONTAINER;
    private boolean sharable = false;

    private List<InjectionTargetDescription> injectionTargetDescriptions = new ArrayList<InjectionTargetDescription>(1);

    /**
     * Creates a binding description for an absolute name.
     * @param bindingName The jndi name to bind. Must start with <code>java:</code>
     */
    public BindingDescription(String bindingName) {
        if(!bindingName.startsWith("java:")) {
            throw new RuntimeException("Absolute binding name " + bindingName + " must start with java:");
        }
        this.bindingName = bindingName;
    }

    /**
     * Creates a binding description for an absolute or relative name.
     * If the name does not start with <code>java:</code> it is assumed to be a relative
     * name, as will have either <code>java:comp/env</code> or <code>java:module/env</code>
     * appended to it, depending on the naming mode of the component.
     *
     * @param bindingName The JNDI name to bind
     * @param componentDescription The component that defined the binding
     */
    public BindingDescription(String bindingName, AbstractComponentDescription componentDescription) {
        if(!bindingName.startsWith(JAVA)) {
            if(componentDescription.getNamingMode() == ComponentNamingMode.CREATE) {
                this.bindingName = "java:comp/env/" + bindingName;
            } else {
                this.bindingName = "java:module/env/" + bindingName;
            }
        } else {
            this.bindingName = bindingName;
        }
    }

    /**
     * Get the description of the reference.
     *
     * @return the description
     * @see javax.annotation.Resource#description()
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the reference.
     *
     * @param description the description
     * @see javax.annotation.Resource#description()
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Get the mutable list of injection target descriptions.
     *
     * @return the injection target description list
     */
    public List<InjectionTargetDescription> getInjectionTargetDescriptions() {
        return injectionTargetDescriptions;
    }

    /**
     * Determine whether this binding is a dependency of the component.
     *
     * @return {@code true} if the component start depends on this binding, {@code false} otherwise
     */
    public boolean isDependency() {
        return dependency;
    }

    /**
     * Set whether this binding is a dependency of the component.
     *
     * @param dependency {@code true} if the component start depends on this binding, {@code false} otherwise
     */
    public void setDependency(final boolean dependency) {
        this.dependency = dependency;
    }

    /**
     * The JNDI name into which this reference will be bound, or {@code null} if no binding should take place.
     *
     * @return the JNDI binding name
     */
    public String getBindingName() {
        return bindingName;
    }

    /**
     * Get the class name of what is being bound.  Note that this is the
     * name of the class of the final injectable instance, not that of any
     * intermediate reference or factory.
     *
     * @return the class name
     */
    public String getBindingType() {
        return bindingType;
    }

    /**
     * Set the class name of what is being bound.  Note that this is the
     * name of the class of the final injectable instance, not that of any
     * intermediate reference or factory.
     *
     * @param bindingType the class name
     */
    public void setBindingType(final String bindingType) {
        this.bindingType = bindingType;
    }

    /**
     * The injection source.
     *
     * @return the injection source description
     */
    public BindingSourceDescription getReferenceSourceDescription() {
        return referenceSourceDescription;
    }

    /**
     * Set the injection source.
     *
     * @param referenceSourceDescription the injection source description
     */
    public void setReferenceSourceDescription(final BindingSourceDescription referenceSourceDescription) {
        if (referenceSourceDescription == null) {
            throw new IllegalArgumentException("referenceSourceDescription is null");
        }
        this.referenceSourceDescription = referenceSourceDescription;
    }

    /**
     * The authentication type for the resource. Will not be used in most cases
     * @return The authentication type
     */
    public Resource.AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    /**
     * Sets the authentication type
     * @param authenticationType The authentication type
     */
    public void setAuthenticationType(Resource.AuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    /**
     * If this resource can be shared between components.
     * @return The sharable attribute
     */
    public boolean isSharable() {
        return sharable;
    }

    /**
     * Sets if this resource can be shared between components.
     * @param sharable If this resource is shareable
     */
    public void setSharable(boolean sharable) {
        this.sharable = sharable;
    }

    @Override
    public String toString() {
        return "BindingDescription{" +
                "bindingName='" + bindingName + '\'' +
                ", bindingType='" + bindingType + '\'' +
                ", referenceSourceDescription=" + referenceSourceDescription +
                '}';
    }

}
