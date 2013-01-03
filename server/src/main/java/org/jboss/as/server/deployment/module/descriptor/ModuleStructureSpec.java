/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.module.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.server.deployment.module.FilterSpecification;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleIdentifier;

/**
 * @author Stuart Douglas
 */
class ModuleStructureSpec {

    private ModuleIdentifier moduleIdentifier;
    private final List<ModuleDependency> moduleDependencies = new ArrayList<ModuleDependency>();
    private final List<DependencySpec> systemDependencies = new ArrayList<DependencySpec>();
    private final List<ResourceRoot> resourceRoots = new ArrayList<ResourceRoot>();
    private final List<FilterSpecification> exportFilters = new ArrayList<FilterSpecification>();
    private final List<ModuleIdentifier> exclusions = new ArrayList<ModuleIdentifier>();
    private final List<String> classFileTransformers = new ArrayList<String>();
    private final List<ModuleIdentifier> aliases = new ArrayList<ModuleIdentifier>();
    private final List<ModuleIdentifier> annotationModules = new ArrayList<ModuleIdentifier>();

    /**
     * Note that this being null is different to an empty list.
     *
     * Null means unspecified, while empty means specified but empty
     *
     * A sub deployment will inherit this from its parent if it is unspecified, but not if
     * it is empty but specified.
     */
    private Set<String> excludedSubsystems;

    private boolean localLast = false;

    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    public void setModuleIdentifier(ModuleIdentifier moduleIdentifier) {
        this.moduleIdentifier = moduleIdentifier;
    }

    public void addModuleDependency(ModuleDependency dependency) {
        moduleDependencies.add(dependency);
    }

    public List<ModuleDependency> getModuleDependencies() {
        return Collections.unmodifiableList(moduleDependencies);
    }

    public void addResourceRoot(ResourceRoot resourceRoot) {
        resourceRoots.add(resourceRoot);
    }

    public List<ResourceRoot> getResourceRoots() {
        return Collections.unmodifiableList(resourceRoots);
    }

    public void addSystemDependency(final DependencySpec dependency) {
        systemDependencies.add(dependency);
    }

    public List<DependencySpec> getSystemDependencies() {
        return Collections.unmodifiableList(systemDependencies);
    }

    public void addAlias(final ModuleIdentifier dependency) {
        aliases.add(dependency);
    }

    public List<ModuleIdentifier> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

    public void addAnnotationModule(final ModuleIdentifier dependency) {
        annotationModules.add(dependency);
    }

    public List<ModuleIdentifier> getAnnotationModules() {
        return Collections.unmodifiableList(annotationModules);
    }

    public List<ModuleIdentifier> getExclusions() {
        return exclusions;
    }

    public List<FilterSpecification> getExportFilters() {
        return exportFilters;
    }

    public List<String> getClassFileTransformers() {
        return classFileTransformers;
    }

    public boolean isLocalLast() {
        return localLast;
    }

    public void setLocalLast(final boolean localLast) {
        this.localLast = localLast;
    }

    public Set<String> getExcludedSubsystems() {
        return excludedSubsystems;
    }

    public void setExcludedSubsystems(final Set<String> excludedSubsystems) {
        this.excludedSubsystems = excludedSubsystems;
    }
}
