/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jdr.util;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import java.util.List;

public class FSTree {
    int directoryCount = 0;
    int fileCount = 0;
    StringBuilder buf = new StringBuilder();
    String topDirectory = null;
    String fmt = "%s%s%s %s";

    public FSTree(String root) throws Exception {
        this.traverse(root, "", true);
    }

    private static double div(long left, long right) {
        return (double)left / (double)right;
    }

    private static String formatBytes(long size) {

        if (size > Utils.ONE_TB) {
            return String.format("%.1fT", div(size, Utils.ONE_TB));
        } else if (size > Utils.ONE_GB) {
            return String.format("%.1fG", div(size, Utils.ONE_GB));
        } else if (size > Utils.ONE_MB) {
            return String.format("%.1fM", div(size, Utils.ONE_MB));
        } else if (size > Utils.ONE_KB) {
            return String.format("%.1fK", div(size, Utils.ONE_KB));
        } else {
            return String.format("%d", size);
        }
    }

    private void traverse(String dir, String padding) throws java.io.IOException {
        traverse(dir, padding, false);
    }

    private void append(VirtualFile f, String padding) {
        String baseName = f.getName();
        String size = formatBytes(f.getSize());
        buf.append(String.format(fmt, padding, "+-- ", size, baseName));
        buf.append("\n");
    }

    private void traverse(String dir, String padding, boolean first)
        throws java.io.IOException {
        VirtualFile path = VFS.getChild(dir);

        if (!first) {
            String _p = padding.substring(0, padding.length() -1);
            append(path, _p);
            padding += "   ";
        }
        else {
            buf.append(path.getName());
            buf.append("\n");
        }

        int count = 0;
        List<VirtualFile> files = path.getChildren();
        for (VirtualFile f : files ) {
            count += 1;

            if (f.getPathName().startsWith(".")) {
                continue;
            }
            else if (f.isFile()) {
                append(f, padding);
            }
            else if (Utils.isSymlink(f)) {
                buf.append(padding);
                buf.append("+-- ");
                buf.append(f.getName());
                buf.append(" -> ");
                buf.append(f.getPathName());
                buf.append("\n");
            }
            else if (f.isDirectory()) {
                if (count == files.size()) {
                    traverse(f.getPathName(), padding + " ");
                }
                else {
                    traverse(f.getPathName(), padding + "|");
                }
            }
        }
    }

    public String toString() {
        return buf.toString();
    }
}
