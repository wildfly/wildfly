/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.filters;

import io.undertow.Handlers;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class ExpressionFilterDefinition extends Filter {

    public static final AttributeDefinition EXPRESSION = new SimpleAttributeDefinitionBuilder("expression", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final AttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder("module", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final ExpressionFilterDefinition INSTANCE = new ExpressionFilterDefinition();

    private ExpressionFilterDefinition() {
        super("expression-filter");
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(EXPRESSION, MODULE);
    }

    @Override
    public HttpHandler createHttpHandler(Predicate predicate, ModelNode model, HttpHandler next) {
        String expression = model.get(EXPRESSION.getName()).asString();
        String moduleName = null;
        if (model.hasDefined(MODULE.getName())) {
            moduleName = model.get(MODULE.getName()).asString();
        }
        ClassLoader classLoader;
        if (moduleName == null) {
            classLoader = getClass().getClassLoader();
        } else {
            try {
                ModuleLoader moduleLoader = Module.getBootModuleLoader();
                Module filterModule = moduleLoader.loadModule(ModuleIdentifier.fromString(moduleName));
                classLoader = filterModule.getClassLoader();
            } catch (ModuleLoadException e) {
                throw UndertowLogger.ROOT_LOGGER.couldNotLoadHandlerFromModule(expression, moduleName, e);
            }
        }
        List<PredicatedHandler> handlers = PredicatedHandlersParser.parse(expression, classLoader);
        UndertowLogger.ROOT_LOGGER.debugf("Creating http handler %s from module %s", expression, moduleName);

        if (predicate != null) {
            return Handlers.predicate(predicate, Handlers.predicates(handlers, next), next);
        } else {
            return Handlers.predicates(handlers, next);
        }
    }

    @Override
    protected Class[] getConstructorSignature() {
        throw new IllegalStateException(); //should not be used, as the handler is constructed above
    }
}
