/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.messaging.jms.cli;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.handlers.BatchModeCommandHandler;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
class CreateJMSResourceHandler extends BatchModeCommandHandler {

    public CreateJMSResourceHandler(CommandContext ctx) {
        super(ctx, "create-jms-resource", true);
        this.addRequiredPath("/subsystem=messaging");
    }

    @Override
    public ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws OperationFormatException {

        try {
            if(!ctx.getParsedCommandLine().hasProperties()) {
                throw new OperationFormatException("Arguments are missing");
            }
        } catch (CommandFormatException e) {
            throw new OperationFormatException(e.getLocalizedMessage());
        }

        //String target = null;
        String restype = null;
        //String description = null;
        String propsStr = null;
        //boolean enabled = false;
        String jndiName = null;

        String[] args = ctx.getArgumentsString().split("\\s+");
        int i = 0;
        while(i < args.length) {
            String arg = args[i++];
            if(arg.equals("--restype")) {
                if(i < args.length) {
                    restype = args[i++];
                }
            } else if(arg.equals("--target")) {
//                if(i < args.length) {
//                    target = args[i++];
//                }
            } else if(arg.equals("--description")) {
//                if(i < args.length) {
//                    restype = args[i++];
//                }
            } else if(arg.equals("--property")) {
                if (i < args.length) {
                    propsStr = args[i++];
                }
            } else if(arg.equals("--enabled")) {
//                if (i < args.length) {
//                    enabled = Boolean.parseBoolean(args[i++]);
//                }
            } else {
                jndiName = arg;
            }
        }

        if(restype == null) {
            throw new OperationFormatException("Required parameter --restype is missing.");
        }

        if(jndiName == null) {
            throw new OperationFormatException("JNDI name is missing.");
        }

        String name = null;
        String serverName = "default"; // TODO read server name from props
        final Map<String, String> props;
        if(propsStr != null) {
            props = new HashMap<String, String>();
            String[] propsArr = propsStr.split(":");
            for(String prop : propsArr) {
                int equalsIndex = prop.indexOf('=');
                if(equalsIndex < 0 || equalsIndex == prop.length() - 1) {
                    throw new OperationFormatException("Failed to parse property '" + prop + "'");
                }

                String propName = prop.substring(0, equalsIndex).trim();
                String propValue = prop.substring(equalsIndex + 1).trim();
                if(propName.isEmpty()) {
                    throw new OperationFormatException("Failed to parse property '" + prop + "'");
                }

                if(propName.equals("imqDestinationName") ||propName.equalsIgnoreCase("name")) {
                    name = propValue;
                } else if("ClientId".equals(propName)) {
                    props.put("client-id", propValue);
                }
            }
        } else {
            props = Collections.emptyMap();
        }

        if(name == null) {
            name = jndiName.replace('/', '_');
        }

        if(restype.equals("javax.jms.Queue")) {

            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            builder.addNode("subsystem", "messaging");
            builder.addNode("hornetq-server", serverName);
            builder.addNode("jms-queue", name);
            builder.setOperationName("add");
            builder.getModelNode().get("entries").add(jndiName);

            for(String prop : props.keySet()) {
                builder.addProperty(prop, props.get(prop));
            }

            return builder.buildRequest();

        } else if(restype.equals("javax.jms.Topic")) {

            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            builder.addNode("subsystem", "messaging");
            builder.addNode("hornetq-server", serverName);
            builder.addNode("jms-topic", name);
            builder.setOperationName("add");
            builder.getModelNode().get("entries").add(jndiName);

            for(String prop : props.keySet()) {
                builder.addProperty(prop, props.get(prop));
            }

            return builder.buildRequest();

        } else if(restype.equals("javax.jms.ConnectionFactory") ||
                restype.equals("javax.jms.TopicConnectionFactory") ||
                restype.equals("javax.jms.QueueConnectionFactory")) {

            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            builder.addNode("subsystem", "messaging");
            builder.addNode("hornetq-server", serverName);
            builder.addNode("connection-factory", name);
            builder.setOperationName("add");
            builder.getModelNode().get("entries").add(jndiName);

            for(String prop : props.keySet()) {
                builder.addProperty(prop, props.get(prop));
            }

            return builder.buildRequest();

        } else {
            throw new OperationFormatException("Resource type " + restype + " isn't supported.");
        }
    }
}
