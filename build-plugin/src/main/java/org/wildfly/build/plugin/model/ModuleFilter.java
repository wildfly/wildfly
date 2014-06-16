package org.wildfly.build.plugin.model;

import java.util.regex.Pattern;

/**
 * @author Stuart Douglas
 */
public class ModuleFilter {

    private final Pattern pattern;
    private final boolean include;
    private final boolean transitive;

    public ModuleFilter(String pattern, boolean include, boolean transitive) {
        this.transitive = transitive;
        this.pattern = Pattern.compile(pattern);
        this.include = include;
    }

    /**
     * Returns true if the file matches the regular expression
     */
    public boolean matches(final String filePath) {
        return pattern.matcher(filePath).matches();
    }

    public boolean isInclude() {
        return include;
    }

    public boolean isTransitive() {
        return transitive;
    }
}
