/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web.deployment;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.property.PropertyReplacer;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;

/**
 * @author Paul Ferraro
 */
public class MutableDistributableDeploymentConfiguration implements DistributableWebDeploymentConfiguration, UnaryOperator<String>, Consumer<String> {

    private final List<String> immutableClasses = new LinkedList<>();
    private final PropertyReplacer replacer;

    private String managementName;
    private DistributableSessionManagementProvider<? extends DistributableSessionManagementConfiguration<DeploymentUnit>> management;

    public MutableDistributableDeploymentConfiguration() {
        this.replacer = null;
    }

    public MutableDistributableDeploymentConfiguration(DeploymentUnit unit) {
        this.replacer = JBossDescriptorPropertyReplacement.propertyReplacer(unit);
    }

    @Override
    public DistributableSessionManagementProvider<? extends DistributableSessionManagementConfiguration<DeploymentUnit>> getSessionManagement() {
        return this.management;
    }

    public void setSessionManagement(DistributableSessionManagementProvider<? extends DistributableSessionManagementConfiguration<DeploymentUnit>> management) {
        this.management = management;
    }

    @Override
    public String getSessionManagementName() {
        return this.managementName;
    }

    public void setSessionManagementName(String value) {
        this.managementName = this.apply(value);
    }

    @Override
    public List<String> getImmutableClasses() {
        return Collections.unmodifiableList(this.immutableClasses);
    }

    @Override
    public void accept(String className) {
        this.immutableClasses.add(className);
    }

    @Override
    public String apply(String value) {
        return (this.replacer != null) ? this.replacer.replaceProperties(value) : value;
    }
}
