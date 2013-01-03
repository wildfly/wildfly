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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import java.io.File;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 * Grabs the full domain resource description of a running domain instance and writes it out to {@code target/domain-resource-definition-running.dmr}.
 * The host model is written out to {@code target/host-resource-definition-running.dmr}.
 * If this is for a released version so that it can be used for comparisons in the future, these files should be copied to
 * {@code src/test/resources/legacy-models} and {@code running} replaced with the real version of the running server, e.g.
 * {@code src/test/resources/legacy-models/domain-resource-definition-7.1.2.Final} and {@code src/test/resources/legacy-models/host-resource-definition-7.1.2.Final}.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DumpDomainResourceDefinitionUtil {

    public static void main(String[] args) throws Exception {
        ModelNode resourceDefinition = Tools.getCurrentRunningDomainResourceDefinition();
        System.out.println("Dumping domain model....");
        Tools.serializeModeNodeToFile(resourceDefinition, new File("target/" + ResourceType.DOMAIN.toString().toLowerCase() +
        		"-resource-definition-running.dmr"));

        resourceDefinition = Tools.getCurrentRunningResourceDefinition(PathAddress.pathAddress(PathElement.pathElement(HOST, "master")));
        System.out.println("Dumping host model....");
        Tools.serializeModeNodeToFile(resourceDefinition, new File("target/" + ResourceType.HOST.toString().toLowerCase() +
                        "-resource-definition-running.dmr"));

    }
}
