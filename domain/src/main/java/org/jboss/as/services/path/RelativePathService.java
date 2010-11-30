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

package org.jboss.as.services.path;

import java.io.File;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * {@link AbstractPathService} implementation for paths that are relative
 * to other paths.
 *
 * @author Brian Stansberry
 */
public class RelativePathService extends AbstractPathService {

    private final String relativePath;
    private final InjectedValue<String> injectedPath = new InjectedValue<String>();

    public static void addService(final String name, final String relativePath,
            final String relativeTo, final ServiceTarget target) {
        addService(pathNameOf(name), relativePath, relativeTo, target);
    }

    public static void addService(final ServiceName name, final String relativePath,
            final String relativeTo, final ServiceTarget target) {
        RelativePathService service = new RelativePathService(relativePath);
        target.addService(name, service)
            .addDependency(pathNameOf(relativeTo), String.class, service.injectedPath)
            .install();
    }

    public RelativePathService(final String relativePath) {
        if (relativePath == null) {
            throw new IllegalArgumentException("relativePath is null");
        }
        if (relativePath.length() == 0) {
            throw new IllegalArgumentException("relativePath is empty");
        }
        if (relativePath.charAt(0) == '/') {
            if (relativePath.length() == 1) {
                throw new IllegalArgumentException("Invalid relativePath value '/'");
            }
            this.relativePath = relativePath.substring(1);
        }
        else if (relativePath.indexOf(":\\") == 1) {
            throw new IllegalArgumentException(relativePath + " is a Windows absolute path");
        }
        else {
            if(isWindows()) {
                this.relativePath = relativePath.replace("/", File.separator);
            } else {
                this.relativePath = relativePath.replace("\\", File.separator);
            }
        }
    }

    @Override
    protected String resolvePath() {
        String base = injectedPath.getValue();
        base = base.endsWith(File.separator) ? base.substring(0, base.length() -1) : base;
        String relative = relativePath.startsWith(File.separator) ? relativePath.substring(0, relativePath.length() -1) : relativePath;
        return base + File.separatorChar + relative;
    }

    private static boolean isWindows(){
        return File.separatorChar == '\\';
    }

}
