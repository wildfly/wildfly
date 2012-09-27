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

import org.jboss.as.patching.metadata.MiscContentItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
class PatchContents {

    public static final String PATCH = "patch.xml";
    public static final String MODULES = "modules";
    public static final String BUNDLES = "modules";
    public static final String MISC = "misc";

    static Collection<MiscContentItem> getContents(final File directory) {
        final List<MiscContentItem> contents = new ArrayList<MiscContentItem>();
        establishContentList(directory, directory, contents);
        return contents;
    }

    static void establishContentList(final File root, final File directory, final List<MiscContentItem> contents) {
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

    static MiscContentItem createContentItem(final File root, final File file) {
        final String[] relativePath = getPath(root, file);
        final String name = file.getName();
        return new MiscContentItem(name, relativePath, new byte[0]);
    }

    static String[] getPath(final File root, final File file) {
        final List<String> path = new ArrayList<String>();
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
        return path.toArray(new String[path.size()]);
    }

    static List<String> getPath(final File file) {
        final List<String> path = new ArrayList<String>();
        File parent = file.getParentFile();
        while(parent != null) {
            path.add(parent.getName());
            parent = file.getParentFile();
        }
        return path;
    }

}
