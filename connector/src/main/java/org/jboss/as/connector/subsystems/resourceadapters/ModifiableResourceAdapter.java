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

package org.jboss.as.connector.subsystems.resourceadapters;

import java.util.List;
import java.util.Map;

import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.metadata.resourceadapter.ResourceAdapterImpl;
import org.jboss.msc.service.ServiceName;


public class ModifiableResourceAdapter extends ResourceAdapterImpl {

    private volatile ServiceName raXmlDeploymentServiceName = null;

    public ModifiableResourceAdapter(String archive, TransactionSupportEnum transactionSupport, List<CommonConnDef> connectionDefinitions, List<CommonAdminObject> adminObjects, Map<String, String> configProperties, List<String> beanValidationGroups, String bootstrapContext) {
        super(archive, transactionSupport, connectionDefinitions, adminObjects, configProperties, beanValidationGroups, bootstrapContext);
    }

    public synchronized void addConfigProperty(String name, String value) {
        configProperties.put(name, value);
    }

    public synchronized void addConnectionDefinition(CommonConnDef value) {
        connectionDefinitions.add(value);
    }

    public synchronized int connectionDefinitionSize() {
            return connectionDefinitions.size();
    }

    public synchronized void addAdminObject(CommonAdminObject value) {
        adminObjects.add(value);
    }

    public synchronized int adminObjectSize() {
        return adminObjects.size();
    }

    public ServiceName getRaXmlDeploymentServiceName() {
        return raXmlDeploymentServiceName;
    }

    public void setRaXmlDeploymentServiceName(ServiceName raXmlDeploymentServiceName) {
        this.raXmlDeploymentServiceName = raXmlDeploymentServiceName;
    }
}
