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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.XML_NAMESPACES;

import java.io.File;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Grabs the model versions for the currently running standalone version for use in the
 * <a href="https://community.jboss.org/wiki/AS7ManagementVersions">AS7 Management Versions wiki</a>.
 * It also saves the model versions in dmr format to {@code target/standalone-model-versions-running.dmr}
 * If this is for a released version so that it can be used for comparisons in the future, this file should be copied to
 * {@code src/test/resources/legacy-models} and {@code running} replaced with the real version of the running server, e.g.
 * {@code src/test/resources/legacy-models/standalone-model-versions-7.1.2.Final}. *
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GrabModelVersionsUtil {

    public static void main(String[] args) throws Exception {
        ModelNode versions = Tools.getCurrentModelVersions();
        System.out.println("<table border=\"1\">");
        System.out.println("<tr><th>Subsystem</th><th>Management Version</th><th>Schemas</th></tr>");
        System.out.print("<tr valign=\"top\" align=\"left\"><td><b>Standalone core</b></td><td>");
        System.out.print(Tools.createModelVersion(versions.get(Tools.CORE, Tools.STANDALONE)));
        System.out.println("</td><td>&nbsp;</td></tr>");

        for (Property entry : versions.get(SUBSYSTEM).asPropertyList()) {
            System.out.print("<tr valign=\"top\" align=\"left\"><td><b>");
            System.out.print(entry.getName());
            System.out.print("</b></td><td>");
            System.out.print(Tools.createModelVersion(entry.getValue()));
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

        System.out.println("----------------");
        Tools.serializeModeNodeToFile(versions, new File("target/standalone-model-versions-running.dmr"));
    }
}
