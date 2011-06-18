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

package org.jboss.as.controller;

import java.util.List;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class NewCompositeOperationHandler implements NewStepHandler {
    private final NewStepHandler rootHandler;

    NewCompositeOperationHandler(final NewStepHandler handler) {
        rootHandler = handler;
    }

    public void execute(final NewOperationContext context, final ModelNode operation) {
        final List<ModelNode> list = operation.get(OPERATIONS).asList();
        ModelNode responseList = context.getResult().setEmptyList();
        final int size = list.size();
        for (int i = size - 1; i >= 0; i --) {
            final ModelNode subOperation = list.get(i);
            context.addStep(responseList.get(i).setEmptyObject(), subOperation, rootHandler, NewOperationContext.Stage.IMMEDIATE);
        }
        context.completeStep();
    }
}
