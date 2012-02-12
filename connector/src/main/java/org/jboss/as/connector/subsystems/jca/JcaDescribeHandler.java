/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.connector.subsystems.jca;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_FAIL_ON_ERROR;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_FAIL_ON_WARN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * JCA describe handler
 */
class JcaDescribeHandler implements OperationStepHandler, DescriptionProvider {
    static final JcaDescribeHandler INSTANCE = new JcaDescribeHandler();

    /**
     * {@inheritDoc}
     */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode result = context.getResult();
        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);

        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());

        result.add(subsystemAdd);

        if (subModel.hasDefined(ARCHIVE_VALIDATION)) {
            ModelNode mn = subModel.get(ARCHIVE_VALIDATION);
            if (mn.hasDefined(ARCHIVE_VALIDATION_ENABLED)) {
                subsystemAdd.get(ARCHIVE_VALIDATION).get(ARCHIVE_VALIDATION_ENABLED).set(mn.get(ARCHIVE_VALIDATION_ENABLED));
            }
            if (mn.hasDefined(ARCHIVE_VALIDATION_FAIL_ON_WARN)) {
                subsystemAdd.get(ARCHIVE_VALIDATION).get(ARCHIVE_VALIDATION_FAIL_ON_WARN).set(mn.get(ARCHIVE_VALIDATION_FAIL_ON_WARN));
            }
            if (mn.hasDefined(ARCHIVE_VALIDATION_FAIL_ON_ERROR)) {
                subsystemAdd.get(ARCHIVE_VALIDATION).get(ARCHIVE_VALIDATION_FAIL_ON_ERROR).set(mn.get(ARCHIVE_VALIDATION_FAIL_ON_ERROR));
            }
        }

        /*
          final ModelNode add = createEmptyAddOperation();
          final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

          if (model.hasDefined(BEAN_VALIDATION_ENABLED)) {
          add.get(BEAN_VALIDATION_ENABLED).set(model.get(BEAN_VALIDATION_ENABLED));
          }
          if (model.hasDefined(ARCHIVE_VALIDATION_ENABLED)) {
          add.get(ARCHIVE_VALIDATION_ENABLED).set(model.get(ARCHIVE_VALIDATION_ENABLED));
          }
          if (model.hasDefined(ARCHIVE_VALIDATION_FAIL_ON_ERROR)) {
          add.get(ARCHIVE_VALIDATION_FAIL_ON_ERROR).set(model.get(ARCHIVE_VALIDATION_FAIL_ON_ERROR));
          }
          if (model.hasDefined(ARCHIVE_VALIDATION_FAIL_ON_WARN)) {
          add.get(ARCHIVE_VALIDATION_FAIL_ON_WARN).set(model.get(ARCHIVE_VALIDATION_FAIL_ON_WARN));
          }
          if (model.hasDefined(CACHED_CONNECTION_MANAGER_DEBUG)) {
                add.get(CACHED_CONNECTION_MANAGER_DEBUG).set(model.get(CACHED_CONNECTION_MANAGER_DEBUG));
                }
                if (model.hasDefined(CACHED_CONNECTION_MANAGER_ERROR)) {
                add.get(CACHED_CONNECTION_MANAGER_ERROR).set(model.get(CACHED_CONNECTION_MANAGER_ERROR));
            }

            final ModelNode result = context.getResult();
            result.add(add);

            if (model.hasDefined(THREAD_POOL)) {
                ModelNode pools = model.get(THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    if (poolProp.getName().equals(LONG_RUNNING_THREADS)) {
                        addBoundedQueueThreadPool(result, poolProp.getValue(), PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(THREAD_POOL, LONG_RUNNING_THREADS));
                    } else if (poolProp.getName().equals(SHORT_RUNNING_THREADS)) {
                        addBoundedQueueThreadPool(result, poolProp.getValue(), PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(THREAD_POOL, SHORT_RUNNING_THREADS));
                    }
                }
            }
            */

        context.completeStep();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getSubsystemDescribeOperation(locale);
    }
}
