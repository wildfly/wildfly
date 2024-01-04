/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.transform;

import java.util.Arrays;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardPolicy;
import org.jboss.as.controller.transform.description.DynamicDiscardPolicy;
import org.jboss.dmr.ModelNode;

/**
 * Convenience implementation of {@link DynamicDiscardPolicy} that silently discards (i.e. {@link DiscardPolicy#SILENT}) if none of the attributes are defined;
 * rejects otherwise (i.e. {@link DiscardPolicy#REJECT_AND_WARN}.
 *
 * @author Radoslav Husar
 * @version August 2015
 */
public class UndefinedAttributesDiscardPolicy implements DynamicDiscardPolicy {

    private final Iterable<Attribute> attributes;

    public UndefinedAttributesDiscardPolicy(Attribute... attributes) {
        this(Arrays.asList(attributes));
    }

    public UndefinedAttributesDiscardPolicy(Iterable<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public DiscardPolicy checkResource(TransformationContext context, PathAddress address) {
        ModelNode model = Resource.Tools.readModel(context.readResourceFromRoot(address));
        for (Attribute attribute : this.attributes) {
            if (model.hasDefined(attribute.getName())) {
                return DiscardPolicy.REJECT_AND_WARN;
            }
        }
        return DiscardPolicy.SILENT;
    }
}
