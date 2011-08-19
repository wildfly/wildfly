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
import org.jboss.as.cli.handlers.CommandHandlerWithArguments;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class RequestParamPropertiesArg extends RequestParamArgWithValue {


    public RequestParamPropertiesArg(String paramName,
            CommandHandlerWithArguments handler,
            CommandLineCompleter valueCompleter) {
        super(paramName, handler, valueCompleter);
    }

    public RequestParamPropertiesArg(String paramName,
            CommandHandlerWithArguments handler, String fullArgName) {
        super(paramName, handler, fullArgName);
    }

    public RequestParamPropertiesArg(String paramName,
            CommandHandlerWithArguments handler) {
        super(paramName, handler);
    }

    public void set(ParsedCommandLine args, ModelNode request) throws CommandFormatException {
        final String value = getValue(args);
        if(value != null) {
            setPropertyMapValue(request, paramName, value);
        }
    }

    protected static void setPropertyMapValue(ModelNode request, String name, String value) throws CommandFormatException {
        if(name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The argument name is not specified: '" + name + "'");
        if(value == null || value.trim().isEmpty())
            throw new IllegalArgumentException("The argument value is not specified: '" + value + "'");

        ModelNode nodeValue = new ModelNode();
        String[] props = value.split(",");
        for(String prop : props) {
            int equals = prop.indexOf('=');
            if(equals == -1) {
                throw new CommandFormatException("Property '" + prop + "' in '" + value + "' is missing the equals sign.");
            }
            String propName = prop.substring(0, equals);
            if(propName.isEmpty()) {
                throw new CommandFormatException("Property name is missing for '" + prop + "' in '" + value + "'");
            }
            nodeValue.add(propName, prop.substring(equals + 1));
        }

        request.get(name).set(nodeValue);
    }
}
