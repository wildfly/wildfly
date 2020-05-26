/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * An attribute translator that converts a single value to/from a list of values.
 * @author Paul Ferraro
 */
public class ListAttributeTranslation implements AttributeTranslation {
    private static final AttributeValueTranslator READ_TRANSLATOR = new AttributeValueTranslator() {
        @Override
        public ModelNode translate(OperationContext context, ModelNode value) throws OperationFailedException {
            return value.isDefined() ? value.asList().get(0) : value;
        }
    };
    private static final AttributeValueTranslator WRITE_TRANSLATOR = new AttributeValueTranslator() {
        @Override
        public ModelNode translate(OperationContext context, ModelNode value) throws OperationFailedException {
            return new ModelNode().add(value);
        }
    };

    private final Attribute targetAttribute;

    public ListAttributeTranslation(Attribute targetAttribute) {
        this.targetAttribute = targetAttribute;
    }

    @Override
    public Attribute getTargetAttribute() {
        return this.targetAttribute;
    }

    @Override
    public AttributeValueTranslator getReadTranslator() {
        return READ_TRANSLATOR;
    }

    @Override
    public AttributeValueTranslator getWriteTranslator() {
        return WRITE_TRANSLATOR;
    }
}
