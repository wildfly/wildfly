/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar
 * @created 8.12.11 0:19
 */
class MailServerAdd extends AbstractAddStepHandler {

    private final AttributeDefinition[] attributes;

    MailServerAdd(AttributeDefinition[] attributes) {
        this.attributes = attributes;
    }

    /**
     * Populate the given node in the persistent configuration model based on the values in the given operation.
     *
     * @param operation the operation
     * @param model     persistent configuration model node that corresponds to the address of {@code operation}
     * @throws org.jboss.as.controller.OperationFailedException
     *          if {@code operation} is invalid or populating the model otherwise fails
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : attributes) {
            def.validateAndSet(operation, model);
        }
    }

}
