/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.dmr.ModelNode;

/**
 * Simple {@link AttributeConverter} that provides the operation or model context to a {@link Converter}.
 * @author Paul Ferraro
 */
public class SimpleAttributeConverter implements AttributeConverter {

    public interface Converter {
        void convert(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context);
    }

    private final Converter converter;

    public SimpleAttributeConverter(Converter converter) {
        this.converter = converter;
    }

    @Override
    public final void convertOperationParameter(PathAddress address, String name, ModelNode value, ModelNode operation, TransformationContext context) {
        this.converter.convert(address, name, value, operation, context);
    }

    @Override
    public final void convertResourceAttribute(PathAddress address, String name, ModelNode value, TransformationContext context) {
        this.converter.convert(address, name, value, context.readResource(PathAddress.EMPTY_ADDRESS).getModel(), context);
    }
}
