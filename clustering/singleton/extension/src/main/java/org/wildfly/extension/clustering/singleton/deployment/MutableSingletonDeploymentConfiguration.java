/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Mutable configuration for a singleton deployment.
 * @author Paul Ferraro
 */
public class MutableSingletonDeploymentConfiguration implements SingletonDeploymentConfiguration {

    private final PropertyReplacer replacer;

    private volatile String policy;

    public MutableSingletonDeploymentConfiguration(DeploymentUnit unit) {
        this(JBossDescriptorPropertyReplacement.propertyReplacer(unit));
    }

    public MutableSingletonDeploymentConfiguration(PropertyReplacer replacer) {
        this.replacer = replacer;
    }

    public void setPolicy(String value) {
        this.policy = this.replacer.replaceProperties(value);
    }

    @Override
    public String getPolicy() {
        return this.policy;
    }
}
