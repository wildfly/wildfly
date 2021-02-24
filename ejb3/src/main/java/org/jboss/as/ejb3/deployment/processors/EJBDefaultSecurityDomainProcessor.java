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

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.server.security.SecurityMetaData.ATTACHMENT_KEY;

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
import org.jboss.as.server.security.SecurityMetaData;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.security.auth.server.SecurityDomain;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

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

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
        final SecurityMetaData securityMetaData = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        // If we have a ServiceName for a security domain it should be used for all components.
        ServiceName elytronDomainServiceName = securityMetaData != null ? securityMetaData.getSecurityDomain() : null;

        final ServiceName ejbSecurityDomainServiceName = deploymentUnit.getServiceName().append(EJBSecurityDomainService.SERVICE_NAME);

        final ApplicationSecurityDomainConfig defaultDomainMapping = knownSecurityDomain.apply(defaultSecurityDomain);
        final ServiceName defaultElytronDomainServiceName;
        if (defaultDomainMapping != null) {
            defaultElytronDomainServiceName = support
                    .getCapabilityServiceName(ApplicationSecurityDomainDefinition.APPLICATION_SECURITY_DOMAIN_CAPABILITY_NAME, defaultSecurityDomain)
                    .append("security-domain");
        } else {
            defaultElytronDomainServiceName = null;
        }

        ApplicationSecurityDomainConfig selectedElytronDomainConfig = null;
        if (elytronDomainServiceName == null) {
            String selectedElytronDomainName = null;
            boolean legacyDomainDefined  = false;

            boolean defaultRequired = false;
            for (ComponentDescription componentDescription : componentDescriptions) {
                if (componentDescription instanceof EJBComponentDescription) {
                    EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
                    ejbComponentDescription.setDefaultSecurityDomain(defaultSecurityDomain);

                    // Ensure the EJB components within a deployment are associated with at most one Elytron security domain

                    String definedSecurityDomain = ejbComponentDescription.getDefinedSecurityDomain();
                    defaultRequired = defaultRequired || definedSecurityDomain == null;
                    ApplicationSecurityDomainConfig definedDomainMapping = definedSecurityDomain != null ? knownSecurityDomain.apply(definedSecurityDomain) : null;

                    if (definedDomainMapping != null) {
                        if (selectedElytronDomainName == null) {
                            selectedElytronDomainName = definedSecurityDomain;
                            selectedElytronDomainConfig = definedDomainMapping;
                        } else if (selectedElytronDomainName.equals(definedSecurityDomain) == false) {
                            throw EjbLogger.ROOT_LOGGER.multipleSecurityDomainsDetected();
                        }
                    } else if (definedSecurityDomain != null) {
                        legacyDomainDefined = true;
                    }
                }
            }

            final boolean useDefaultElytronMapping;
            /*
             * We only need to fall into the default handling if at least one EJB Component has no defined
             * security domain.
             */
            if (defaultRequired && selectedElytronDomainName == null && defaultDomainMapping != null) {
                selectedElytronDomainName = defaultSecurityDomain;
                selectedElytronDomainConfig = defaultDomainMapping;
                elytronDomainServiceName = defaultElytronDomainServiceName;
                // Only apply a default domain to the whole deployment if no legacy domain was defined.
                useDefaultElytronMapping = !legacyDomainDefined;
            } else {
                useDefaultElytronMapping = false;
            }

            // If this EJB deployment is associated with an Elytron security domain, set up the security domain mapping
            if (selectedElytronDomainConfig != null) {
                final EJBSecurityDomainService ejbSecurityDomainService = new EJBSecurityDomainService(deploymentUnit);

                ServiceName applicationSecurityDomainServiceName = support.getCapabilityServiceName(
                        ApplicationSecurityDomainDefinition.APPLICATION_SECURITY_DOMAIN_CAPABILITY_NAME, selectedElytronDomainName);
                elytronDomainServiceName = applicationSecurityDomainServiceName.append("security-domain");

                final ServiceBuilder<Void> builder = phaseContext.getServiceTarget().addService(ejbSecurityDomainServiceName, ejbSecurityDomainService)
                        .addDependency(applicationSecurityDomainServiceName, ApplicationSecurityDomain.class, ejbSecurityDomainService.getApplicationSecurityDomainInjector());
                builder.install();

                for(final ComponentDescription componentDescription : componentDescriptions) {
                    if (componentDescription instanceof EJBComponentDescription) {
                        EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
                        String definedSecurityDomain = ejbComponentDescription.getDefinedSecurityDomain();

                        /*
                         * Only apply the Elytron domain if one of the following is true:
                         *  - No security domain was defined within the deployment so the default is being applied to all components.
                         *  - The security domain defined on the EJB Component matches the name mapped to the Elytron domain.
                         *
                         * Otherwise EJBComponents will be in one of the following states:
                         *  - No associated security domain.
                         *  - Using configured domain as PicketBox domain.
                         *  - Fallback to subsystem defined default PicketBox domain.
                         */

                        // The component may have had a legacy SecurityDomain defined.
                        if (useDefaultElytronMapping
                                || selectedElytronDomainName.equals(definedSecurityDomain)) {
                            ejbComponentDescription.setOutflowSecurityDomainsConfigured(outflowSecurityDomainsConfigured);
                            ejbComponentDescription.setSecurityDomainServiceName(elytronDomainServiceName);
                            ejbComponentDescription.setRequiresJacc(selectedElytronDomainConfig.isEnableJacc());
                            ejbComponentDescription.setLegacyCompliantPrincipalPropagation(selectedElytronDomainConfig.isLegacyCompliantPrincipalPropagation());
                            ejbComponentDescription.getConfigurators().add((context, description, configuration) ->
                                            configuration.getCreateDependencies().add((serviceBuilder, service) -> serviceBuilder.requires(ejbSecurityDomainServiceName))
                            );
                        } else if (definedSecurityDomain == null && defaultDomainMapping != null) {
                            ejbComponentDescription.setOutflowSecurityDomainsConfigured(outflowSecurityDomainsConfigured);
                            ejbComponentDescription.setSecurityDomainServiceName(defaultElytronDomainServiceName);
                            ejbComponentDescription.setRequiresJacc(defaultDomainMapping.isEnableJacc());
                            ejbComponentDescription.setLegacyCompliantPrincipalPropagation(defaultDomainMapping.isLegacyCompliantPrincipalPropagation());
                            ejbComponentDescription.getConfigurators().add((context, description, configuration) ->
                                            configuration.getCreateDependencies().add((serviceBuilder, service) -> serviceBuilder.requires(ejbSecurityDomainServiceName))
                            );
                        }
                    }
                }
            }
        } else {
            // We will use the defined Elytron domain for all EJBs and ignore individual configuration.
            // Bean level activation remains dependent on configuration of bean - i.e. does it actually need security?
            final EJBSecurityDomainService ejbSecurityDomainService = new EJBSecurityDomainService(deploymentUnit);

            final ServiceBuilder<Void> builder = phaseContext.getServiceTarget().addService(ejbSecurityDomainServiceName, ejbSecurityDomainService)
                    .addDependency(elytronDomainServiceName, SecurityDomain.class, ejbSecurityDomainService.getSecurityDomainInjector());

            builder.install();

            for (ComponentDescription componentDescription : componentDescriptions) {
                if (componentDescription instanceof EJBComponentDescription) {
                    EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
                    ejbComponentDescription.setSecurityDomainServiceName(elytronDomainServiceName);
                    ejbComponentDescription.setOutflowSecurityDomainsConfigured(outflowSecurityDomainsConfigured);
                    componentDescription.getConfigurators()
                            .add((context, description, configuration) -> configuration.getCreateDependencies()
                                    .add((serviceBuilder, service) -> serviceBuilder.requires(ejbSecurityDomainServiceName)));
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
