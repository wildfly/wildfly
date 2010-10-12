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

package org.jboss.as.connector;

import org.jboss.msc.service.ServiceName;

/**
 * A ConnectorServices.
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public final class ConnectorServices {

    public static final ServiceName CONNECTOR_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "config");

    public static final ServiceName RESOURCE_ADAPTER_SERVICE_PREFIX = ServiceName.JBOSS.append("ra");

    /**
     * convenient method to check notNull of value
     * @param <T> type of the value
     * @param value the value
     * @return the value or throw an {@link IllegalStateException} if value is
     *         null (a.k.a. service not started)
     */
    public static <T> T notNull(T value) {
        if (value == null)
            throw new IllegalStateException("Service not started");
        return value;
    }

    private ConnectorServices() {
    }
}
