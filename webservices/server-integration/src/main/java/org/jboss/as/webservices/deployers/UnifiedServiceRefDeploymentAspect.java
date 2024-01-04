/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
