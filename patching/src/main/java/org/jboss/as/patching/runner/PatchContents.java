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

package org.jboss.as.patching.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Emanuel Muckenhuber
 */
class PatchContents {

    public static final String MODULES = "modules";
    public static final String FILES = "files";

    static Collection<ContentItem> getContents(final File directory) {
        final List<ContentItem> contents = new ArrayList<ContentItem>();
        establishContentList(directory, directory, contents);
        return contents;
    }

    static void establishContentList(final File root, final File directory, final List<ContentItem> contents) {
        final File[] children = directory.listFiles();
        if(children != null && children.length > 0) {
            for(final File child : children) {
                if(child.isDirectory()) {
                    establishContentList(root, child, contents);
                } else {
                    contents.add(createContentItem(root, child));
                }
            }
        }
    }

    static ContentItem createContentItem(final File root, final File file) {
        final Set<String> relativePath = getPath(root, file);
        final String name = file.getName();
        return new ContentItem() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Set<String> getPath() {
                return relativePath;
            }
        };
    }

    static Set<String> getPath(final File root, final File file) {
        final Set<String> path = new HashSet<String>();
        final Iterator<String> rootPath = getPath(root).iterator();
        final Iterator<String> filePath = getPath(file).iterator();
        while(rootPath.hasNext()) {
            assert filePath.hasNext();
            String rp = rootPath.next();
            String fp = filePath.next();
            assert rp.equals(fp);
        }
        while(filePath.hasNext()) {
            final String p = filePath.next();
            path.add(p);
        }
        return path;
    }

    static Set<String> getPath(final File file) {
        final Set<String> path = new HashSet<String>();
        File parent = file.getParentFile();
        while(parent != null) {
            path.add(parent.getName());
            parent = file.getParentFile();
        }
        return path;
    }


}
