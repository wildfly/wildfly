/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.service;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Returns the value supplied by a {@link Service}, starting if necessary.
 * If used within the context of a management operation, the requisite {@link ServiceRegistry} should be obtained via:
 * <code>OperationContext.getServiceRegistry(true)</code>, requiring an exclusive lock on the service registry,
 * since the temporary service installed might force the target service to change state.
 * @author Paul Ferraro
 */
@Deprecated
public class ActiveServiceSupplier<T> extends ServiceSupplier<T> {

    public ActiveServiceSupplier(ServiceRegistry target, ServiceName name) {
        super(target, name, ServiceController.Mode.ACTIVE);
    }
}
