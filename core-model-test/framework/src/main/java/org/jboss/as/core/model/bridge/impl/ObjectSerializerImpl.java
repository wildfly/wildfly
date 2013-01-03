/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.bridge.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.core.model.bridge.shared.ObjectSerializer;
import org.jboss.as.core.model.test.LegacyModelInitializerEntry;
import org.jboss.as.host.controller.ignored.IgnoreDomainResourceTypeResource;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ObjectSerializerImpl implements ObjectSerializer {

    private static final String PARENT_ADDRESS = "_parent_address";
    private static final String RELATIVE_RESOURCE_ADDRESS = "_relative_resource_address";
    private static final String MODEL_NODE = "_model_node";

    @Override
    public byte[] serializeModelNode(Object object) throws IOException {
        //Happens in the app classloader
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            ((ModelNode)object).writeExternal(bout);
        } finally {
            bout.flush();
            IoUtils.safeClose(bout);
        }
        return bout.toByteArray();
    }

    @Override
    public Object deserializeModelNode(byte[] object) throws IOException {
        //Happens in the child classloader
        InputStream in = new ByteArrayInputStream(object);
        try {
            ModelNode modelNode = new ModelNode();
            modelNode.readExternal(in);
            return modelNode;
        } finally {
            IoUtils.safeClose(in);
        }
    }

    @Override
    public String serializeModelVersion(Object object) {
        //Happens in the app classloader
        return ((ModelVersion)object).toString();

    }

    @Override
    public Object deserializeModelVersion(String object) {
        //Happens in the child classloader
        return ModelVersion.fromString(object);
    }

    @Override
    public byte[] serializeLegacyModelInitializerEntry(Object object) throws IOException {
        //Happens in the app classloader
        LegacyModelInitializerEntry entry = (LegacyModelInitializerEntry)object;
        ModelNode node = new ModelNode();
        if (entry.getParentAddress() != null) {
            node.get(PARENT_ADDRESS).set(entry.getParentAddress().toModelNode());
        }
        node.get(RELATIVE_RESOURCE_ADDRESS).set(PathAddress.pathAddress(entry.getRelativeResourceAddress()).toModelNode());
        if (entry.getModel() != null) {
            node.get(MODEL_NODE).set(entry.getModel());
        }
        return serializeModelNode(node);
    }

    @Override
    public Object deserializeLegacyModelInitializerEntry(byte[] object) throws IOException {
        //Happens in the child classloader
        ModelNode node = (ModelNode)deserializeModelNode(object);
        PathAddress parentAddress = null;
        if (node.hasDefined(PARENT_ADDRESS)) {
            parentAddress = PathAddress.pathAddress(node.get(PARENT_ADDRESS));
        }
        PathElement relativeResourceAddress = PathAddress.pathAddress(node.get(RELATIVE_RESOURCE_ADDRESS)).getElement(0);
        ModelNode model = null;
        if (node.hasDefined(MODEL_NODE)) {
            model = node.get(MODEL_NODE);
        }
        return new LegacyModelInitializerEntry(parentAddress, relativeResourceAddress, model);
    }

    @Override
    public byte[] serializeIgnoreDomainTypeResource(Object object) throws IOException {
        //Happens in the app classloader
        IgnoreDomainResourceTypeResource entry = (IgnoreDomainResourceTypeResource)object;
        ModelNode model = entry.getModel().clone();
        model.get("type").set(entry.getName());
        return serializeModelNode(model);
    }

    @Override
    public Object deserializeIgnoreDomainTypeResource(byte[] object) throws IOException {
        //Happens in the child classloader
        ModelNode model = (ModelNode)deserializeModelNode(object);
        String type = model.require("type").asString();
        ModelNode names = model.get("names");
        Boolean wildcard = null;
        if (model.hasDefined("wildcard")) {
            wildcard = model.get("wildcard").asBoolean();
        }
        return new IgnoreDomainResourceTypeResource(type, names, wildcard);
    }

}
