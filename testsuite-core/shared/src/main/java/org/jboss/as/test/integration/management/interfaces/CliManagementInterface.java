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

package org.jboss.as.test.integration.management.interfaces;

import java.util.Iterator;

import org.jboss.as.cli.scriptsupport.CLI;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * @author jcechace
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class CliManagementInterface implements ManagementInterface {
    private final CLI client;

    protected CliManagementInterface(CLI client) {
        this.client = client;
    }

    @Override
    public ModelNode execute(ModelNode operation) {
        String command = createCommand(operation);
        CLI.Result result = client.cmd(command);
        return result.getResponse();
    }

    private static String createCommand(ModelNode operation) {
        StringBuilder command = new StringBuilder();
        if (operation.has(ClientConstants.OP_ADDR)) {
            ModelNode address = operation.get(ClientConstants.OP_ADDR);
            for (ModelNode key : address.asList()) {
                Property segment = key.asProperty();
                command.append("/").append(segment.getName()).append("=").append(segment.getValue());
            }
            operation.remove(ClientConstants.OP_ADDR);
        }

        if (operation.has(ClientConstants.OP)) {
            ModelNode op = operation.get(ClientConstants.OP);
            command.append(":").append(op.asString());
            operation.remove(ClientConstants.OP);
        }

        if (operation.has(ClientConstants.OPERATION_HEADERS)) {
            throw new UnsupportedOperationException(ClientConstants.OPERATION_HEADERS
                    + " are not supported");
        }

        StringBuilder attrs = new StringBuilder();
        Iterator<String> keys = operation.keys().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            ModelNode value = operation.get(key);
            if (value.getType() != ModelType.OBJECT) {
                attrs.append(key).append("=").append(value.asString());
            }
            if (keys.hasNext()) {
                attrs.append(",");
            }
        }
        command.append("(").append(attrs).append(")");

        return command.toString();
    }

    @Override
    public void close() {
        client.disconnect();
    }

    public static ManagementInterface create(String host, int port, String username, String password) {
        CLI client = CLI.newInstance();
        client.connect(host, port, username, password.toCharArray());
        return new CliManagementInterface(client);
    }
}
