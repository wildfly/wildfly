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

/**
 * <p>
 * Command to call the AS 7 management system and store the output in a file.
 *</p>
 * <p>
 * This class' methods are meant to be chained together to describe a management call. For example:
 *</p>
 * <pre>
 * new CallAS7("my_file").resource("foo", "bar").operation("read-resource")
 *</pre>
 *
 * <p>
 * will return a configured CallAS7 instance that will, when executed call {@code read-resource}
 * on the {@code /foo=bar/} resource, and store the output in a file called {@code my_file.json}.
 * </p>
 */
public class CallAS7 extends JdrCommand {

    private String operation = "read-resource";
    private LinkedList<String> resource = new LinkedList<String>();
    private Map<String, String> parameters = new HashMap<String, String>();
    private String name;

    /**
     * constructs an instance and sets the resulting file name
     *
     * @param name of the file to write results to
     */
    public CallAS7(String name) {
        this.name = name + ".json";
    }

    /**
     * sets the operation to call
     *
     * @param operation to call, defaults to {@code read-resource}
     * @return this
     */
    public CallAS7 operation(String operation) {
        this.operation = operation;
        return this;
    }

    /**
     * adds a key/value parameter pair to the call
     *
     * @param key
     * @param val
     * @return this
     */
    public CallAS7 param(String key, String val) {
        this.parameters.put(key, val);
        return this;
    }

    /**
     * appends resource parts to the resource to call
     * <p></p>
     * If you want to call /foo=bar/baz=boo/, do this:
     * <pre>
     * .resource("foo", "bar", "baz", "boo")
     * </pre>
     *
     * @param parts to call
     * @return this
     */
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
