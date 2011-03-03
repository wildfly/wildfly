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
    private BindingSourceDescription referenceSourceDescription;
    private boolean absoluteBinding;
    private boolean dependency;
    private String bindingName;
    private String bindingType;
    private String description;
    private List<InjectionTargetDescription> injectionTargetDescriptions = new ArrayList<InjectionTargetDescription>(1);

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
     * Determine whether this binding is an absolute name or whether it's relative to the component environment base.
     *
     * @return {@code true} if the binding is absolute
     */
    public boolean isAbsoluteBinding() {
        return absoluteBinding;
    }

    /**
     * Set whether this binding is an absolute name or whether it's relative to the component environment base.
     *
     * @param absoluteBinding {@code true} if the binding is absolute
     */
    public void setAbsoluteBinding(final boolean absoluteBinding) {
        this.absoluteBinding = absoluteBinding;
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
     * The JNDI name into which this reference will be bound, or {@code null} if no binding should take place.
     *
     * @param bindingName the JNDI binding name
     */
    public void setBindingName(final String bindingName) {
        this.bindingName = bindingName;
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
}
