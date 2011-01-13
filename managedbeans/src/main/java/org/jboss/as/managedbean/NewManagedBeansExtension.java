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

package org.jboss.as.managedbean;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Domain extension used to initialize the managed bean subsystem element handlers.
 *
 * @author Emanuel Muckenhuber
 */
public class NewManagedBeansExtension implements NewExtension {

    public static final String NAMESPACE = "urn:jboss:domain:managedbeans:1.0";

    private static final ManagedBeanSubsystemElementParser parser = new ManagedBeanSubsystemElementParser();
    private static final DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };


    /** {@inheritDoc} */
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration registration = context.registerSubsystem("managed-beans");
        final ModelNodeRegistration nodeRegistration = registration.registerSubsystemModel(DESCRIPTION);
        nodeRegistration.registerOperationHandler(ADD, NewManagedBeansSubsystemAdd.INSTANCE, DESCRIPTION, false);
    }

    /** {@inheritDoc} */
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(NAMESPACE, parser, parser);
    }

    static class ManagedBeanSubsystemElementParser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

        /** {@inheritDoc} */
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            ParseUtils.requireNoContent(reader);
            final ModelNode update = new ModelNode();
            update.get(OP).set("add");
            update.get(OP_ADDR).setEmptyObject();
            list.add(update);
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter arg0, ModelNode arg1) throws XMLStreamException {
            //
        }

    }

}
