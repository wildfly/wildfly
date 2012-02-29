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

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * {@link AbstractPathService} implementation for paths that are not relative
 * to other paths.
 *
 * @author Brian Stansberry
 * @author Kabir Khan
 * @deprecated Use {@link org.jboss.as.controller.services.path.AbsolutePathService} instead. This class is here for backwards compatibility with third-party subsystems
 */
public class AbsolutePathService extends org.jboss.as.controller.services.path.AbsolutePathService {

    @Deprecated
    public static ServiceController<String> addService(final String name, final String abstractPath, final ServiceTarget serviceTarget) {
        return org.jboss.as.controller.services.path.AbsolutePathService.addService(name, abstractPath, serviceTarget);
    }

    @Deprecated
    public static ServiceController<String> addService(final String name, final String abstractPath,
                                                       final ServiceTarget serviceTarget, final List<ServiceController<?>> newControllers,
                                                       final ServiceListener... listeners) {
        return org.jboss.as.controller.services.path.AbsolutePathService.addService(name, abstractPath, serviceTarget, newControllers, listeners);
    }

    @Deprecated
    public static ServiceController<String> addService(final ServiceName sname, final String abstractPath, final ServiceTarget serviceTarget) {
        return org.jboss.as.controller.services.path.AbsolutePathService.addService(sname, abstractPath, serviceTarget);
    }

    @Deprecated
    public static ServiceController<String> addService(final ServiceName sname, final String abstractPath,
                                                       final ServiceTarget serviceTarget, final List<ServiceController<?>> newControllers,
                                                       final ServiceListener... listeners) {
        return org.jboss.as.controller.services.path.AbsolutePathService.addService(sname, abstractPath, serviceTarget, newControllers, listeners);
    }

    @Deprecated
    public static void addService(final ServiceName name, final ModelNode element, final ServiceTarget serviceTarget) {
        org.jboss.as.controller.services.path.AbsolutePathService.addService(name, element, serviceTarget);
    }

    @Deprecated
    public AbsolutePathService(final String abstractPath) {
        super(abstractPath);
    }

}
