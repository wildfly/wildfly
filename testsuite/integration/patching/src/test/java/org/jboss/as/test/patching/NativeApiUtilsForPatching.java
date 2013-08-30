/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.patching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * @author Jan Martiska
 */
public class NativeApiUtilsForPatching {

    /**
     * prepares a DMR operation for applying a patch
     */
    public static Operation createPatchOperation(File patchFile) throws FileNotFoundException {
        final ModelNode patchOperation = new ModelNode();
        patchOperation.get(ModelDescriptionConstants.OP_ADDR).add("core-service", "patching");
        patchOperation.get(ModelDescriptionConstants.OP).set("patch");
        patchOperation.get("content").get(0).get("input-stream-index").set(0);
        return OperationBuilder.create(patchOperation)
                .addInputStream(new FileInputStream(patchFile)).build();
    }

    /**
     * prepares a DMR operation for applying a patch
     */
    public static Operation createRollbackOperation(String patchId) throws FileNotFoundException {
        final ModelNode patchOperation = new ModelNode();
        patchOperation.get(ModelDescriptionConstants.OP_ADDR).add("core-service", "patching");
        patchOperation.get(ModelDescriptionConstants.OP).set("rollback");
        patchOperation.get("patch-id").set(patchId);
        patchOperation.get("reset-configuration").set(true);
        return OperationBuilder.create(patchOperation).build();
    }

    /**
     * returns a list of currently installed patches, containing their IDs
     */
    public static List<String> getInstalledPatches(ModelControllerClient client) throws IOException {
        final ModelNode patchOperation = new ModelNode();
        patchOperation.get(ModelDescriptionConstants.OP_ADDR).add("core-service", "patching");
        patchOperation.get(ModelDescriptionConstants.OP).set("read-resource");
        patchOperation.get("include-runtime").set(true);
        ModelNode ret = client.execute(patchOperation);
        List<ModelNode> list = ret.get("result").get("patches").asList();
        List<String> patchesListString = new ArrayList<String>();
        for (ModelNode n : list) {
            patchesListString.add(n.asString());
        }
        return patchesListString;
    }

    public static List<ModelNode> getHistory(ModelControllerClient client) throws IOException {
        final ModelNode readOp = new ModelNode();
        readOp.get(ModelDescriptionConstants.OP_ADDR).add("core-service", "patching");
        readOp.get(ModelDescriptionConstants.OP).set("show-history");
        return client.execute(readOp).get("result").asList();
    }

    /**
     * Gets the history item of a patch specified by patchId.
     * Returns a ModelNode like this:
     * {
     *    "one-off" : "ed7a416f-a53b-4583-8d7e-6c01afc249f6",
     *    "applied-at" : "7/25/13 4:47 PM";
     * }
     */
    public static ModelNode getHistoryItemForOneOffPatch(ModelControllerClient client, String patchId)
            throws IOException {
        final ModelNode readOp = new ModelNode();
        readOp.get(ModelDescriptionConstants.OP_ADDR).add("core-service", "patching");
        readOp.get(ModelDescriptionConstants.OP).set("show-history");
        List<ModelNode> list = client.execute(readOp).get("result").asList();
        for (ModelNode item : list) {
            if (item.get("patch-id").asString().equalsIgnoreCase(patchId))
                return item;
        }
        return null;
    }

}
