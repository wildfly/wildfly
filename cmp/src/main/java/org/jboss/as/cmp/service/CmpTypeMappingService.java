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

package org.jboss.as.cmp.service;

import org.jboss.as.cmp.jdbc.metadata.JDBCTypeMappingMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * @author John Bailey
 */
public class CmpTypeMappingService implements Service<JDBCTypeMappingMetaData> {
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("cmp", "type-mapping");

    public static ServiceController<?> addService(final ServiceTarget target, final JDBCTypeMappingMetaData typeMappingMetaData) {
        return target.addService(SERVICE_NAME_BASE.append(typeMappingMetaData.getName()), new CmpTypeMappingService(Values.immediateValue(typeMappingMetaData)))
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }

    private final Value<JDBCTypeMappingMetaData> value;

    private JDBCTypeMappingMetaData typeMapping;

    public CmpTypeMappingService(final Value<JDBCTypeMappingMetaData> value) {
        this.value = value;
    }

    public synchronized void start(final StartContext startContext) throws StartException {
        typeMapping = value.getValue();
    }

    public synchronized void stop(final StopContext stopContext) {
        this.typeMapping = null;
    }

    public synchronized JDBCTypeMappingMetaData getValue() throws IllegalStateException, IllegalArgumentException {
        return typeMapping;
    }
}
