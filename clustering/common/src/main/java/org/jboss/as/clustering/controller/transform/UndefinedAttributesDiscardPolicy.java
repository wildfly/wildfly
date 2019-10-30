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

package org.jboss.as.clustering.controller.transform;

import java.util.Collection;

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

    private final Collection<Attribute> attributes;

    public UndefinedAttributesDiscardPolicy(Collection<Attribute> attributes) {
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
