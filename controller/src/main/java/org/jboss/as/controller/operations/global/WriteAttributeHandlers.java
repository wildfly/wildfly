/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class WriteAttributeHandlers {
    public static abstract class AbstractWriteAttributeOperationHandler implements ModelUpdateOperationHandler {

        @Override
        public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
            Cancellable cancellable = Cancellable.NULL;
            try {
                final String name = operation.require(NAME).asString();
                final ModelNode value = operation.require(VALUE);

                String error = validateValue(name, value);
                if (error != null) {
                    resultHandler.handleFailed(new ModelNode().set(error));
                } else {
                    context.getSubModel().require(name).set(value);
                    resultHandler.handleResultComplete(null);
                }

            } catch (final Exception e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }
            return cancellable;
        }

        protected abstract String validateValue(String name, ModelNode value);
    }

    public static class ValidatingWriteAttributeOperationHandler extends AbstractWriteAttributeOperationHandler implements ModelUpdateOperationHandler {
        protected final ModelType type;
        protected final boolean nullable;

        public ValidatingWriteAttributeOperationHandler(ModelType type) {
            this(type, false);
        }

        public ValidatingWriteAttributeOperationHandler(ModelType type, boolean nullable) {
            this.type = type;
            this.nullable = nullable;
        }

        @Override
        protected String validateValue(final String name, final ModelNode value) {
            if (!value.isDefined()) {
                if (!nullable) {
                    return "Attribute " + name + " may not be null "; //TODO i18n
                }
            } else {
                if (value.getType() != type) {
                    return "Wrong type for " + name + ". Expected " + type + " but was " + value.getType(); //TODO i18n
                }
            }
            return null;
        }
    }

}
