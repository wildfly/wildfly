/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descriptive information about an item of content within a distribution.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 * @author Emanuel Muckenhuber
 */
abstract class DistributionContentItem implements Comparable<DistributionContentItem> {

    static final char PATH_DELIMITER = '/';
    static final Set<DistributionContentItem> NO_CHILDREN = Collections.emptySet();

    protected final DistributionContentItem parent;
    protected final String name;

    protected DistributionContentItem(final DistributionContentItem parent, final String name) {
        this.parent = parent;
        this.name = name;
    }

    /**
     * Get the hash used for the metadata.
     *
     * @return the hash used for the metadata
     */
    public abstract byte[] getMetadataHash();

    /**
     * Get the hash which is used to check whether an item changed.
     *
     * @return the hash used for the comparison
     */
    public abstract byte[] getComparisonHash();

    /**
     * See whether this item is a leaf in the tree.
     *
     * @return
     */
    public abstract boolean isLeaf();

    /**
     * Get the children.
     *
     * @return the children
     */
    public abstract Collection<DistributionContentItem> getChildren();

    public String getName() {
        return name;
    }

    public DistributionContentItem getParent() {
        return parent;
    }

    public File getFile(File rootFile) {
        if (parent == null) {
            return rootFile;
        } else {
            return new File(parent.getFile(rootFile), getName());
        }
    }

    public String getPath() {
        return getPath(PATH_DELIMITER);
    }

    public String getPath(char delimiter) {
        StringBuilder sb = new StringBuilder();
        recordPath(sb, delimiter);
        return sb.toString();
    }

    private void recordPath(StringBuilder sb, char delimiter) {
        if (parent != null) {
            parent.recordPath(sb, delimiter);
            if (sb.length() > 0) {
                sb.append(delimiter);
            }
        }

        if (name != null) {
            sb.append(name);
        }
    }

    public List<String> getPathAsList() {
        List<String> list = new ArrayList<String>();
        recordPath(list);
        return list;
    }

    private void recordPath(List<String> list) {
        if (parent != null) {
            parent.recordPath(list);
        }

        if (name != null) {
            list.add(name);
        }
    }

    public int getDepth() {
        int depth = 1;
        DistributionContentItem ancestor = parent;
        while (ancestor != null) {
            depth++;
            ancestor = ancestor.parent;
        }
        return depth;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (parent == null ? 0 : parent.hashCode());
        result = 31 * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = this == obj;
        if (!result && obj instanceof DistributionContentItem) {
            DistributionContentItem other = (DistributionContentItem) obj;
            result = (parent == other.parent) || (parent != null && parent.equals(other.parent));
            result = result && (same(name, other.name) || (name != null && name.equals(other.name)));
        }
        return result;
    }

    @Override
    public int compareTo(DistributionContentItem o) {

        // If the two items are at different depths in the tree, compare their ancestors at the same depth
        int myDepth = getDepth();
        int theirDepth = o.getDepth();
        if (myDepth != theirDepth) {
            DistributionContentItem me = this;
            DistributionContentItem them = o;
            if (myDepth < theirDepth) {
                them = o.getAncestor(theirDepth - myDepth);
            } else if (theirDepth < myDepth) {
                me = getAncestor(myDepth - theirDepth);
            }

            int result = me.compareTo(them);
            if (result != 0) {
                return result;
            } else {
                return myDepth > theirDepth ? 1 : -1;
            }
        }

        // Same depth
        if (parent != o.parent) {
            if (parent == null) {
                return -1;
            } else if (o.parent == null) {
                return 1;
            } else {
                int result = parent.compareTo(o.parent);
                if (result != 0) {
                    return result;
                }
            }
        }

        if (same(name, o.name)) {
            return 0;
        } else if (name == null) {
            return -1;
        } else if (o.name == null) {
            return 1;
        }
        return name.compareTo(o.name);
    }

    private DistributionContentItem getAncestor(int generationsAbove) {
        DistributionContentItem ancestor = this;
        for (int i = 0; i < generationsAbove; i++) {
            ancestor = ancestor.parent;
        }
        return ancestor;
    }

