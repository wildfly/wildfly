/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.transform;

import java.util.List;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * Converts a singleton list to a single value.
 * @author Paul Ferraro
 */
public class SingletonListAttributeConverter extends SimpleAttributeConverter {

    public SingletonListAttributeConverter(Attribute listAttribute) {
        this(listAttribute.getDefinition());
    }

    public SingletonListAttributeConverter(AttributeDefinition listAttribute) {
        super(new Converter() {
            @Override
            public void convert(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context) {
                if (model.hasDefined(listAttribute.getName())) {
                    List<ModelNode> list = model.get(listAttribute.getName()).asList();
                    if (!list.isEmpty()) {
                        value.set(list.get(0));
                    }
                }
            }
        });
    }
}
