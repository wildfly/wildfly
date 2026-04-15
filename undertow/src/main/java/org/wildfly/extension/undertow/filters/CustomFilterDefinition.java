/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshallers;
import org.jboss.as.controller.AttributeParsers;
import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.extension.undertow.deployment.ConfiguredHandlerWrapper;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class CustomFilterDefinition extends SimpleFilterDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement("custom-filter");

    public static final AttributeDefinition CLASS_NAME = new SimpleAttributeDefinitionBuilder("class-name", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final AttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder("module", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setCorrector(ModuleIdentifierUtil.MODULE_NAME_CORRECTOR)
            .build();

    public static final PropertiesAttributeDefinition PARAMETERS = new PropertiesAttributeDefinition.Builder("parameters", true)
            .setRequired(false)
            .setAllowExpression(true)
            .setAttributeParser(new AttributeParsers.PropertiesParser(null, "param", false))
            .setAttributeMarshaller(new AttributeMarshallers.PropertiesAttributeMarshaller(null, "param", false))
            .setRestartAllServices()
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(CLASS_NAME, MODULE, PARAMETERS);

    CustomFilterDefinition() {
        super(PATH_ELEMENT, CustomFilterDefinition::createHandlerWrapper);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static PredicateHandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) throws OperationFailedException {
        String className = CLASS_NAME.resolveModelAttribute(context, model).asString();
        String moduleName = MODULE.resolveModelAttribute(context, model).asString();
        Map<String, String> parameters = PARAMETERS.unwrap(context, model);
        UndertowLogger.ROOT_LOGGER.debugf("Creating http handler %s from module %s with parameters %s", className, moduleName, parameters);
        // Resolve module lazily
        return PredicateHandlerWrapper.filter(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler handler) {
                Class<?> handlerClass = getHandlerClass(className, moduleName);
                return new ConfiguredHandlerWrapper(handlerClass, parameters).wrap(handler);
            }
        });
    }

    private static Class<?> getHandlerClass(String className, String moduleName) {
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        try {
            Module filterModule = moduleLoader.loadModule(moduleName);
            return filterModule.getClassLoader().loadClassLocal(className);
        } catch (ModuleLoadException | ClassNotFoundException e) {
            throw UndertowLogger.ROOT_LOGGER.couldNotLoadHandlerFromModule(className,moduleName,e);
        }
    }
}
