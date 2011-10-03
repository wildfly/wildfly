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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.PathElement;

/**
 * User: jpai
 */
public interface EJB3SubsystemModel {
    String LITE = "lite";
    String NAME = "name";

    String DEFAULT_MDB_INSTANCE_POOL = "default-mdb-instance-pool";
    String DEFAULT_RESOURCE_ADAPTER_NAME = "default-resource-adapter-name";
    String DEFAULT_SLSB_INSTANCE_POOL = "default-slsb-instance-pool";
    String INSTANCE_ACQUISITION_TIMEOUT = "timeout";
    String INSTANCE_ACQUISITION_TIMEOUT_UNIT = "timeout-unit";

    String MAX_POOL_SIZE = "max-pool-size";
    String STRICT_MAX_BEAN_INSTANCE_POOL = "strict-max-bean-instance-pool";

    String CORE_THREADS = "core-threads";
    String MAX_THREADS = "max-threads";
    String RELATIVE_TO = "relative-to";
    String PATH = "path";

    String DEFAULT_STATEFUL_ACCESS_TIMEOUT = "default-stateful-access-timeout";
    String DEFAULT_SINGLETON_ACCESS_TIMEOUT = "default-singleton-access-timeout";

    String SERVICE = "service";
    String TIMER_SERVICE = "timer-service";
    String DEFAULT = "default";

    PathElement TIMER_SERVICE_PATH = PathElement.pathElement(SERVICE, TIMER_SERVICE);


}
