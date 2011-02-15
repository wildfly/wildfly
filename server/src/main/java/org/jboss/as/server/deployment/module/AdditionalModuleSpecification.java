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
package org.jboss.as.server.deployment.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.Attachable;
import org.jboss.modules.ModuleIdentifier;


/**
 * Represents an additional module attached to a top level deployment.
 *
 * @author Stuart Douglas
 *
 */
public class AdditionalModuleSpecification extends ModuleSpecification implements Attachable {

    private final ModuleIdentifier moduleIdentifier;

    private final List<ResourceRoot> resourceRoots;

    public AdditionalModuleSpecification(ModuleIdentifier moduleIdentifier, ResourceRoot resourceRoot) {
        this.moduleIdentifier = moduleIdentifier;
        this.resourceRoots = new ArrayList<ResourceRoot>();
        this.resourceRoots.add(resourceRoot);
    }

    public AdditionalModuleSpecification(ModuleIdentifier moduleIdentifier, Collection<ResourceRoot> resourceRoots) {
        this.moduleIdentifier = moduleIdentifier;
        this.resourceRoots = new ArrayList<ResourceRoot>(resourceRoots);
    }

    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }


    public void addResourceRoot(ResourceRoot resourceRoot) {
        this.resourceRoots.add(resourceRoot);
    }


    public void addResourceRoots(Collection<ResourceRoot> resourceRoots) {
        this.resourceRoots.addAll(resourceRoots);
    }


    public List<ResourceRoot> getResourceRoots() {
        return Collections.unmodifiableList(resourceRoots);
    }
}
