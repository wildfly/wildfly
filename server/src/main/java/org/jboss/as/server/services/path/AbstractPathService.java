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

package org.jboss.as.server.services.path;

import org.jboss.msc.service.ServiceName;

/**
 * Abstract superclass for services that return a path.
 *
 * @author Brian Stansberry
 * @author Kabir Khan
 * @deprecated Use {@link org.jboss.as.controller.services.path.AbsolutePathService} instead. This class is here for backwards compatibility with third-party subsystems
 */
@Deprecated
public abstract class AbstractPathService extends org.jboss.as.controller.services.path.AbstractPathService {

    @Deprecated
    public static ServiceName pathNameOf(String pathName) {
        return org.jboss.as.controller.services.path.AbstractPathService.pathNameOf(pathName);
    }

    @Deprecated
    public static boolean isAbsoluteUnixOrWindowsPath(final String path) {
        return org.jboss.as.controller.services.path.AbstractPathService.isAbsoluteUnixOrWindowsPath(path);
    }
}
