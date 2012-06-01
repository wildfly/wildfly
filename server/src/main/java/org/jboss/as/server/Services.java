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

package org.jboss.as.server;

import java.util.concurrent.ExecutorService;

import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * A holder class for constants containing the names of the core services.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Services {

    private Services() {
    }

    /**
     * The service name of the root application server service.
     */
    public static final ServiceName JBOSS_AS = ServiceName.JBOSS.append("as");

    /**
     * The service corresponding to the {@link org.jboss.as.controller.ModelController} for this instance.
     */
    public static final ServiceName JBOSS_SERVER_CONTROLLER = JBOSS_AS.append("server-controller");

    /**
     * The service corresponding to the {@link java.util.concurrent.ExecutorService} for this instance.
     */
    static final ServiceName JBOSS_SERVER_EXECUTOR = JBOSS_AS.append("server-executor");

    /**
     * The service corresponding to the {@link ServiceModuleLoader} for this instance.
     */
    public static final ServiceName JBOSS_SERVICE_MODULE_LOADER = JBOSS_AS.append("service-module-loader");

    /**
     * The service corresponding to the {@link ExternalModuleService} for this instance.
     */
    public static final ServiceName JBOSS_EXTERNAL_MODULE_SERVICE = JBOSS_AS.append("external-module-service");

    /**
     * The service that caches system module jandex indexes
     */
    public static final ServiceName JBOSS_MODULE_INDEX_SERVICE = JBOSS_AS.append("module-index-service");

    public static void addServerExecutorDependency(ServiceBuilder<?> builder, Injector<ExecutorService> injector, boolean optional) {
        ServiceBuilder.DependencyType type = optional ? ServiceBuilder.DependencyType.OPTIONAL : ServiceBuilder.DependencyType.REQUIRED;
        builder.addDependency(type, JBOSS_SERVER_EXECUTOR, ExecutorService.class, injector);
    }
}
