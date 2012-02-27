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

package org.jboss.as.web;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.web.WebAccessLogDefinition.ACCESS_LOG_ATTRIBUTES;

/**
 * {@code OperationHandler} responsible for defining the accesslog entry.
 *
 * @author Jean-Frederic Clere
 */
class WebAccessLogAdd extends AbstractAddStepHandler {

    static final WebAccessLogAdd INSTANCE = new WebAccessLogAdd();

    private WebAccessLogAdd() {
        //
    }

    /* @Override
    protected void populateModel(final ModelNode operation, final Resource resource) {
        final ModelNode model = resource.getModel();
       WebConfigurationHandlerUtils.populateAccessLog(model, operation, resource);
    }*/


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition def : ACCESS_LOG_ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
    }
}
