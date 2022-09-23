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

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.capability.CapabilityServiceSupport.NoSuchCapabilityException;
import org.jboss.as.jsf.deployment.JsfVersionMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.weld.Capabilities;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;
import org.wildfly.extension.clustering.web.SessionMarshallerFactory;

/**
 * {@link DeploymentUnitProcessor} that attaches any configured {@link DistributableSessionManagementProvider} to the deployment unit.
 * @author Paul Ferraro
 */
public class DistributableWebDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String PROTOSTREAM = "org.infinispan.protostream";
    private static final String EL_EXPRESSLY = "org.wildfly.clustering.el.expressly";
    private static final String WELD_CORE = "org.wildfly.clustering.weld.core";
    private static final String WELD_EJB = "org.wildfly.clustering.weld.ejb";
    private static final String WELD_WEB = "org.wildfly.clustering.weld.web";
    private static final String FACES_MOJARRA = "org.wildfly.clustering.faces.mojarra";
    private static final String UNDERTOW = "org.wildfly.clustering.web.undertow";

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();
        DistributableSessionManagementProvider<DistributableSessionManagementConfiguration<DeploymentUnit>> provider = context.getAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY);
        if (provider != null) {
            unit.putAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY, provider);

            ModuleSpecification specification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
            ModuleLoader loader = Module.getBootModuleLoader();

            if (provider.getSessionManagementConfiguration().getMarshallerFactory() == SessionMarshallerFactory.PROTOSTREAM) {
                specification.addSystemDependency(new ModuleDependency(loader, PROTOSTREAM, false, false, false, false));
                specification.addSystemDependency(new ModuleDependency(loader, UNDERTOW, false, false, true, false));

                CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                if (support.hasCapability(Capabilities.WELD_CAPABILITY_NAME)) {
                    try {
                        WeldCapability weldCapability = support.getCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY_NAME, WeldCapability.class);
                        if (weldCapability.isPartOfWeldDeployment(unit)) {
                            specification.addSystemDependency(new ModuleDependency(loader, EL_EXPRESSLY, false, false, true, false));
                            specification.addSystemDependency(new ModuleDependency(loader, WELD_CORE, false, false, true, false));
                            specification.addSystemDependency(new ModuleDependency(loader, WELD_EJB, false, false, true, false));
                            specification.addSystemDependency(new ModuleDependency(loader, WELD_WEB, false, false, true, false));
                        }
                    } catch (NoSuchCapabilityException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }

            if (JsfVersionMarker.getVersion(unit).equals(JsfVersionMarker.JSF_2_0)) {
                specification.addSystemDependency(new ModuleDependency(loader, EL_EXPRESSLY, false, false, true, false));
                specification.addSystemDependency(new ModuleDependency(loader, FACES_MOJARRA, false, false, true, false));
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        unit.removeAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY);
    }
}