    // Fool the Intellij code analysis feature that complains about object identity comparisons between Strings
    private static boolean same(Object a, Object b) {
        return a == b;
    }

    interface Filter {

        boolean accept(DistributionContentItem item);

    }

    /**
     * Uses glob based includes and excludes to determine whether to export.
     *
     * @author John E. Bailey
     * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
     */
    static class GlobPathFilter implements Filter {

        private final String glob;
        private final Pattern pattern;

        /**
         * Construct a new instance.
         *
         * @param glob the path glob to match
         */
        GlobPathFilter(final String glob) {
            pattern = getGlobPattern(glob);
            this.glob = glob;
        }

        @Override
        public boolean accept(final DistributionContentItem item) {
            return accept(item.getPath());
        }

        /**
         * Determine whether a path should be accepted.
         *
         * @param path the path to check
         * @return true if the path should be accepted, false if not
         */
        public boolean accept(final String path) {
            return pattern.matcher(path).matches();
        }

        public int hashCode() {
            return glob.hashCode() + 13;
        }

        public boolean equals(final Object obj) {
            return obj instanceof GlobPathFilter && equals((GlobPathFilter) obj);
        }

        public boolean equals(final GlobPathFilter obj) {
            return obj != null && obj.pattern.equals(pattern);
        }

        public String toString() {
            final StringBuilder b = new StringBuilder();
            b.append("match ");
            if (glob != null) {
                b.append('"').append(glob).append('"');
            } else {
                b.append('/').append(pattern).append('/');
            }
            return b.toString();
        }
    }

    private static final Pattern GLOB_PATTERN = Pattern.compile("(\\*\\*?)|(\\?)|(\\\\.)|(/+)|([^*?]+)");

    /**
     * Get a regular expression pattern which accept any path names which match the given glob.  The glob patterns
     * function similarly to {@code ant} file patterns.  Valid metacharacters in the glob pattern include:
     * <ul>
     * <li><code>"\"</code> - escape the next character (treat it literally, even if it is itself a recognized metacharacter)</li>
     * <li><code>"?"</code> - match any non-slash character</li>
     * <li><code>"*"</code> - match zero or more non-slash characters</li>
     * <li><code>"**"</code> - match zero or more characters, including slashes</li>
     * <li><code>"/"</code> - match one or more slash characters.  Consecutive {@code /} characters are collapsed down into one.</li>
     * </ul>
     * In addition, any glob pattern matches all subdirectories thereof.  A glob pattern ending in {@code /} is equivalent
     * to a glob pattern ending in <code>/**</code> in that the named directory is not itself included in the glob.
     * <p/>
     * <b>See also:</b> <a href="http://ant.apache.org/manual/dirtasks.html#patterns">"Patterns" in the Ant Manual</a>
     *
     * @param glob the glob to match
     * @return the pattern
     */
    private static Pattern getGlobPattern(final String glob) {
        StringBuilder patternBuilder = new StringBuilder();
        final Matcher m = GLOB_PATTERN.matcher(glob);
        boolean lastWasSlash = false;
        while (m.find()) {
            lastWasSlash = false;
            String grp;
            if ((grp = m.group(1)) != null) {
                // match a * or **
                if (grp.length() == 2) {
                    // it's a **
                    patternBuilder.append(".*");
                } else {
                    // it's a *
                    patternBuilder.append("[^/]*");
                }
            } else if ((grp = m.group(2)) != null) {
                // match a '?' glob pattern; any non-slash character
                patternBuilder.append("[^/]");
            } else if ((grp = m.group(3)) != null) {
                // backslash-escaped value
                patternBuilder.append(Pattern.quote(m.group().substring(1)));
            } else if ((grp = m.group(4)) != null) {
                // match any number of / chars
                patternBuilder.append("/+");
                lastWasSlash = true;
            } else {
                // some other string
                patternBuilder.append(Pattern.quote(m.group()));
            }
        }
        if (lastWasSlash) {
            // ends in /, append **
            patternBuilder.append(".*");
        } else {
            patternBuilder.append("(?:/.*)?");
        }
        return Pattern.compile(patternBuilder.toString());
    }

}

