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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSchema;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.parsing.ProfileParsingCompletionHandler;
import org.jboss.dmr.ModelNode;

/**
 * Implementation of {@link ProfileParsingCompletionHandler} that moves all operations intended for JGroups subsystem
 * while parsing Infinispan subsystem.
 * <p/>
 * Currently used to move channel add operations from pre-3.0 schemas.
 *
 * @author Radoslav Husar
 * @version May 2015
 */
public class InfinispanProfileParsingCompletionHandler implements ProfileParsingCompletionHandler {

    @Override
    public void handleProfileParsingCompletion(final Map<String, List<ModelNode>> profileBootOperations, final List<ModelNode> otherBootOperations) {
        List<ModelNode> infinispanOps = null;
        for (InfinispanSchema schema : InfinispanSchema.values()) {
            infinispanOps = profileBootOperations.get(schema.getNamespaceUri());
            if (infinispanOps != null) {
                break;
            }
        }
        if (infinispanOps == null) return;

        // Extract and remove operations intended for jgroups subsystem
        List<ModelNode> jgroupsOpsToMove = new LinkedList<>();
        for (Iterator<ModelNode> iterator = infinispanOps.iterator(); iterator.hasNext();) {
            ModelNode op = iterator.next();
            PathAddress operationAddress = Operations.getPathAddress(op);

            // If this operation is intended for JGroups subsystem, remove it from here
            if (operationAddress.getElement(0).equals(JGroupsSubsystemResourceDefinition.PATH)) {
                jgroupsOpsToMove.add(op);
                iterator.remove();
            }
        }
        if (jgroupsOpsToMove.isEmpty()) return;

        // Re-add the operations in jgroups subsystem if jgroups subsystem is defined
        List<ModelNode> jgroupsOps = null;
        for (JGroupsSchema schema : JGroupsSchema.values()) {
            jgroupsOps = profileBootOperations.get(schema.getNamespaceUri());
            if (jgroupsOps != null) {
                break;
            }
        }
        if (jgroupsOps == null) return;

        // Re-add all previously removed operations since jgroups subsystem is defined
        jgroupsOps.addAll(jgroupsOpsToMove);
    }
}
