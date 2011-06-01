/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.impl;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.handlers.CommandHandlerWithArguments;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class RequestParamArg extends ArgumentWithValue {

    protected final String paramName;

    public RequestParamArg(String paramName, CommandHandlerWithArguments handler, CommandLineCompleter valueCompleter) {
        super(handler, valueCompleter, "--" + paramName);
        if(paramName == null) {
            throw new IllegalArgumentException("Parameter name is null.");
        }
        this.paramName = paramName;
    }

    public RequestParamArg(String paramName, CommandHandlerWithArguments handler) {
        this(paramName, handler, "--" + paramName);
    }

    public RequestParamArg(String paramName, CommandHandlerWithArguments handler, String fullArgName) {
        super(handler, fullArgName);
        if(paramName == null) {
            throw new IllegalArgumentException("Parameter name is null.");
        }
        this.paramName = paramName;
    }

    public void set(ParsedArguments args, ModelNode request) throws CommandFormatException {
        final String value = getValue(args);
        if(value != null) {
            setValue(request, paramName, value);
        }
    }

    protected static void setValue(ModelNode request, String name, String value) {
        if(name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The argument name is not specified: '" + name + "'");
        if(value == null || value.trim().isEmpty())
            throw new IllegalArgumentException("The argument value is not specified: '" + value + "'");
        ModelNode toSet = null;
        try {
            toSet = ModelNode.fromString(value);
        } catch (Exception e) {
            // just use the string
            toSet = new ModelNode().set(value);
        }
        request.get(name).set(toSet);
    }
}
