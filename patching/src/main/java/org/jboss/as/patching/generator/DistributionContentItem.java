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

package org.jboss.as.patching.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Descriptive information about an item of content within a distribution.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class DistributionContentItem implements Comparable<DistributionContentItem> {

    private static final String PATH_DELIMITER = "/";

    public enum Type {
        /** The root of a distribution */
        DISTRIBUTION_ROOT(true),
        /** A directory within the bundle storage area that is not the final root directory of a bundle */
        BUNDLE_PARENT(true),
        /** A directory within the bundle storage area that is the final root directory of a bundle */
        BUNDLE_ROOT(true),
        /** Content within a bundle */
        BUNDLE_CONTENT(true),
        /** A directory within the module storage area that is not the final root directory of a module */
        MODULE_PARENT(true),
        /** A directory within the module storage area that is the final root directory of a module */
        MODULE_ROOT(true),
        /** Content within a module that is not the module descriptor file */
        MODULE_CONTENT(true),
        /** A miscellaneous file within the distribution */
        MISC(true),
        /** A file that can be ignored in patch processing */
        IGNORED(false),
        ;

        private final boolean hasRelevantChildren;
        private Type(boolean hasRelevantChildren) {
            this.hasRelevantChildren = hasRelevantChildren;
        }

        public boolean getHasRelevantChildren() {
            return hasRelevantChildren;
        }
    }

    public static DistributionContentItem createDistributionRoot() {
        return new DistributionContentItem(null, Type.DISTRIBUTION_ROOT, null, true);
    }

    public static DistributionContentItem createMiscItemForPath(String path) {
        DistributionContentItem result = createDistributionRoot();
        final String[] s = path.split(PATH_DELIMITER);
        final int length = s.length;
        for (int i = 0; i < length; i++) {
            boolean dir = i < length - 1;
            result = new DistributionContentItem(s[i], Type.MISC, result, dir);
        }
        return result;
    }

    private final String name;
    private final Type type;
    private final boolean directory;
    private final DistributionContentItem parent;

    DistributionContentItem(final String name, final Type type, final DistributionContentItem parent, boolean directory) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.directory = directory;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public boolean isDirectory() {
        return directory;
    }

    public DistributionContentItem getParent() {
        return parent;
    }

    public DistributionContentItem getRelativeItem(DistributionContentItem ancestor) {
        if (ancestor.equals(parent)) {
            return new DistributionContentItem(name, type, null, directory);
        } else if (parent == null) {
            throw new IllegalArgumentException();
        } else {
            DistributionContentItem relativeParent = parent.getRelativeItem(ancestor);
            return new DistributionContentItem(name, type, relativeParent, directory);
        }
    }

    public File getFile(File rootFile) {
        if (parent == null) {
            return rootFile;
        } else {
            return new File(parent.getFile(rootFile), name);
        }
    }

    public String getPath() {
        return getPath('/');
    }

    public String getPath(char delimiter) {
        StringBuilder sb = new StringBuilder();
        recordPath(sb, delimiter);
        return sb.toString();
    }

    private void recordPath(StringBuilder sb, char delimiter) {
        if (parent != null) {
            parent.recordPath(sb, delimiter);
            if (parent.getType() != Type.DISTRIBUTION_ROOT) {
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
}
