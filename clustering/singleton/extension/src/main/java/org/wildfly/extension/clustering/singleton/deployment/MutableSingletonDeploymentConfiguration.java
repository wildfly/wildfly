/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

    public MutableSingletonDeploymentConfiguration() {
        this.replacer = null;
    }

    public MutableSingletonDeploymentConfiguration(DeploymentUnit unit) {
        this(JBossDescriptorPropertyReplacement.propertyReplacer(unit));
    }

    public MutableSingletonDeploymentConfiguration(PropertyReplacer replacer) {
        this.replacer = replacer;
    }

    public void setPolicy(String value) {
        this.policy = (this.replacer != null) ? this.replacer.replaceProperties(value) : value;
    }

    @Override
    public String getPolicy() {
        return this.policy;
    }
}
