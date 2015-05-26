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
import org.jboss.as.clustering.jgroups.subsystem.JGroupsExtension;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSchema;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.parsing.ProfileParsingCompletionHandler;
import org.jboss.dmr.ModelNode;

/**
 * Implementation of ProfileParsingCompletionHandler that moves channel add operations from Infinispan subsystem to
 * JGroups subsystem.
 *
 * @author Radoslav Husar
 * @version May 2015
 */
public class InfinispanProfileParsingCompletionHandler implements ProfileParsingCompletionHandler {

    @Override
    public void handleProfileParsingCompletion(final Map<String, List<ModelNode>> profileBootOperations, final List<ModelNode> otherBootOperations) {
        // Find if legacy subsystem was parsed
        List<ModelNode> legacyInfinispanOps = null;
        for (InfinispanSchema schema : InfinispanSchema.values()) {
            if (!schema.since(InfinispanSchema.VERSION_3_0)) {
                legacyInfinispanOps = profileBootOperations.get(schema.getNamespaceUri());
                if (legacyInfinispanOps != null) {
                    break;
                }
            }
        }
        if (legacyInfinispanOps == null) return;

        // Extract and remove operations intended for jgroups subsystem
        List<ModelNode> jgroupsOps = new LinkedList<>();
        for (Iterator<ModelNode> iterator = legacyInfinispanOps.iterator(); iterator.hasNext();) {
            ModelNode op = iterator.next();
            PathAddress operationAddress = Operations.getPathAddress(op);

            // If this operation is intended for JGroups subsystem, remove it from here
            if (operationAddress.getElement(0).getValue().equals(JGroupsExtension.SUBSYSTEM_NAME)) {
                jgroupsOps.add(op);
                iterator.remove();
            }
        }
        if (jgroupsOps.isEmpty()) return;

        // Re-add the operations in jgroups subsystem if jgroups subsystem is defined
        List<ModelNode> jgroupsLegacyOps = null;
        for (JGroupsSchema schema : JGroupsSchema.values()) {
            jgroupsLegacyOps = profileBootOperations.get(schema.getNamespaceUri());
            if (jgroupsLegacyOps != null) {
                break;
            }
        }
        if (jgroupsLegacyOps == null) return;

        // Re-add all previously removed operations since jgroups subsystem is defined
        jgroupsLegacyOps.addAll(jgroupsOps);
    }
}
