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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import io.undertow.Handlers;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.undertow.AbstractHandlerDefinition;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
abstract class Filter extends AbstractHandlerDefinition {
    private String name;

    protected Filter(String name) {
        super(name, Constants.FILTER);
        this.name = name;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        FilterAdd add = new FilterAdd(this);
        registerAddOperation(resourceRegistration, add, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        //registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(UndertowService.FILTER, add), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(UndertowService.FILTER, add), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);

    }

    public HttpHandler createHttpHandler(final Predicate predicate, final ModelNode model, HttpHandler next) {
        List<AttributeDefinition> attributes = new ArrayList<>(getAttributes());
        HttpHandler handler = createHandler(getHandlerClass(), model, attributes, next);
        if (predicate != null) {
            return Handlers.predicate(predicate, handler, next);
        } else {
            return handler;
        }
    }

    protected HttpHandler createHandler(Class<? extends HttpHandler> handlerClass, final ModelNode model, List<AttributeDefinition> attributes, HttpHandler next) {
        int numOfParams = attributes.size();
        if (next != null) {
            numOfParams++;
        }
        try {
            Constructor<?> c = handlerClass.getDeclaredConstructor(getConstructorSignature());
            if (c.getParameterTypes().length == numOfParams) {
                boolean match = true;
                Object[] params = new Object[numOfParams];
                Class[] parameterTypes = c.getParameterTypes();
                int attrCounter = 0;
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class param = parameterTypes[i];
                    if (param == String.class) {
                        params[i] = model.get(attributes.get(attrCounter).getName()).asString();
                        attrCounter++;
                    } else if (param == Integer.class || param == int.class) {
                        params[i] = model.get(attributes.get(attrCounter).getName()).asInt();
                        attrCounter++;
                    } else if (param == Long.class || param == long.class) {
                        params[i] = model.get(attributes.get(attrCounter).getName()).asLong();
                        attrCounter++;
                    } else if (param == HttpHandler.class) {
                        params[i] = next;
                    } else {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return (HttpHandler) c.newInstance(params);
                }
            }
        } catch (Throwable e) {
            throw UndertowLogger.ROOT_LOGGER.cannotCreateHttpHandler(handlerClass, model, e);
        }
        throw UndertowLogger.ROOT_LOGGER.cannotCreateHttpHandler(handlerClass, model, null);
    }

    protected abstract Class[] getConstructorSignature();
}
