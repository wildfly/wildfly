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
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ConnectorServices {

    public static final ServiceName CONNECTOR_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "config");

    public static final ServiceName DEFAULT_BOOTSTRAP_CONTEXT_SERVICE = ServiceName.JBOSS.append("connector",
            "defaultbootstrapcontext");

    public static final ServiceName TRANSACTION_INTEGRATION_SERVICE = ServiceName.JBOSS.append("connector",
            "transactionintegration");

    public static final ServiceName WORKMANAGER_SERVICE = ServiceName.JBOSS.append("connector", "workmanager");

    public static final ServiceName RESOURCE_ADAPTER_SERVICE_PREFIX = ServiceName.JBOSS.append("ra");

    public static final ServiceName RESOURCE_ADAPTER_XML_SERVICE_PREFIX = ServiceName.JBOSS.append("raxml");

    public static final ServiceName RESOURCE_ADAPTER_REGISTRY_SERVICE = ServiceName.JBOSS.append("raregistry");

    public static final ServiceName RESOURCE_ADAPTER_ACTIVATOR_SERVICE = ServiceName.JBOSS.append("raactivator");

    /** MDR service name **/
    public static final ServiceName IRONJACAMAR_MDR = ServiceName.JBOSS.append("ironjacamar", "mdr");

    public static final ServiceName RA_REPOSISTORY_SERVICE = ServiceName.JBOSS.append("rarepository");

    public static final ServiceName MANAGEMENT_REPOSISTORY_SERVICE = ServiceName.JBOSS.append("management_repository");

    public static final ServiceName RESOURCEADAPTERS_SERVICE = ServiceName.JBOSS.append("resourceadapters");

    public static final ServiceName DATASOURCES_SERVICE = ServiceName.JBOSS.append("datasources");

    public static final ServiceName JDBC_DRIVER_REGISTRY_SERVICE = ServiceName.JBOSS.append("jdbc-driver", "registry");

    public static final ServiceName CCM_SERVICE = ServiceName.JBOSS.append("cached-connection-manager");

    private ConnectorServices() {
    }

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
}
