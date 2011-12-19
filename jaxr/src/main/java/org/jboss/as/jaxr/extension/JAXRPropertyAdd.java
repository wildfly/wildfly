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
package org.jboss.as.jaxr.extension;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.jaxr.JAXRConfiguration;
import org.jboss.as.jaxr.ModelConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Kurt Stam
 */
public class JAXRPropertyAdd extends AbstractAddStepHandler {
    static final JAXRPropertyAdd INSTANCE = new JAXRPropertyAdd();

    private JAXRPropertyAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.get(ModelConstants.VALUE).set(operation.get(ModelConstants.VALUE));
    }

    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            ResourceBundle resbundle = JAXRConfiguration.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.ADD);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("jaxr.property.add"));
            node.get(ModelDescriptionConstants.PROPERTIES, ModelConstants.VALUE, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("jaxr.property.value"));
            node.get(ModelDescriptionConstants.PROPERTIES, ModelConstants.VALUE, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            node.get(ModelDescriptionConstants.PROPERTIES, ModelConstants.VALUE, ModelDescriptionConstants.REQUIRED).set(true);
            return node;
        }
    };
}
