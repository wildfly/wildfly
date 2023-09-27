/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
