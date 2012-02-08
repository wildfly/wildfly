/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.config.assembly;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SubsystemParser extends NodeParser {

    private final String socketBindingNamespace;
    private final File inputFile;
    private final String supplementName;
    private String extensionModule;
    private Node subsystem;
    private final Map<String, ElementNode> socketBindings = new HashMap<String, ElementNode>();
    private final Map<String, ElementNode> outboundSocketBindings = new HashMap<String, ElementNode>();
    private final Map<String, ProcessingInstructionNode> supplementPlaceholders = new HashMap<String, ProcessingInstructionNode>();
    private final Map<String, Supplement> supplementReplacements = new HashMap<String, Supplement>();
    private final Map<String, AttributeValue> attributesForReplacement = new HashMap<String, AttributeValue>();

    SubsystemParser(String socketBindingNamespace, String supplementName, File inputFile){
        this.socketBindingNamespace = socketBindingNamespace;
        this.supplementName = supplementName;
        this.inputFile = inputFile;
    }

    String getExtensionModule() {
        return extensionModule;
    }

    Node getSubsystem() {
        return subsystem;
    }

    Map<String, ElementNode> getSocketBindings(){
        return socketBindings;
    }

    Map<String, ElementNode> getOutboundSocketBindings(){
        return outboundSocketBindings;
    }

    void parse() throws IOException, XMLStreamException {
        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            reader.require(START_DOCUMENT, null, null);
            Map<String, String> configAttributes = new HashMap<String, String>();
            configAttributes.put("default-supplement", null);
            ParsingUtils.getNextElement(reader, "config", configAttributes, false);
            extensionModule = ParsingUtils.getNextElement(reader, "extension-module", null, true);
            ParsingUtils.getNextElement(reader, "subsystem", null, false);
            subsystem = super.parseNode(reader, "subsystem");

            while (reader.hasNext()) {
                if (reader.next() == START_ELEMENT) {
                    if (reader.getLocalName().equals("subsystem")) {
                        throw new XMLStreamException("Only one subsystem element is allowed", reader.getLocation());
                    } else if (reader.getLocalName().equals("supplement")) {
                        parseSupplement(reader, ((ElementNode)subsystem).getNamespace());
                    } else if (reader.getLocalName().equals("socket-binding")) {
                        ElementNode socketBinding = new NodeParser(socketBindingNamespace).parseNode(reader, "socket-binding");
                        socketBindings.put(socketBinding.getAttributeValue("name"), socketBinding);
                    } else if (reader.getLocalName().equals("outbound-socket-binding")) {
                        ElementNode socketBinding = new NodeParser(socketBindingNamespace).parseNode(reader, "outbound-socket-binding");
                        outboundSocketBindings.put(socketBinding.getAttributeValue("name"), socketBinding);
                    }
                }
            }

            //Check for the default supplement name if no supplement is set
            String supplementName = this.supplementName;
            if (supplementName == null) {
                supplementName = configAttributes.get("default-supplement");
            }

            if (supplementName != null) {
                Supplement supplement = supplementReplacements.get(supplementName);
                if (supplement == null) {
                    throw new IllegalStateException("No supplement called '" + supplementName + "' could be found to augment the subsystem configuration");
                }
                Map<String, ElementNode> nodeReplacements = supplement.getAllNodeReplacements();
                for (Map.Entry<String, ProcessingInstructionNode> entry : supplementPlaceholders.entrySet()) {
                    ElementNode replacement = nodeReplacements.get(entry.getKey());
                    if (replacement != null) {
                        for (Iterator<Node> it = replacement.iterateChildren() ; it.hasNext() ; ) {
                            entry.getValue().addDelegate(it.next());
                        }
                    }
                }

                Map<String, String> attributeReplacements = supplement.getAllAttributeReplacements();
                for (Map.Entry<String, AttributeValue> entry : attributesForReplacement.entrySet()) {
                    String replacement = attributeReplacements.get(entry.getKey());
                    if (replacement == null) {
                        throw new IllegalStateException("No replacement found for " + entry.getKey() + " in supplement " + supplementName);
                    }
                    entry.getValue().setValue(replacement);
                }
            }

        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
            }
        }
    }

    protected void parseSupplement(XMLStreamReader reader, String subsystemNs) throws XMLStreamException {
        String name = null;
        String[] includes = null;
        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
            String attr = reader.getAttributeLocalName(i);
            if (attr.equals("name")) {
                name = reader.getAttributeValue(i);
            } else if (attr.equals("includes")){
                String tmp = reader.getAttributeValue(i);
                includes = tmp.split(" ");
            } else {
                throw new XMLStreamException("Invalid attribute " + attr, reader.getLocation());
            }
        }
        if (name == null) {
            throw new XMLStreamException("Missing required attribute 'name'", reader.getLocation());
        }
        if (name.length() == 0) {
            throw new XMLStreamException("Empty name attribute for <supplement>", reader.getLocation());
        }

        Supplement supplement = new Supplement(includes);
        if (supplementReplacements.put(name, supplement) != null) {
            throw new XMLStreamException("Already have a supplement called " + name, reader.getLocation());
        }

        while (reader.hasNext()) {
            reader.next();
            int type = reader.getEventType();
            switch (type) {
            case START_ELEMENT:
                if (reader.getLocalName().equals("replacement")) {
                    parseSupplementReplacement(reader, subsystemNs, supplement);
                } else {
                    throw new XMLStreamException("Unknown element " + reader.getLocalName(), reader.getLocation());
                }
                break;
            case END_ELEMENT:
                if (reader.getLocalName().equals("supplement")){
                    return;
                } else {
                    throw new XMLStreamException("Unknown element " + reader.getLocalName(), reader.getLocation());
                }
            }
        }
    }

    protected void parseSupplementReplacement(XMLStreamReader reader, String subsystemNs, Supplement supplement) throws XMLStreamException {
        String placeholder = null;
        String attributeValue = null;
        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
            String attr = reader.getAttributeLocalName(i);
            if (attr.equals("placeholder")) {
                placeholder = reader.getAttributeValue(i);
            } else if (attr.equals("attributeValue")) {
                attributeValue = reader.getAttributeValue(i);
            }else {
                throw new XMLStreamException("Invalid attribute " + attr, reader.getLocation());
            }
        }
        if (placeholder == null) {
            throw new XMLStreamException("Missing required attribute 'placeholder'", reader.getLocation());
        }
        if (placeholder.length() == 0) {
            throw new XMLStreamException("Empty placeholder attribute for <replacement>", reader.getLocation());
        }

        if (attributeValue != null) {
            supplement.addAttributeReplacement(placeholder, attributeValue);
        }

        while (reader.hasNext()) {
            int type = reader.getEventType();
            switch (type) {
            case START_ELEMENT:
                ElementNode node = new NodeParser(subsystemNs).parseNode(reader, reader.getLocalName());
                if (attributeValue != null && node.iterateChildren().hasNext()) {
                    throw new XMLStreamException("Can not have nested content when attributeValue is used", reader.getLocation());
                }
                if (supplement.addNodeReplacement(placeholder, node) != null) {
                    throw new XMLStreamException("Already have a replacement called " + placeholder + " in supplement", reader.getLocation());
                }
                break;
            case END_ELEMENT:
                if (reader.getLocalName().equals("replacement")){
                    return;
                } else {
                    throw new XMLStreamException("Unknown element " + reader.getLocalName(), reader.getLocation());
                }
            }
        }
    }

    @Override
    protected ProcessingInstructionNode parseProcessingInstruction(XMLStreamReader reader, ElementNode parent) throws XMLStreamException {
        String name = reader.getPITarget();
        ProcessingInstructionNode placeholder = new ProcessingInstructionNode(name, parseProcessingInstructionData(reader.getPIData()));
        if (supplementPlaceholders.put(name, placeholder) != null) {
            throw new IllegalStateException("Already have a processing instruction called <?" + name + "?>");
        }
        return placeholder;
    }


    protected AttributeValue createAttributeValue(String attributeValue) {
        AttributeValue value = super.createAttributeValue(attributeValue);
        if (attributeValue.startsWith("@@")) {
            attributesForReplacement.put(attributeValue, value);
        }
        return value;
    }

    private class Supplement {
        final String[] includes;
        final Map<String, ElementNode> nodeReplacements = new HashMap<String, ElementNode>();
        final Map<String, String> attributeReplacements = new HashMap<String, String>();

        Supplement(String[] includes){
            this.includes = includes;
        }

        ElementNode addNodeReplacement(String placeholder, ElementNode replacement) {
            return nodeReplacements.put(placeholder, replacement);
        }

        String addAttributeReplacement(String placeholder, String replacement) {
            return attributeReplacements.put(placeholder, replacement);
        }

        Map<String, ElementNode> getAllNodeReplacements() {
            Map<String, ElementNode> result = new HashMap<String, ElementNode>();
            getAllNodeReplacements(result);
            return result;
        }

        void getAllNodeReplacements(Map<String, ElementNode> result) {
            if (includes != null && includes.length > 0) {
                for (String include : includes) {
                    Supplement parent = supplementReplacements.get(include);
                    if (parent == null) {
                        throw new IllegalStateException("Can't find included supplement '" + include + "'");
                    }
                    parent.getAllNodeReplacements(result);
                }
            }
            result.putAll(nodeReplacements);
        }

        Map<String, String> getAllAttributeReplacements() {
            Map<String, String> result = new HashMap<String, String>();
            getAllAttributeReplacements(result);
            return result;
        }

        void getAllAttributeReplacements(Map<String, String> result) {
            if (includes != null && includes.length > 0) {
                for (String include : includes) {
                    Supplement parent = supplementReplacements.get(include);
                    if (parent == null) {
                        throw new IllegalStateException("Can't find included supplement '" + include + "'");
                    }
                    parent.getAllAttributeReplacements(result);
                }
            }
            result.putAll(attributeReplacements);
        }
}
}
