/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

/**
 * Handler responsible for adding a datasource resource to the model
 *
 * @author Thomas.Diesler@jboss.com
 * @since 07-Nov-2011
 */
class JAXRDatasourceAttributeHandler extends AbstractAddStepHandler {

    static final JAXRDatasourceAttributeHandler INSTANCE = new JAXRDatasourceAttributeHandler();

    // Hide ctor
    private JAXRDatasourceAttributeHandler() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode jndiName = operation.get(ModelConstants.DATASOURCE);
        model.get(ModelConstants.DATASOURCE).set(jndiName);
    }

    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode subsystem = new ModelNode();
            subsystem.get(ModelDescriptionConstants.DESCRIPTION).set("Adds the datasource used by the JAXR subsystem");
            subsystem.get(ModelDescriptionConstants.OPERATION_NAME).set(ADD);
            return subsystem;
        }
    };
}
