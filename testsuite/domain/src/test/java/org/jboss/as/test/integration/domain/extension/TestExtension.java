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

package org.jboss.as.test.integration.domain.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Fake extension to use in testing extension management.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TestExtension implements Extension {

    public static final String MODULE_NAME = "org.jboss.as.test.extension";

    private final Parser parserOne = new Parser("urn:jboss:test:extension:1:1.0");
    private final Parser parserTwo = new Parser("urn:jboss:test:extension:2:1.0");


    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration one = context.registerSubsystem("1", 1, 1);
        one.registerXMLElementWriter(parserOne);
        one.registerSubsystemModel(new Subsystem("1"));

        SubsystemRegistration two = context.registerSubsystem("2", 2, 2);
        two.registerXMLElementWriter(parserTwo);
        two.registerSubsystemModel(new Subsystem("2"));
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping("1", parserOne.namespace, parserOne);
        context.setSubsystemXmlMapping("2", parserTwo.namespace, parserTwo);
    }


    private static class Subsystem implements DescriptionProvider {

        private final String name;

        private Subsystem(String name) {
            this.name = name;
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {

            ModelNode desc = new ModelNode();
            desc.get(DESCRIPTION).set("Subsystem " + name);
            ModelNode attr = desc.get(ATTRIBUTES, NAME);
            attr.get(DESCRIPTION).set("The name");
            attr.get(TYPE).set(ModelType.STRING);
            attr.get(NILLABLE).set(false);

            desc.get(OPERATIONS);

            desc.get(CHILDREN).setEmptyObject();

            return desc;
        }
    }

    private static class Parser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        private final String namespace;

        private Parser(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> value) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            ParseUtils.requireNoContent(reader);
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter streamWriter, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(namespace, false);
            streamWriter.writeEndElement();
        }
    }
}
