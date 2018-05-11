/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EJBSecurityDomainService;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.security.ApplicationSecurityDomainConfig;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainDefinition;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.ApplicationSecurityDomain;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;

/**
 * A {@link DeploymentUnitProcessor} which looks for {@link EJBComponentDescription}s in the deployment
 * unit and sets the default security domain name, that's configured at the EJB subsystem level,
 * {@link EJBComponentDescription#setDefaultSecurityDomain(String) to each of the EJB component descriptions}.
 *
 * @author Jaikiran Pai
 */
public class EJBDefaultSecurityDomainProcessor implements DeploymentUnitProcessor {

    private volatile String defaultSecurityDomainName;
    private volatile Function<String, ApplicationSecurityDomainConfig> knownSecurityDomain;
    private volatile BooleanSupplier outflowSecurityDomainsConfigured;

    public EJBDefaultSecurityDomainProcessor(final String defaultSecurityDomainName, final Function<String, ApplicationSecurityDomainConfig> knownSecurityDomain, final BooleanSupplier outflowSecurityDomainsConfigured) {
        this.defaultSecurityDomainName = defaultSecurityDomainName;
        this.knownSecurityDomain = knownSecurityDomain;
        this.outflowSecurityDomainsConfigured = outflowSecurityDomainsConfigured;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(EE_MODULE_DESCRIPTION);
        if (eeModuleDescription == null) {
            return;
        }
        final Collection<ComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        if (componentDescriptions == null || componentDescriptions.isEmpty()) {
            return;
        }
        final String defaultSecurityDomain;
        if(eeModuleDescription.getDefaultSecurityDomain() == null) {
            defaultSecurityDomain = this.defaultSecurityDomainName;
        } else {
            defaultSecurityDomain = eeModuleDescription.getDefaultSecurityDomain();
        }

        String knownSecurityDomainName = null;
        String defaultKnownSecurityDomainName = null;
        for (ComponentDescription componentDescription : componentDescriptions) {
            if (componentDescription instanceof EJBComponentDescription) {
                EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
                ejbComponentDescription.setDefaultSecurityDomain(defaultSecurityDomain);
                ejbComponentDescription.setKnownSecurityDomainFunction(knownSecurityDomain);
                ejbComponentDescription.setOutflowSecurityDomainsConfigured(outflowSecurityDomainsConfigured);

                // Ensure the EJB components within a deployment are associated with at most one Elytron security domain
                if (ejbComponentDescription.isSecurityDomainKnown()) {
                    if (ejbComponentDescription.isExplicitSecurityDomainConfigured()) {
                        if (knownSecurityDomainName == null) {
                            knownSecurityDomainName = ejbComponentDescription.getSecurityDomain();
                        } else if (! knownSecurityDomainName.equals(ejbComponentDescription.getSecurityDomain())) {
                            throw EjbLogger.ROOT_LOGGER.multipleSecurityDomainsDetected();
                        }
                    } else {
                        defaultKnownSecurityDomainName = ejbComponentDescription.getSecurityDomain();
                    }
                }
            }
        }
        if (knownSecurityDomainName == null) {
            knownSecurityDomainName = defaultKnownSecurityDomainName;
        }

        // If this EJB deployment is associated with an Elytron security domain, set up the security domain mapping
        if (knownSecurityDomainName != null && ! knownSecurityDomainName.isEmpty()) {
            final EJBSecurityDomainService ejbSecurityDomainService = new EJBSecurityDomainService(deploymentUnit);
            final CapabilityServiceSupport support = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
            ServiceName serviceName = deploymentUnit.getServiceName().append(EJBSecurityDomainService.SERVICE_NAME);
            final ServiceBuilder<Void> builder = phaseContext.getServiceTarget().addService(serviceName, ejbSecurityDomainService)
                    .addDependency(support.getCapabilityServiceName(ApplicationSecurityDomainDefinition.APPLICATION_SECURITY_DOMAIN_CAPABILITY, knownSecurityDomainName),
                            ApplicationSecurityDomain.class, ejbSecurityDomainService.getApplicationSecurityDomainInjector());
            builder.install();

            for(final ComponentDescription componentDescription : componentDescriptions) {
                if (componentDescription instanceof EJBComponentDescription) {
                    componentDescription.getConfigurators().add((context, description, configuration) ->
                                    configuration.getCreateDependencies().add((serviceBuilder, service) -> serviceBuilder.addDependency(serviceName))
                    );
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    /**
     * Sets the default security domain name to be used for EJB components, if no explicit security domain
     * is configured for the bean.
     *
     * @param securityDomainName The security domain name. Can be null.
     */
    public void setDefaultSecurityDomainName(final String securityDomainName) {
        this.defaultSecurityDomainName = securityDomainName;
    }
}
