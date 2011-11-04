package org.jboss.as.server.deployment.module.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.module.ExtensionListEntry;
import org.jboss.as.server.deployment.module.FilterSpecification;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.ModuleIdentifier;

/**
* @author Stuart Douglas
*/
class ModuleStructureSpec {

    private ModuleIdentifier moduleIdentifier;
    private final List<ModuleDependency> moduleDependencies = new ArrayList<ModuleDependency>();
    private final List<ResourceRoot> resourceRoots = new ArrayList<ResourceRoot>();
    private final List<ExtensionListEntry> moduleExtensionDependencies = new ArrayList<ExtensionListEntry>();
    private final List<FilterSpecification> exportFilters = new ArrayList<FilterSpecification>();
    private final List<ModuleIdentifier> exclusions = new ArrayList<ModuleIdentifier>();
    private final List<String> classFileTransformers = new ArrayList<String>();
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

    public void addModuleExtensionDependency(ExtensionListEntry extension) {
        moduleExtensionDependencies.add(extension);
    }

    public List<ExtensionListEntry> getModuleExtensionDependencies() {
        return Collections.unmodifiableList(moduleExtensionDependencies);
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
}
