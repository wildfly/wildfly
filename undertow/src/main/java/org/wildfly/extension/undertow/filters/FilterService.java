/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.Handler;
import org.wildfly.extension.undertow.UndertowMessages;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class FilterService implements Service<FilterService> {
    private final Handler handler;
    private final ModelNode model;

    public FilterService(Handler handler, ModelNode model) {
        this.handler = handler;
        this.model = model;
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {

    }

    public HttpHandler createHttpHandler(HttpHandler next) {
        Class<? extends HttpHandler> handlerClass = handler.getHandlerClass();
        List<AttributeDefinition> attributes = new ArrayList<>(handler.getAttributes());
        int numOfParams = attributes.size() + 1;
        try {
            for (Constructor<?> c : handlerClass.getDeclaredConstructors()) {
                if (c.getParameterTypes().length == numOfParams) {
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
                        }
                    }
                    return (HttpHandler) c.newInstance(params);
                }
            }
        } catch (Throwable e) {
            throw UndertowMessages.MESSAGES.cannotCreateHttpHandler(handlerClass, model, e);
        }
        throw UndertowMessages.MESSAGES.cannotCreateHttpHandler(handlerClass, model, null);
    }

    @Override
    public FilterService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
