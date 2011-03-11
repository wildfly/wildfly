/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.demos.description.runner;

import java.net.InetAddress;

import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Brian Stansberry
 */
public class ExampleRunner {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        ModelControllerClient client = null;
        try {
            System.out.println("Connecting");
            client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            System.out.println("Connected");

            System.out.println("Dumping resource tree\n");
            ModelNode request = new ModelNode();
            request.get("operation").set("read-resource");
            request.get("address").setEmptyList();
            // request.get("address").set("subsystem", "threads");
            request.get("recursive").set(true);
            ModelNode r = client.execute(OperationBuilder.Factory.create(request).build());
            System.out.println(r);

            System.out.println("Dumping resource description tree\n");
            request = new ModelNode();
            request.get("operation").set("read-resource-description");
            request.get("address").setEmptyList();
            request.get("operations").set(true);
            request.get("recursive").set(true);
            r = client.execute(OperationBuilder.Factory.create(request).build());
            System.out.println(r);

        } finally {
            StreamUtils.safeClose(client);
            System.out.println("Closed");
        }

    }

}
