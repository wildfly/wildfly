/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processor;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.spi.DeploymentUnitDependenciesProvider;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.msc.service.ServiceName;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * @author Martin Kouba
 */
public class JpaDependenciesProvider implements DeploymentUnitDependenciesProvider {

    @Override
    public Set<ServiceName> getDependencies(DeploymentUnit deploymentUnit) {
        Set<ServiceName> dependencies = new HashSet<>();
        EarMetaData earConfig = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
        // WFLY-14923 handle initialize-in-order by only adding (top level deployment) persistence units dependencies to WeldStartService.
        // with initialize-in-order enabled WeldStartService cannot depend on persistence units contained in sub-deployments as that
        // may violate the initialize-in-order ordering and lead to deployment failures.
        if (earConfig != null && earConfig.getInitializeInOrder() && earConfig.getModules().size() > 1) {
            // Only add Jakarta EE component dependencies on all persistence units in top level deployment unit.
            if (deploymentUnit.getParent() == null) {
                for (ResourceRoot root : DeploymentUtils.allResourceRoots(deploymentUnit)) {
                    // Only process resources that aren't subdeployments
                    if (!SubDeploymentMarker.isSubDeployment(root)) {
                        addDependencyOnPersistenceUnit(dependencies, root.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS));
                    }
                }
            }
        } else {
            // handle when the `initialize-in-order` feature is not enabled.
            for (ResourceRoot root : DeploymentUtils.allResourceRoots(deploymentUnit)) {
                addDependencyOnPersistenceUnit(dependencies, root.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS));
            }
        }
        return dependencies;
    }

    private void addDependencyOnPersistenceUnit(Set<ServiceName> dependencies, PersistenceUnitMetadataHolder persistenceUnits) {
        if (persistenceUnits != null && persistenceUnits.getPersistenceUnits() != null) {
            for (final PersistenceUnitMetadata pu : persistenceUnits.getPersistenceUnits()) {
                final Properties properties = pu.getProperties();
                final String jpaContainerManaged = properties.getProperty(Configuration.JPA_CONTAINER_MANAGED);
                final boolean deployPU = (jpaContainerManaged == null || Boolean.parseBoolean(jpaContainerManaged));
                if (deployPU) {
                    final ServiceName serviceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
                    dependencies.add(serviceName);
                }
            }
        }
    }
}
