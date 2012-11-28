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
package org.jboss.as.jdr.commands;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;

import org.jboss.dmr.ModelNode;

public class CallAS7 extends JdrCommand {

    private String operation = "read-resource";
    private LinkedList<String> resource = new LinkedList<String>();
    private Map<String, String> parameters = new HashMap<String, String>();
    private String name;

    public CallAS7(String name) {
        this.name = name + ".json";
    }

    public CallAS7 operation(String operation) {
        this.operation = operation;
        return this;
    }

    public CallAS7 param(String key, String val) {
        this.parameters.put(key, val);
        return this;
    }

    public CallAS7 resource(String... parts) {
        for(String part : parts ) {
            this.resource.add(part);
        }
        return this;
    }

    @Override
    public void execute() throws Exception {
        ModelNode request = new ModelNode();
        request.get("operation").set(this.operation);

        assert this.resource.size() % 2 == 0;
        while (this.resource.size() > 0) {
            request.get("address").add(this.resource.removeFirst(),
                    this.resource.removeFirst());
        }

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            request.get(entry.getKey()).set(entry.getValue());
        }

        if (this.env.getHostControllerName() != null) {
            request.get("host").set(this.env.getHostControllerName());
        }

        if (this.env.getServerName() != null) {
            request.get("server").set(this.env.getServerName());
        }

        this.env.getZip().add(this.env.getClient().execute(request).toJSONString(true), this.name);
    }
}
