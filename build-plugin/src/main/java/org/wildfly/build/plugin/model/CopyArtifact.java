package org.wildfly.build.plugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class CopyArtifact {

    private final String artifact;
    private final String toLocation;
    private final boolean extract;
    private final List<FileFilter> filters = new ArrayList<>();


    public CopyArtifact(String artifact, String toLocation, boolean extract) {
        this.artifact = artifact;
        this.toLocation = toLocation;
        this.extract = extract;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getToLocation() {
        return toLocation;
    }

    public boolean isExtract() {
        return extract;
    }

    public List<FileFilter> getFilters() {
        return filters;
    }

    public boolean includeFile(final String path) {
        for(FileFilter filter : filters) {
            if(filter.matches(path)) {
                return filter.isInclude();
            }
        }
        return true; //default include
    }
}
