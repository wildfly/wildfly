package org.wildfly.build.plugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class Server {

    private String path;
    private String artifact;


    private final List<FileFilter> filters = new ArrayList<>();
    private final List<ModuleFilter> modules = new ArrayList<>();

    public List<FileFilter> getFilters() {
        return filters;
    }

    public List<ModuleFilter> getModules() {
        return modules;
    }

    public boolean includeFile(final String path) {
        if(path.startsWith("modules/") || path.startsWith("configuration")) {
            return false; //TODO: should we be hard coding this? And if so should it be here?
            //maybe we should move these files into src/modules and src/server-config?
        }
        for(FileFilter filter : filters) {
            if(filter.matches(path)) {
                return filter.isInclude();
            }
        }
        return true; //default include
    }

    public ModuleIncludeType includeModule(final String path) {
        for(ModuleFilter filter : modules) {
            if(filter.matches(path)) {
                if(filter.isInclude() ){
                    return filter.isTransitive() ? ModuleIncludeType.TRANSITIVE : ModuleIncludeType.MODULE_ONLY;
                } else {
                    return ModuleIncludeType.NONE;
                }
            }
        }
        return ModuleIncludeType.TRANSITIVE; //default include
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public static enum ModuleIncludeType {
        NONE,
        MODULE_ONLY,
        TRANSITIVE,
    }
}
