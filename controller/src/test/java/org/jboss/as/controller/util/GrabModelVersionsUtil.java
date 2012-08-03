/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.XML_NAMESPACES;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GrabModelVersionsUtil {

    public static void main(String[] args) throws Exception {
        ModelControllerClient client = ModelControllerClient.Factory.create("localhost", 9999);
        try {
            ModelNode op = new ModelNode();
            op.get(OP).set(READ_RESOURCE_OPERATION);
            op.get(OP_ADDR).setEmptyList();
            ModelNode result = getAndCheckResult(client.execute(op));
            ModelVersion coreModelVersion = createModelVersion(result);

            System.out.println("<table border=\"1\">");
            System.out.println("<tr><th>Subsystem</th><th>Management Version</th><th>Schemas</th></tr>");
            System.out.print("<tr valign=\"top\" align=\"left\"><td><b>Standalone core</b></td><td>");
            System.out.print(coreModelVersion);
            System.out.println("</td><td>&nbsp;</td></tr>");

            op.get(OP_ADDR).add(EXTENSION, "*").add(SUBSYSTEM, "*");
            result = getAndCheckResult(client.execute(op));

            TreeMap<String, ModelNode> map = new TreeMap<String, ModelNode>();

            List<ModelNode> subsystemResults = result.asList();
            for (ModelNode subsystemResult : subsystemResults) {
                String subsystemName = PathAddress.pathAddress(subsystemResult.get(OP_ADDR)).getLastElement().getValue();
                map.put(subsystemName, getAndCheckResult(subsystemResult));
            }

            for (Map.Entry<String, ModelNode> entry : map.entrySet()) {
                System.out.print("<tr valign=\"top\" align=\"left\"><td><b>");
                System.out.print(entry.getKey());
                System.out.print("</b></td><td>");
                System.out.print(createModelVersion(entry.getValue()));
                System.out.print("<td>");

                boolean first = true;
                for (ModelNode ns : entry.getValue().get(XML_NAMESPACES).asList()) {
                    if (first) {
                        first = false;
                    } else {
                        System.out.println("<br/>");
                    }
                    System.out.print(ns.asString());
                }


                System.out.print("</td>");
                System.out.println("</td></tr>");
            }


            System.out.println("</table>");

        } finally {
            IoUtils.safeClose(client);
        }
    }

    private static ModelNode getAndCheckResult(ModelNode result) {
        if (!result.get(OUTCOME).asString().equals(SUCCESS)) {
            throw new RuntimeException(result.get(FAILURE_DESCRIPTION).asString());
        }
        return result.get(RESULT);
    }

    private static ModelVersion createModelVersion(ModelNode node) {
        return ModelVersion.create(
                readVersion(node, MANAGEMENT_MAJOR_VERSION),
                readVersion(node, MANAGEMENT_MINOR_VERSION),
                readVersion(node, MANAGEMENT_MICRO_VERSION));
    }

    private static int readVersion(ModelNode node, String name) {
        if (!node.hasDefined(name)) {
            return 0;
        }
        return node.get(name).asInt();
    }
}
