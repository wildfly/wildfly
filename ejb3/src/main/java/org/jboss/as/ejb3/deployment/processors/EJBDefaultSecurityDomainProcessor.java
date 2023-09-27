/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.as.server.security.VirtualDomainMarkerUtility;
import org.jboss.as.server.security.VirtualDomainMetaData;
import org.jboss.as.server.security.VirtualDomainUtil;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.security.auth.server.SecurityDomain;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * A {@link DeploymentUnitProcessor} which looks for {@link EJBComponentDescription}s in the deployment
 * unit and sets the default security domain name, that's configured at the Jakarta Enterprise Beans subsystem level,
 * {@link EJBComponentDescription#setDefaultSecurityDomain(String) to each of the Jakarta Enterprise Beans component descriptions}.
 *
 * @author Jaikiran Pai
 */
public class EJBDefaultSecurityDomainProcessor implements DeploymentUnitProcessor, Function<String, ApplicationSecurityDomainConfig>, BooleanSupplier {

    private final AtomicReference<String> defaultSecurityDomainName;
    private final Iterable<ApplicationSecurityDomainConfig> knownApplicationSecurityDomains;
    private final Iterable<String> outflowSecurityDomains;

    public EJBDefaultSecurityDomainProcessor(AtomicReference<String> defaultSecurityDomainName, Iterable<ApplicationSecurityDomainConfig> knownApplicationSecurityDomains, Iterable<String> outflowSecurityDomains) {
        this.defaultSecurityDomainName = defaultSecurityDomainName;
        this.knownApplicationSecurityDomains = knownApplicationSecurityDomains;
        this.outflowSecurityDomains = outflowSecurityDomains;
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
            defaultSecurityDomain = this.defaultSecurityDomainName.get();
        } else {
            defaultSecurityDomain = eeModuleDescription.getDefaultSecurityDomain();
        }

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
        final SecurityMetaData securityMetaData = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        // If we have a ServiceName for a security domain it should be used for all components.
        ServiceName elytronDomainServiceName = securityMetaData != null ? securityMetaData.getSecurityDomain() : null;

        final ServiceName ejbSecurityDomainServiceName = deploymentUnit.getServiceName().append(EJBSecurityDomainService.SERVICE_NAME);

        final ApplicationSecurityDomainConfig defaultDomainMapping = this.apply(defaultSecurityDomain);
        final ServiceName defaultElytronDomainServiceName;
        if (defaultDomainMapping != null) {
            defaultElytronDomainServiceName = support
                    .getCapabilityServiceName(ApplicationSecurityDomainDefinition.APPLICATION_SECURITY_DOMAIN_CAPABILITY_NAME, defaultSecurityDomain)
                    .append("security-domain");
        } else {
            defaultElytronDomainServiceName = null;
        }

        ApplicationSecurityDomainConfig selectedElytronDomainConfig = null;
        VirtualDomainMetaData virtualDomainMetaData = null;
        boolean isDefinedSecurityDomainVirtual = false;

        if (elytronDomainServiceName == null) {
            String selectedElytronDomainName = null;
            boolean legacyDomainDefined  = false;

            boolean defaultRequired = false;
            for (ComponentDescription componentDescription : componentDescriptions) {
                if (componentDescription instanceof EJBComponentDescription) {
                    EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
                    ejbComponentDescription.setDefaultSecurityDomain(defaultSecurityDomain);

                    // Ensure the Jakarta Enterprise Beans components within a deployment are associated with at most one Elytron security domain

                    String definedSecurityDomain = ejbComponentDescription.getDefinedSecurityDomain();
                    defaultRequired = defaultRequired || definedSecurityDomain == null;
                    ApplicationSecurityDomainConfig definedDomainMapping = definedSecurityDomain != null ? this.apply(definedSecurityDomain) : null;

                    if (definedDomainMapping != null) {
                        if (selectedElytronDomainName == null) {
                            selectedElytronDomainName = definedSecurityDomain;
                            selectedElytronDomainConfig = definedDomainMapping;
                        } else if (selectedElytronDomainName.equals(definedSecurityDomain) == false) {
                            throw EjbLogger.ROOT_LOGGER.multipleSecurityDomainsDetected();
                        }
                    } else if (definedSecurityDomain != null) {
                        virtualDomainMetaData = getVirtualDomainMetaData(definedSecurityDomain, phaseContext);
                        if (virtualDomainMetaData != null) {
                            elytronDomainServiceName = VirtualDomainMarkerUtility.virtualDomainName(definedSecurityDomain);
                            isDefinedSecurityDomainVirtual = true;
                        }
                        if (elytronDomainServiceName != null) {
                            selectedElytronDomainName = definedSecurityDomain;
                        } else {
                            legacyDomainDefined = true;
                        }
                    }
                }
            }

            final boolean useDefaultElytronMapping;
            /*
             * We only need to fall into the default handling if at least one Jakarta Enterprise Beans Component has no defined
             * security domain.
             */
            if (defaultRequired && selectedElytronDomainName == null) {
                DeploymentUnit topLevelDeployment = toRoot(deploymentUnit);
                final SecurityMetaData topLevelSecurityMetaData = topLevelDeployment.getAttachment(ATTACHMENT_KEY);
                ServiceName topLevelElytronDomainServiceName = topLevelSecurityMetaData != null ? topLevelSecurityMetaData.getSecurityDomain() : null;
                if (topLevelElytronDomainServiceName != null) {
                    // use the ServiceName from the top level deployment if the security domain has not been explicitly defined
                    elytronDomainServiceName = topLevelElytronDomainServiceName;
                    useDefaultElytronMapping = true;
                } else if (defaultDomainMapping != null) {
                    selectedElytronDomainName = defaultSecurityDomain;
                    selectedElytronDomainConfig = defaultDomainMapping;
                    elytronDomainServiceName = defaultElytronDomainServiceName;
                    // Only apply a default domain to the whole deployment if no legacy domain was defined.
                    useDefaultElytronMapping = !legacyDomainDefined;
                } else {
                    useDefaultElytronMapping = false;
                }
            } else {
                useDefaultElytronMapping = false;
            }

            // If this Jakarta Enterprise Beans deployment is associated with an Elytron security domain, set up the security domain mapping
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
                         *  - The security domain defined on the Jakarta Enterprise Beans Component matches the name mapped to the Elytron domain.
                         *
                         * Otherwise Jakarta Enterprise Beans Components will be in one of the following states:
                         *  - No associated security domain.
                         *  - Using configured domain as PicketBox domain.
                         *  - Fallback to subsystem defined default PicketBox domain.
                         */

                        // The component may have had a legacy SecurityDomain defined.
                        if (useDefaultElytronMapping
                                || selectedElytronDomainName.equals(definedSecurityDomain)) {
                            ejbComponentDescription.setOutflowSecurityDomainsConfigured(this);
                            ejbComponentDescription.setSecurityDomainServiceName(elytronDomainServiceName);
                            ejbComponentDescription.setRequiresJacc(selectedElytronDomainConfig.isEnableJacc());
                            ejbComponentDescription.setLegacyCompliantPrincipalPropagation(selectedElytronDomainConfig.isLegacyCompliantPrincipalPropagation());
                            ejbComponentDescription.getConfigurators().add((context, description, configuration) ->
                                            configuration.getCreateDependencies().add((serviceBuilder, service) -> serviceBuilder.requires(ejbSecurityDomainServiceName))
                            );
                        } else if (definedSecurityDomain == null && defaultDomainMapping != null) {
                            ejbComponentDescription.setOutflowSecurityDomainsConfigured(this);
                            ejbComponentDescription.setSecurityDomainServiceName(defaultElytronDomainServiceName);
                            ejbComponentDescription.setRequiresJacc(defaultDomainMapping.isEnableJacc());
                            ejbComponentDescription.setLegacyCompliantPrincipalPropagation(defaultDomainMapping.isLegacyCompliantPrincipalPropagation());
                            ejbComponentDescription.getConfigurators().add((context, description, configuration) ->
                                            configuration.getCreateDependencies().add((serviceBuilder, service) -> serviceBuilder.requires(ejbSecurityDomainServiceName))
                            );
                        }
                    }
                }
            } else if (elytronDomainServiceName != null) {
                // virtual domain
                final EJBSecurityDomainService ejbSecurityDomainService = new EJBSecurityDomainService(deploymentUnit);

                if (isDefinedSecurityDomainVirtual && ! VirtualDomainUtil.isVirtualDomainCreated(deploymentUnit)) {
                    VirtualDomainUtil.createVirtualDomain(phaseContext.getServiceRegistry(), virtualDomainMetaData, elytronDomainServiceName, phaseContext.getServiceTarget());
                }
                final ServiceBuilder<Void> builder = phaseContext.getServiceTarget().addService(ejbSecurityDomainServiceName, ejbSecurityDomainService)
                        .addDependency(elytronDomainServiceName, SecurityDomain.class, ejbSecurityDomainService.getSecurityDomainInjector());
                builder.install();

                for (final ComponentDescription componentDescription : componentDescriptions) {
                    if (componentDescription instanceof EJBComponentDescription) {
                        EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
                        ejbComponentDescription.setSecurityDomainServiceName(elytronDomainServiceName);
                        ejbComponentDescription.setOutflowSecurityDomainsConfigured(this);
                        componentDescription.getConfigurators()
                                .add((context, description, configuration) -> configuration.getCreateDependencies()
                                        .add((serviceBuilder, service) -> serviceBuilder.requires(ejbSecurityDomainServiceName)));
                    }
                }
            }

        } else {
            // We will use the defined Elytron domain for all Jakarta Enterprise Beans and ignore individual configuration.
            // Bean level activation remains dependent on configuration of bean - i.e. does it actually need security?
            final EJBSecurityDomainService ejbSecurityDomainService = new EJBSecurityDomainService(deploymentUnit);

            final ServiceBuilder<Void> builder = phaseContext.getServiceTarget().addService(ejbSecurityDomainServiceName, ejbSecurityDomainService)
                    .addDependency(elytronDomainServiceName, SecurityDomain.class, ejbSecurityDomainService.getSecurityDomainInjector());

            builder.install();

            for (ComponentDescription componentDescription : componentDescriptions) {
                if (componentDescription instanceof EJBComponentDescription) {
                    EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
                    ejbComponentDescription.setSecurityDomainServiceName(elytronDomainServiceName);
                    ejbComponentDescription.setOutflowSecurityDomainsConfigured(this);
                    componentDescription.getConfigurators()
                            .add((context, description, configuration) -> configuration.getCreateDependencies()
                                    .add((serviceBuilder, service) -> serviceBuilder.requires(ejbSecurityDomainServiceName)));
                }
            }
        }
    }

    @Override
    public ApplicationSecurityDomainConfig apply(String name) {
        for (ApplicationSecurityDomainConfig applicationSecurityDomainConfig : this.knownApplicationSecurityDomains) {
            if (applicationSecurityDomainConfig.isSameDomain(name)) {
                return applicationSecurityDomainConfig;
            }
        }
        return null;
    }

    @Override
    public boolean getAsBoolean() {
        return this.outflowSecurityDomains.iterator().hasNext();
    }

    private <T> ServiceController<T> getService(ServiceRegistry serviceRegistry, ServiceName serviceName, Class<T> serviceType) {
        ServiceController<?> controller = serviceRegistry.getService(serviceName);
        return (ServiceController<T>) controller;
    }

    private VirtualDomainMetaData getVirtualDomainMetaData(String definedSecurityDomain, DeploymentPhaseContext phaseContext) {
        if (definedSecurityDomain != null && ! definedSecurityDomain.isEmpty()) {
            ServiceName virtualDomainMetaDataName = VirtualDomainMarkerUtility.virtualDomainMetaDataName(phaseContext, definedSecurityDomain);
            ServiceController<VirtualDomainMetaData> serviceContainer = getService(phaseContext.getServiceRegistry(), virtualDomainMetaDataName, VirtualDomainMetaData.class);
            if (serviceContainer != null) {
                ServiceController.State serviceState = serviceContainer.getState();
                if (serviceState == ServiceController.State.UP) {
                    return serviceContainer.getValue();
                }
            }
        }
        return null;
    }

    private static DeploymentUnit toRoot(final DeploymentUnit deploymentUnit) {
        DeploymentUnit result = deploymentUnit;
        DeploymentUnit parent = result.getParent();
        while (parent != null) {
            result = parent;
            parent = result.getParent();
        }

        return result;
    }
}
