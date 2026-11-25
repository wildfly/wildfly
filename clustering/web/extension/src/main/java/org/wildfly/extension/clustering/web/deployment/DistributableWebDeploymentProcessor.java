/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.wildfly.extension.clustering.web.SessionMarshallerFactory;

/**
 * {@link DeploymentUnitProcessor} that attaches any configured {@link DistributableSessionManagementProvider} to the deployment unit.
 * @author Paul Ferraro
 */
public class DistributableWebDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String WEB_API = "org.wildfly.clustering.web.api";
    private static final String MARSHALLING_API = "org.wildfly.clustering.marshalling.api";
    private static final String PROTOSTREAM = "org.infinispan.protostream";
    private static final String EJB_CLIENT = "org.wildfly.clustering.ejb.client";
    private static final String EL_EXPRESSLY = "org.wildfly.clustering.el.expressly";
    private static final String WELD_CORE = "org.wildfly.clustering.weld.core";
    private static final String WELD_EJB = "org.wildfly.clustering.weld.ejb";
    private static final String WELD_WEB = "org.wildfly.clustering.weld.web";
    private static final String FACES_API = "org.wildfly.clustering.faces.api";
    private static final String FACES_MOJARRA = "org.wildfly.clustering.faces.mojarra";
    private static final String UNDERTOW = "org.wildfly.clustering.web.undertow";

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();
        DistributableSessionManagementProvider provider = context.getAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY);
        if (provider != null) {
            unit.putAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY, provider);

            ModuleSpecification specification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
            ModuleLoader loader = Module.getBootModuleLoader();

            specification.addSystemDependency(ModuleDependency.Builder.of(loader, WEB_API).build());

            if (provider.getSessionManagementConfiguration().getMarshallerFactory() == SessionMarshallerFactory.PROTOSTREAM) {
                specification.addSystemDependency(ModuleDependency.Builder.of(loader, PROTOSTREAM).build());
                specification.addSystemDependency(ModuleDependency.Builder.of(loader, UNDERTOW).setImportServices(true).build());

                CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                if (support.hasCapability(Capabilities.WELD_CAPABILITY_NAME)) {
                    try {
                        WeldCapability weldCapability = support.getCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY_NAME, WeldCapability.class);
                        if (weldCapability.isPartOfWeldDeployment(unit)) {
                            specification.addSystemDependency(ModuleDependency.Builder.of(loader, EJB_CLIENT).setImportServices(true).build());
                            specification.addSystemDependency(ModuleDependency.Builder.of(loader, EL_EXPRESSLY).setImportServices(true).build());
                            specification.addSystemDependency(ModuleDependency.Builder.of(loader, WELD_CORE).setImportServices(true).build());
                            specification.addSystemDependency(ModuleDependency.Builder.of(loader, WELD_EJB).setImportServices(true).build());
                            specification.addSystemDependency(ModuleDependency.Builder.of(loader, WELD_WEB).setImportServices(true).build());
                        }
                    } catch (NoSuchCapabilityException e) {
                        throw new IllegalStateException(e);
                    }
                }
            } else {
                specification.addSystemDependency(ModuleDependency.Builder.of(loader, MARSHALLING_API).build());
            }

            if (JsfVersionMarker.getVersion(unit).equals(JsfVersionMarker.JSF_4_0)) {
                specification.addSystemDependency(ModuleDependency.Builder.of(loader, EL_EXPRESSLY).setImportServices(true).build());
                specification.addSystemDependency(ModuleDependency.Builder.of(loader, FACES_API).setImportServices(true).build());
                specification.addSystemDependency(ModuleDependency.Builder.of(loader, FACES_MOJARRA).setImportServices(true).build());
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        unit.removeAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY);
    }
}
