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
package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.parsing.ProfileParsingCompletionHandler;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.model.test.ModelTestParser;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.junit.Assert;

final class TestParser implements  ModelTestParser {

    final ExtensionRegistry extensionRegistry;
    final String mainSubsystemName;

    TestParser(String mainSubsystemName, ExtensionRegistry extensionRegistry) {
        this.mainSubsystemName = mainSubsystemName;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, ModelMarshallingContext context) throws XMLStreamException {

        String defaultNamespace = writer.getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
        try {
            ModelNode subsystem = context.getModelNode().get(SUBSYSTEM, mainSubsystemName);
            if (subsystem.isDefined()) {
                //We might have been removed
                XMLElementWriter<SubsystemMarshallingContext> subsystemWriter = context.getSubsystemWriter(mainSubsystemName);
                if (subsystemWriter != null) {
                    subsystemWriter.writeContent(writer, new SubsystemMarshallingContext(subsystem, writer));
                }
            }
        }catch (Throwable t){
            Assert.fail("could not marshal subsystem xml: "+t.getMessage()+":\n"+ Arrays.toString(t.getStackTrace()).replaceAll(", ","\n"));
        } finally {
            writer.setDefaultNamespace(defaultNamespace);
        }
        writer.writeEndDocument();
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ParseUtils.requireNoAttributes(reader);
        final Map<String, List<ModelNode>> profileOps = new LinkedHashMap<String, List<ModelNode>>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.UNKNOWN) {
                throw unexpectedElement(reader);
            }
            if (Element.forName(reader.getLocalName()) != Element.SUBSYSTEM) {
                throw unexpectedElement(reader);
            }
            String namespace = reader.getNamespaceURI();
            if (profileOps.containsKey(namespace)) {
                throw ControllerMessages.MESSAGES.duplicateDeclaration("subsystem", reader.getLocation());
            }
            // parse subsystem
            final List<ModelNode> subsystems = new ArrayList<ModelNode>();
            reader.handleAny(subsystems);

            profileOps.put(namespace, subsystems);
        }

        // Let extensions modify the profile
        Set<ProfileParsingCompletionHandler> completionHandlers = extensionRegistry.getProfileParsingCompletionHandlers();
        for (ProfileParsingCompletionHandler completionHandler : completionHandlers) {
            completionHandler.handleProfileParsingCompletion(profileOps, operations);
        }

        for (List<ModelNode> subsystems : profileOps.values()) {
            operations.addAll(subsystems);
        }
    }
}