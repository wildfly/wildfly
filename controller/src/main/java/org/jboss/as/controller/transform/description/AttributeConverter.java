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
package org.jboss.as.controller.transform.description;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface AttributeConverter {

    void convertOperationParameter(PathAddress address, String name, ModelNode attributeValue, ModelNode operation, TransformationContext context);

    void convertResourceAttribute(PathAddress address, String name, ModelNode attributeValue, TransformationContext context);

    public abstract class DefaultAttributeConverter implements AttributeConverter {
        @Override
        public void convertOperationParameter(PathAddress address, String name, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
            convertAttribute(address, name, attributeValue, context);
        }

        @Override
        public void convertResourceAttribute(PathAddress address, String name, ModelNode attributeValue, TransformationContext context) {
            convertAttribute(address, name, attributeValue, context);
        }

        protected abstract void convertAttribute(PathAddress address, String name, ModelNode attributeValue, TransformationContext context);
    }

    public static class Factory {
        public static AttributeConverter createHardCoded(final ModelNode hardCodedValue) {
            return new DefaultAttributeConverter() {
                @Override
                public void convertAttribute(PathAddress address, String name, ModelNode attributeValue, TransformationContext context) {
                    attributeValue.set(hardCodedValue);
                }
            };
        }
    }

    AttributeConverter NAME_FROM_ADDRESS = new DefaultAttributeConverter() {
        @Override
        public void convertAttribute(PathAddress address, String name, ModelNode attributeValue, TransformationContext context) {
            PathElement element = address.getLastElement();
            attributeValue.set(element.getValue());
        }
    };
}
