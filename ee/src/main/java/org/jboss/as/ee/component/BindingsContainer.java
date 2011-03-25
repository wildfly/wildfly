/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container for JNDI bindings. Distinguishes between bindings added through a deployment descriptor and bindings added through
 * annotations.
 *
 * @author Stuart Douglas
 */
public class BindingsContainer {
    private final Map<String, BindingDescription> annotationBindingDescriptions = new HashMap<String, BindingDescription>();
    private final Map<String, BindingDescription> bindingDescriptions = new HashMap<String, BindingDescription>();
    private volatile List<BindingDescription> bindings;


    /**
     * Adds bindings defined though an annotation.
     *
     * @param binding The binding to add
     */
    public synchronized void addAnnotationBinding(BindingDescription binding) {
        bindings = null;
        final String bindingName = binding.getBindingName();

        //first we check if a deployment descriptor entry already exists:
        if (bindingDescriptions.containsKey(bindingName)) {
            final BindingDescription existingBinding = bindingDescriptions.get(bindingName);
            if (existingBinding.isSharable() != binding.isSharable() ||
                    existingBinding.getAuthenticationType() != binding.getAuthenticationType() ||
                    !existingBinding.getBindingType().equals(binding.getBindingType())) {
                throw new RuntimeException("Error merging binding " + binding + " with " + existingBinding + " in " + this + ", the JNDI bindings are not compatible.");
            }
            //add the injection targets
            existingBinding.getInjectionTargetDescriptions().addAll(binding.getInjectionTargetDescriptions());
        } else if (annotationBindingDescriptions.containsKey(bindingName)) {
                throw new RuntimeException("Duplicate binding of JNDI name " + bindingName);
        } else {
            annotationBindingDescriptions.put(bindingName, binding);
        }
    }

    /**
     * Adds multiple bindings defined through an annotation
     *
     * @param bindings The bindings to add
     */
    public void addAnnotationBindings(Collection<BindingDescription> bindings) {
        for(BindingDescription binding : bindings) {
            addAnnotationBinding(binding);
        }
    }

    /**
     * Adds a binding. This binding will override an annotation binding with the same name,
     * however injection target information will be merged.
     *
     * @param binding The binding to add
     */
    public synchronized void addBinding(BindingDescription binding) {
        bindings = null;
        final String bindingName = binding.getBindingName();

        if (bindingDescriptions.containsKey(bindingName)) {
            throw new RuntimeException("JNDI binding for " + binding.getBindingName() + " has already been set " + binding);
        } else if (annotationBindingDescriptions.containsKey(bindingName)) {
            final BindingDescription existingBinding = annotationBindingDescriptions.get(bindingName);
            if (!existingBinding.getBindingType().equals(binding.getBindingType())) {
                throw new RuntimeException("Error overriding annotation based binding" + binding + " with " + existingBinding + ", the JNDI bindings are not compatible.");
            }
            //add the injection targets
            binding.getInjectionTargetDescriptions().addAll(existingBinding.getInjectionTargetDescriptions());
            annotationBindingDescriptions.remove(bindingName);
        }
        bindingDescriptions.put(bindingName, binding);

    }

    /**
     * Adds multiple bindings
     *
     * @see #addBinding(BindingDescription)
     * @param bindings The bindings to add
     * @throws DeploymentUnitProcessingException
     */
    public void addBindings(Collection<BindingDescription> bindings) throws DeploymentUnitProcessingException {
        for(BindingDescription binding : bindings) {
            addBinding(binding);
        }
    }

    /**
     *
     * @return A list of all bindings in the container
     */
    public synchronized List<BindingDescription> getMergedBindings() {
        if (bindings == null) {
            List<BindingDescription> bindings = new ArrayList<BindingDescription>();
            Set<String> names = new HashSet<String>(annotationBindingDescriptions.keySet());
            names.addAll(bindingDescriptions.keySet());
            for(String name : names) {
                if(bindingDescriptions.containsKey(name)) {
                    bindings.add(bindingDescriptions.get(name));
                } else {
                    bindings.add(annotationBindingDescriptions.get(name));
                }
            }
            this.bindings = Collections.unmodifiableList(bindings);
        }
        return bindings;
    }
}
