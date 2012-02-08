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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ElementNode extends Node {

    private final ElementNode parent;
    private final String name;
    private final String namespace;
    private final Map<String, AttributeValue> attributes = new LinkedHashMap<String, AttributeValue>();
    private List<Node> children = new ArrayList<Node>();

    ElementNode(final ElementNode parent, final String name, final String namespace) {
        this.parent = parent;
        this.name = name;
        this.namespace = namespace == null ?
                namespace : namespace.isEmpty() ?
                        null : namespace;
    }

    String getNamespace() {
        return namespace;
    }

    String getName() {
        return name;
    }

    void addAttribute(String name, AttributeValue value) {
        attributes.put(name, value);
    }

    void addChild(Node child) {
        children.add(child);
    }

    Iterator<Node> getChildren() {
        return children.iterator();
    }

    ElementNode getParent() {
        return parent;
    }

    Iterator<Node> iterateChildren(){
        return children.iterator();
    }

    String getAttributeValue(String name) {
        AttributeValue av = attributes.get(name);
        if (av == null) {
            return null;
        }
        return av.getValue();
    }

    String getAttributeValue(String name, String defaultValue) {
        String s = getAttributeValue(name);
        if (s == null) {
            return defaultValue;
        }
        return s;
    }

    @Override
    void marshall(XMLStreamWriter writer) throws XMLStreamException {
//        boolean empty = false;//children.isEmpty()
        boolean empty = isEmpty();
        NamespaceContext context = writer.getNamespaceContext();
        String prefix = writer.getNamespaceContext().getPrefix(namespace);
        if (prefix == null) {
            // Unknown namespace; it becomes default
            writer.setDefaultNamespace(namespace);
            if (empty) {
                writer.writeEmptyElement(name);
            }
            else {
                writer.writeStartElement(name);
            }
            writer.writeNamespace(null, namespace);
        }
        else {
            if (empty) {
                writer.writeEmptyElement(namespace, name);
            }
            else {
                writer.writeStartElement(namespace, name);
            }
        }

        for (Map.Entry<String, AttributeValue> attr : attributes.entrySet()) {
            writer.writeAttribute(attr.getKey(), attr.getValue().getValue());
        }

        for (Node child : children) {
            child.marshall(writer);
        }

        if (!empty) {
            try {
                writer.writeEndElement();
            } catch(XMLStreamException e) {
                //TODO REMOVE THIS
                throw e;
            }
        }
    }

    private boolean isEmpty() {
        if (children.isEmpty()) {
            return true;
        }
        for (Node child : children) {
            if (child.hasContent()) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "Element(name=" + name + ",ns=" + namespace + ")";
    }
}
