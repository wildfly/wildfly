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
package org.jboss.as.domain.controller.transformers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class AddDefaultValueResourceTransformer implements ResourceTransformer {

    private Map<String, ModelNode> attributes;

    private AddDefaultValueResourceTransformer(Map<String, ModelNode> attributes) {
        this.attributes = attributes;
    }

    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource)
            throws OperationFailedException {
        ModelNode model = resource.getModel();
        for (Map.Entry<String, ModelNode> entry : attributes.entrySet()) {
            if (!model.hasDefined(entry.getKey())) {
                //Make sure that the default value appears in the transformed resource
                model.get(entry.getKey()).set(entry.getValue());
            }

        }
        ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }

    static AddDefaultValueResourceTransformer create(String name, ModelNode defaultValue) {
        return new AddDefaultValueResourceTransformer(Collections.singletonMap(name, defaultValue));
    }

    static class Builder {
        Map<String, ModelNode> map = new HashMap<String, ModelNode>();

        AddDefaultValueResourceTransformer build() {
            return new AddDefaultValueResourceTransformer(map);
        }

        Builder add(String name, ModelNode defaultValue) {
            map.put(name, defaultValue);
            return this;
        }

        static Builder createBuilder(String name, ModelNode defaultValue) {
            Builder builder = new Builder();
            builder.add(name, defaultValue);
            return builder;
        }
    }
}
