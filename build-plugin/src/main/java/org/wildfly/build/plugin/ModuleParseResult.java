package org.wildfly.build.plugin;

import org.jboss.modules.ModuleIdentifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class ModuleParseResult {
    final Path moduleRoot;
    final Path moduleXmlFile;
    final List<ModuleDependency> dependencies = new ArrayList<ModuleDependency>();
    final List<String> resourceRoots = new ArrayList<>();
    final List<String> artifacts = new ArrayList<>();
    ModuleIdentifier identifier;

    public ModuleParseResult(Path moduleRoot, Path moduleXmlFile) {
        this.moduleRoot = moduleRoot;
        this.moduleXmlFile = moduleXmlFile;
    }

    public Path getModuleXmlFile() {
        return moduleXmlFile;
    }

    public Path getModuleRoot() {
        return moduleRoot;
    }

    public List<ModuleDependency> getDependencies() {
        return dependencies;
    }

    public List<String> getResourceRoots() {
        return resourceRoots;
    }

    public List<String> getArtifacts() {
        return artifacts;
    }

    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    static class ModuleDependency {
        private final ModuleIdentifier moduleId;
        private final boolean optional;

        ModuleDependency(ModuleIdentifier moduleId, boolean optional) {
            this.moduleId = moduleId;
            this.optional = optional;
        }

        ModuleIdentifier getModuleId() {
            return moduleId;
        }

        boolean isOptional() {
            return optional;
        }

        @Override
        public String toString() {
            return "[" + moduleId + (optional ? ",optional=true" : "") + "]";
        }
    }
}
