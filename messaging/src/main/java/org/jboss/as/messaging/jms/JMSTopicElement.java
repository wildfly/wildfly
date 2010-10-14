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

package org.jboss.as.messaging.jms;

import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The topic configuration element.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSTopicElement extends AbstractModelElement<JMSTopicElement> {

    private static final long serialVersionUID = 7877459701976545141L;
    private final String name;
    private Set<String> bindings;

    public JMSTopicElement(final String name) {
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }
        this.name = name;
    }

    public Set<String> getBindings() {
        return bindings;
    }

    public void setBindings(Set<String> bindings) {
        this.bindings = bindings;
    }

    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    protected Class<JMSTopicElement> getElementClass() {
        return JMSTopicElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        if(bindings != null && bindings.size() > 0) {
            for(final String binding : bindings) {
                streamWriter.writeEmptyElement(Element.ENTRY.getLocalName());
                streamWriter.writeAttribute(Attribute.NAME.getLocalName(), binding);
            }
        }
        streamWriter.writeEndElement();
    }

}
