/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * A write-attribute operation handler that translates a value from another attribute
 * @author Paul Ferraro
 */
public class WriteAttributeTranslationHandler implements OperationStepHandler {

    private final Attribute targetAttribute;
    private final AttributeValueTranslator translator;

    public WriteAttributeTranslationHandler(AttributeTranslation translation) {
        this(translation.getTargetAttribute(), translation.getWriteTranslator());
    }

    public WriteAttributeTranslationHandler(Attribute targetAttribute, AttributeValueTranslator translator) {
        this.targetAttribute = targetAttribute;
        this.translator = translator;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode value = context.resolveExpressions(Operations.getAttributeValue(operation));
        ModelNode targetValue = this.translator.translate(context, value);
        ModelNode targetOperation = Operations.createWriteAttributeOperation(context.getCurrentAddress(), this.targetAttribute, targetValue);
        context.getResourceRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, this.targetAttribute.getName()).getWriteHandler().execute(context, targetOperation);
    }
}
