/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.EnumSet;
import java.util.function.BiFunction;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.security.http.util.sso.SingleSignOnConfiguration;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>  2014 Red Hat Inc.
 * @author Paul Ferraro
 */
abstract class SingleSignOnDefinition implements ChildResourceDefinitionRegistrar, ResourceModelResolver<SingleSignOnConfiguration>, ResourceServiceConfigurator {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.SINGLE_SIGN_ON);

    enum Attribute implements AttributeDefinitionProvider {
        DOMAIN(Constants.DOMAIN, ModelType.STRING, null),
        PATH("path", ModelType.STRING, new ModelNode("/")),
        HTTP_ONLY("http-only", ModelType.BOOLEAN, ModelNode.FALSE),
        SECURE("secure", ModelType.BOOLEAN, ModelNode.FALSE),
        COOKIE_NAME("cookie-name", ModelType.STRING, new ModelNode("JSESSIONIDSSO")),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setDefaultValue(defaultValue)
                    .setRestartAllServices()
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    private final BiFunction<ResourceRegistration, ResourceDescriptionResolver, ResourceDefinition.Builder> builderFactory;
    private final BiFunction<ResourceDescriptor.Builder, ResourceServiceConfigurator, ResourceDescriptor.Builder> configurator;

    SingleSignOnDefinition(BiFunction<ResourceRegistration, ResourceDescriptionResolver, ResourceDefinition.Builder> builderFactory, BiFunction<ResourceDescriptor.Builder, ResourceServiceConfigurator, ResourceDescriptor.Builder> configurator) {
        this.builderFactory = builderFactory;
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptor descriptor = this.configurator.apply(ResourceDescriptor.builder(UndertowExtension.getResolver(PATH_ELEMENT.getValue())), this)
                .provideAttributes(EnumSet.allOf(Attribute.class))
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(this.builderFactory.apply(ResourceRegistration.of(PATH_ELEMENT), descriptor.getResourceDescriptionResolver()).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);
        return registration;
    }

    @Override
    public SingleSignOnConfiguration resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String cookieName = SingleSignOnDefinition.Attribute.COOKIE_NAME.resolveModelAttribute(context, model).asString();
        String domain = SingleSignOnDefinition.Attribute.DOMAIN.resolveModelAttribute(context, model).asStringOrNull();
        String path = SingleSignOnDefinition.Attribute.PATH.resolveModelAttribute(context, model).asString();
        boolean httpOnly = SingleSignOnDefinition.Attribute.HTTP_ONLY.resolveModelAttribute(context, model).asBoolean();
        boolean secure = SingleSignOnDefinition.Attribute.SECURE.resolveModelAttribute(context, model).asBoolean();
        return new SingleSignOnConfiguration(cookieName, domain, path, httpOnly, secure);
    }
}
