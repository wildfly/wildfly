/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.services.path;

import java.io.File;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Abstract superclass for services that return a path.
 *
 * @author Brian Stansberry
 */
public abstract class AbstractPathService implements Service<String> {

    private static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("server", "path");

    public static ServiceName pathNameOf(String pathName) {
        if (pathName == null) {
            throw new IllegalArgumentException("pathName is null");
        }
        return SERVICE_NAME_BASE.append(pathName);
    }

    /**
     * Checks whether the given path looks like an absolute Unix or Windows filesystem pathname <strong>without
     * regard for what the filesystem is underlying the Java Virtual Machine</strong>. A UNIX pathname is
     * absolute if its prefix is <code>"/"</code>.  A Microsoft Windows pathname is absolute if its prefix is a drive
     * specifier followed by <code>"\\"</code>, or if its prefix is <code>"\\\\"</code>.
     * <p>
     * <strong>This method differs from simply creating a new {@code File} and calling {@link File#isAbsolute()} in that
     * its results do not change depending on what the filesystem underlying the Java Virtual Machine is. </strong>
     * </p>
     *
     * @param path the path
     *
     * @return  {@code true} if {@code path} looks like an absolute Unix or Windows pathname
     */
    public static boolean isAbsoluteUnixOrWindowsPath(final String path) {
        if (path != null) {
            int length = path.length();
            if (length > 0) {
                char c0 = path.charAt(0);
                if (c0 == '/') {
                    return true;   // Absolute Unix path
                } else if (length > 1) {
                    char c1 = path.charAt(1);
                    if (c0 == '\\' && c1 == '\\') {
                        return true;   // Absolute UNC pathname "\\\\foo"
                    } else if (length > 2 && c1 == ':' && path.charAt(2) == '\\' && isDriveLetter(c0) ) {
                        return true; // Absolute local pathname "z:\\foo"
                    }
                }

            }
        }
        return false;
    }

    private static boolean isDriveLetter(char c) {
        return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
    }


    private String path;

    // ------------------------------------------------------------  Service

    @Override
    public void start(StartContext context) throws StartException {
        path = resolvePath();
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public String getValue() throws IllegalStateException {
        final String path = this.path;
        if(path == null) {
            throw new IllegalStateException();
        }
        return path;
    }

    // ------------------------------------------------------------  Protected

    protected abstract String resolvePath();
}
