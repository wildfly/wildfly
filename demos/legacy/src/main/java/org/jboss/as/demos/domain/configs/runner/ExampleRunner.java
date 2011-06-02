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
package org.jboss.as.demos.domain.configs.runner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

/**
 * Demonstration of basic aspects of reading domain and host controller configurations
 * via the domain management API.
 *
 * @author Brian Stansberry
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {

        final ModelControllerClient client = ModelControllerClient.Factory.create("localhost", 9999);
        try {
            final ModelNode domainOp = new ModelNode();
            domainOp.get(OP).set(READ_RESOURCE_OPERATION);
            domainOp.get(OP_ADDR).setEmptyList();
            domainOp.get(RECURSIVE).set(true);
            domainOp.get("proxies").set(false);

            ModelNode result = client.execute(domainOp);
            if(! SUCCESS.equals(result.get(OUTCOME).asString())) {
                throw new OperationFailedException(result.get(FAILURE_DESCRIPTION));
            }

            System.out.println("-- domain configuration");
            final ModelNode domainResult = result.get(RESULT).clone();
            System.out.println(domainResult);
            System.out.println("--");

            final ModelNode hostOp = new ModelNode();
            hostOp.get(OP).set(READ_RESOURCE_OPERATION);
            hostOp.get(OP_ADDR).setEmptyList().add(HOST, "local");
            hostOp.get(RECURSIVE).set(true);
            hostOp.get("proxies").set(false);

            result = client.execute(hostOp);
            if(! SUCCESS.equals(result.get(OUTCOME).asString())) {
                throw new OperationFailedException(result.get(FAILURE_DESCRIPTION));
            }

            System.out.println("-- host configuration");
            final ModelNode hostResult = result.get(RESULT).clone();
            System.out.println(hostResult);
            System.out.println("--");

        } finally {
            StreamUtils.safeClose(client);
        }
    }

}
