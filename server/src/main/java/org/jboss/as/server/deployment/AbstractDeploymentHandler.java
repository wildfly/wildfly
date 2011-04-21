/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.server.deployment;

import org.jboss.dmr.ModelNode;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
abstract class AbstractDeploymentHandler {
    protected static DeploymentHandlerUtil.ContentItem[] getContents(ModelNode contentNode) {
        final List<ModelNode> nodes = contentNode.asList();
        final DeploymentHandlerUtil.ContentItem[] contents = new DeploymentHandlerUtil.ContentItem[nodes.size()];
        for(int i = 0; i < contents.length; i++) {
            final ModelNode node = nodes.get(i);
            if (node.has(HASH)) {
                contents[i] = new DeploymentHandlerUtil.ContentItem(node.require(HASH).asBytes());
            } else {
                contents[i] = new DeploymentHandlerUtil.ContentItem(node.require(PATH).asString(), node.require(RELATIVE_TO).asString(), node.require(ARCHIVE).asBoolean());
            }
        }
        return contents;
    }
}
