/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.ApplicationSecurityDomain;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * A {@link ResourceDefinition} to define the mapping from a security domain, as specified in a web application,
 * to an Elytron {@link SecurityDomain}.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class ApplicationSecurityDomainDefinition extends SimpleResourceDefinition {

    public static final String APPLICATION_SECURITY_DOMAIN_CAPABILITY = "org.wildfly.ejb3.application-security-domain";

    static final RuntimeCapability<Void> APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(APPLICATION_SECURITY_DOMAIN_CAPABILITY, true, ApplicationSecurityDomain.class)
            .build();

    private static final String SECURITY_DOMAIN_CAPABILITY = "org.wildfly.security.security-domain";

    static final SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.SECURITY_DOMAIN, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setCapabilityReference(SECURITY_DOMAIN_CAPABILITY, APPLICATION_SECURITY_DOMAIN_CAPABILITY, true)
            .build();

    private static StringListAttributeDefinition REFERENCING_DEPLOYMENTS = new StringListAttributeDefinition.Builder(EJB3SubsystemModel.REFERENCING_DEPLOYMENTS)
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { SECURITY_DOMAIN };

    static final ApplicationSecurityDomainDefinition INSTANCE = new ApplicationSecurityDomainDefinition();

    private static final Set<String> knownApplicationSecurityDomains = Collections.synchronizedSet(new HashSet<>());

    private ApplicationSecurityDomainDefinition() {
        this(new Parameters(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN), EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN))
                .setCapabilities(APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY), new AddHandler());
    }

    private ApplicationSecurityDomainDefinition(Parameters parameters, AbstractAddStepHandler add) {
        super(parameters.setAddHandler(add).setRemoveHandler(new RemoveHandler(add)));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        knownApplicationSecurityDomains.clear();
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute,  null, handler);
        }
        resourceRegistration.registerReadOnlyAttribute(REFERENCING_DEPLOYMENTS, new ReferencingDeploymentsHandler());
    }

    private static class AddHandler extends AbstractAddStepHandler {

        private AddHandler() {
            super(ATTRIBUTES);
        }

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.populateModel(context, operation, resource);
            knownApplicationSecurityDomains.add(context.getCurrentAddressValue());
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String securityDomain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asString();

            RuntimeCapability<?> runtimeCapability = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(context.getCurrentAddressValue());
            ServiceName serviceName = runtimeCapability.getCapabilityServiceName(ApplicationSecurityDomain.class);
            ApplicationSecurityDomainService applicationSecurityDomainService = new ApplicationSecurityDomainService();

            ServiceBuilder<ApplicationSecurityDomain> serviceBuilder = context.getServiceTarget().addService(serviceName, applicationSecurityDomainService)
                    .setInitialMode(Mode.LAZY);
            serviceBuilder.addDependency(context.getCapabilityServiceName(
                            SECURITY_DOMAIN_CAPABILITY, securityDomain, SecurityDomain.class),
                    SecurityDomain.class, applicationSecurityDomainService.getSecurityDomainInjector());
            serviceBuilder.install();
        }
    }

    private static class RemoveHandler extends ServiceRemoveStepHandler {

        protected RemoveHandler(AbstractAddStepHandler addOperation) {
            super(addOperation);
        }

        @Override
        protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRemove(context, operation, model);
            knownApplicationSecurityDomains.remove(context.getCurrentAddressValue());
        }

        @Override
        protected ServiceName serviceName(String name) {
            RuntimeCapability<?> dynamicCapability = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(name);
            return dynamicCapability.getCapabilityServiceName(ApplicationSecurityDomain.class);
        }
    }

    private static class ReferencingDeploymentsHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            RuntimeCapability<Void> runtimeCapability = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(context.getCurrentAddressValue());
            ServiceName serviceName = runtimeCapability.getCapabilityServiceName(ApplicationSecurityDomain.class);
            ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
            ServiceController<?> controller = serviceRegistry.getRequiredService(serviceName);

            ModelNode deploymentList = new ModelNode();
            if (controller.getState() == State.UP) {
                Service service = controller.getService();
                if (service instanceof ApplicationSecurityDomainService) {
                    for (String current : ((ApplicationSecurityDomainService) service).getDeployments()) {
                        deploymentList.add(current);
                    }
                }
            }
            context.getResult().set(deploymentList);
        }
    }

    Predicate<String> getKnownSecurityDomainPredicate() {
        return knownApplicationSecurityDomains::contains;
    }

    static void registerTransformers_1_2_0_and_1_3_0(ResourceTransformationDescriptionBuilder parent) {
        parent.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN));
    }

    static void registerTransformers_3_0_0(ResourceTransformationDescriptionBuilder parent) {
        parent.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN));
    }

    static void registerTransformers_4_0(ResourceTransformationDescriptionBuilder parent) {
        parent.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN));
    }
}
