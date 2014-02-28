package org.wildfly.build.plugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class LineEndings {

    private final Type type;
    private final List<FileFilter> filters = new ArrayList<>();

    public LineEndings(Type type) {
        this.type = type;
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
        return false; //default exclude
    }

    public static enum Type {
        WINDOWS,
        UNIX,
    }
}
