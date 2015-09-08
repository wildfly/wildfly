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

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * A generic and reusable operation context attachment that exposes an initial value of an attribute before a write.
 * Values have to be looked up by the {@link PathAddress} address of the attribute and attribute's name.
 *
 * @author Radoslav Husar
 * @version August 2015
 */
public class InitialAttributeValueOperationContextAttachment {

    public static final OperationContext.AttachmentKey<InitialAttributeValueOperationContextAttachment> INITIAL_VALUES_ATTACHMENT = OperationContext.AttachmentKey.create(InitialAttributeValueOperationContextAttachment.class);

    private volatile Map<String, ModelNode> initialValues = new HashMap<>();

    public ModelNode putIfAbsentInitialValue(PathAddress address, String attributeName, ModelNode initialValue) {
        return initialValues.putIfAbsent(this.keyFor(address, attributeName), initialValue.clone());
    }

    public ModelNode getInitialValue(PathAddress address, String attributeName) {
        return initialValues.get(this.keyFor(address, attributeName));
    }


    private String keyFor(PathAddress address, String attributeName) {
        return address.toString() + attributeName;
    }
}
