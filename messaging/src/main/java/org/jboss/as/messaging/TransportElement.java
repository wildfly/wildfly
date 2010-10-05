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

package org.jboss.as.messaging;

import java.util.Map;
import javax.xml.stream.XMLStreamException;
import static org.jboss.as.messaging.ElementUtils.writeSimpleElement;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author John Bailey
 */
public class TransportElement extends AbstractModelElement<TransportElement> {
     private static final long serialVersionUID = -1825725711314115049L;

    private final String name;
    private Map<String, Object> params;
    private String factoryClass;

    public TransportElement(String name) {
        this.name = name;
    }

    protected Class<TransportElement> getElementClass() {
        return TransportElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), getName());
        writeSimpleElement(Element.FACTORY_CLASS, getFactoryClassName(), streamWriter);
        Map<String, Object> params = getParams();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                streamWriter.writeEmptyElement(Element.PARAM.getLocalName());
                streamWriter.writeAttribute(Attribute.KEY.getLocalName(), entry.getKey());
                streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue().toString());
            }
        }
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(final Map<String, Object> params) {
        this.params = params;
    }

    public String getFactoryClassName() {
        return factoryClass;
    }

    public void setFactoryClassName(final String factoryClass) {
        this.factoryClass = factoryClass;
    }
}
