/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.webservices.deployers;

import static org.jboss.ws.common.integration.WSHelper.getRequiredAttachment;

import java.util.Map;

import javax.xml.namespace.QName;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.webserviceref.WSRefRegistry;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;

/**
 * DeploymentAspect to set deployed ServiceName and address map in unifiedServiceRefMetaData
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public class UnifiedServiceRefDeploymentAspect extends AbstractDeploymentAspect {
    @Override
    public void start(final Deployment dep) {
        final DeploymentUnit unit = getRequiredAttachment(dep, DeploymentUnit.class);
        WSRefRegistry wsRefRegistry = ASHelper.getWSRefRegistry(unit);
        Object obj = dep.getProperty("ServiceAddressMap");
        if(obj != null) {
            @SuppressWarnings("unchecked")
            Map<QName, String> deployedPortsAddress = (Map<QName, String>)obj;
            for (UnifiedServiceRefMetaData metaData : wsRefRegistry.getUnifiedServiceRefMetaDatas()) {
                 metaData.addDeployedServiceAddresses(deployedPortsAddress);
            }
        }
    }
}
