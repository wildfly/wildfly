/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 *
 * TODO: do we still need this?
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleConfiguration {
    private final String applicationName;
    private final String moduleName;
    private final List<ComponentConfiguration> componentConfigurations;


    public EEModuleConfiguration(EEModuleDescription description) throws DeploymentUnitProcessingException {
        applicationName = description.getApplicationName();
        moduleName = description.getModuleName();

        this.componentConfigurations = new ArrayList<ComponentConfiguration>();
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Collection<ComponentConfiguration> getComponentConfigurations() {
        return Collections.unmodifiableList(componentConfigurations);
    }

    public void addComponentConfiguration(ComponentConfiguration configuration) {
        componentConfigurations.add(configuration);
    }

}
