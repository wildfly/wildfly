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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.List;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class GlobalOperationHandlers {

    static final String[] NO_LOCATION = new String[0];

    static String[] getLocation(ModelNode operation) {
        List<ModelNode> nodes = operation.get(ADDRESS).asList();
        String[] loc = new String[nodes.size()];
        for (int i = 0 ; i < loc.length ; i++) {
            loc[i] = nodes.get(i).asString();
        }
        return loc;
    }

    public static final ModelQueryOperationHandler READ_SUB_MODEL = new ModelQueryOperationHandler() {
        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
            try {
                final ModelNode result;
                if (operation.require(REQUEST_PROPERTIES).require(RECURSIVE).asBoolean()) {
                    result = context.getSubModel().clone();
                } else {
                    result = new ModelNode();
                    ModelNode subModel = context.getSubModel().clone();
                    for (String key : subModel.keys()) {
                        ModelNode value = subModel.get(key);
                        switch (value.getType()) {
                            case LIST:
                            case OBJECT:
                            case PROPERTY:
                                result.get(key).set(new ModelNode());
                            break;
                            default:
                                result.get(key).set(value);
                        }
                    }
                }

                resultHandler.handleResultFragment(NO_LOCATION, result);
                resultHandler.handleResultComplete(null);
            } catch (Exception e) {
                resultHandler.handleFailed(null);
            }
            return Cancellable.NULL;
        }
    };

    public static final ModelQueryOperationHandler READ_NAMED_ATTRIBUTE = new ModelQueryOperationHandler() {

        @Override
        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
            try {
                String attributeName = operation.require(REQUEST_PROPERTIES).require(NAME).asString();
                ModelNode result = context.getSubModel().require(attributeName).clone();

                resultHandler.handleResultFragment(new String[0], result);
                resultHandler.handleResultComplete(null);
            } catch (Exception e) {
                resultHandler.handleFailed(null);
            }
            return Cancellable.NULL;
        }
    };


    public static final ModelQueryOperationHandler READ_ATTRIBUTE_OF_TYPE = new ModelQueryOperationHandler() {

        @Override
        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
            try {
                ModelType type = operation.require(REQUEST_PROPERTIES).require(TYPE).asType();

                ModelNode subModel = context.getSubModel().clone();

                ModelNode result = new ModelNode();
                for (String key : subModel.keys()) {
                    ModelNode current = subModel.get(key);
                    if (current.getType() == type) {
                        result.add(key, current.clone());
                    }
                }

                resultHandler.handleResultFragment(new String[0], result.clone());
                resultHandler.handleResultComplete(null);
            } catch (Exception e) {
                resultHandler.handleFailed(null);
            }
            return Cancellable.NULL;
        }
    };

}
