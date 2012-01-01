/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.extension.remove;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TestExtension implements Extension {

    static final String SUBSYSTEM_NAME = "test-extension";

    @Override
    public void initialize(ExtensionContext context) {
        System.out.println("Initializing TestExtension");
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);

        ManagementResourceRegistration root = registration.registerSubsystemModel(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A test subsystem");
                node.get(ATTRIBUTES, "attribute", DESCRIPTION).set("An attribute");
                node.get(ATTRIBUTES, "attribute", TYPE).set(ModelType.STRING);
                node.get(CHILDREN, "child").setEmptyObject();
                return node;
            }
        });
        root.registerOperationHandler(ADD, new AddSubsystemHandler(), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        });
        root.registerReadWriteAttribute("attribute", null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        root.registerOperationHandler(REMOVE, new RemoveHandler(), new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        });

        ManagementResourceRegistration child = root.registerSubModel(PathElement.pathElement("child"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A test child");
                node.get(ATTRIBUTES, "child-attr", DESCRIPTION).set("An attribute");
                node.get(ATTRIBUTES, "child-attr", TYPE).set(ModelType.STRING);
                return node;
            }
        });
        child.registerOperationHandler(ADD, new AddChildHandler(), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        });
        child.registerOperationHandler(REMOVE, new RemoveHandler(), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        });
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, "urn:jboss:domain:remoting:1.0", new XMLElementReader<List<ModelNode>>() {

            @Override
            public void readElement(XMLExtendedStreamReader reader, List<ModelNode> value) throws XMLStreamException {
            }
        });
    }

    private static class AddSubsystemHandler extends AbstractAddStepHandler {

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            model.get("attribute").set("initialized");
            model.get("child").setEmptyObject();
        }
    }

    private static class RemoveHandler extends AbstractRemoveStepHandler {

    }

    private static class AddChildHandler extends AbstractAddStepHandler {

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            model.get("child-attr").set("initialized");
        }

    }
}
