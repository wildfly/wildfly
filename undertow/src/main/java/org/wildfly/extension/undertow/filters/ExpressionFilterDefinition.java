/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.Collection;
import java.util.List;

import io.undertow.Handlers;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class ExpressionFilterDefinition extends SimpleFilterDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement("expression-filter");

    public static final AttributeDefinition EXPRESSION = new SimpleAttributeDefinitionBuilder("expression", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final AttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder("module", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setCorrector(ModuleIdentifierUtil.MODULE_NAME_CORRECTOR)
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(EXPRESSION, MODULE);

    ExpressionFilterDefinition() {
        super(PATH_ELEMENT, ExpressionFilterDefinition::createHandlerWrapper);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static PredicateHandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) throws OperationFailedException {
        String expression = EXPRESSION.resolveModelAttribute(context, model).asString();
        String moduleName = MODULE.resolveModelAttribute(context, model).asStringOrNull();
        ClassLoader loader = ExpressionFilterDefinition.class.getClassLoader();
        if (moduleName != null) {
            try {
                ModuleLoader moduleLoader = Module.getBootModuleLoader();
                Module filterModule = moduleLoader.loadModule(moduleName);
                loader = filterModule.getClassLoader();
            } catch (ModuleLoadException e) {
                throw UndertowLogger.ROOT_LOGGER.couldNotLoadHandlerFromModule(expression, moduleName, e);
            }
        }

        List<PredicatedHandler> handlers = PredicatedHandlersParser.parse(expression, loader);
        UndertowLogger.ROOT_LOGGER.debugf("Creating http handler %s from module %s", expression, moduleName);

        return PredicateHandlerWrapper.filter(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler next) {
                return Handlers.predicates(handlers, next);
            }
        });
    }
}
