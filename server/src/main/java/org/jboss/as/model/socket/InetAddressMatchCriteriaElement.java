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

/**
 *
 */
package org.jboss.as.model.socket;

import java.net.NetworkInterface;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Indicates that a network interface must have a {@link NetworkInterface#getName() name}
 * that matches a given regular expression in order to match the criteria.
 *
 * @author Brian Stansberry
 */
public class InetAddressMatchCriteriaElement extends AbstractInterfaceCriteriaElement<InetAddressMatchCriteriaElement> {

    private static final long serialVersionUID = 52177844089594172L;

    private String address;

    /**
     * Creates a new InetAddressMatchCriteriaElement by parsing an xml stream
     *
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public InetAddressMatchCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(Element.INET_ADDRESS);

        address = ParseUtils.readStringAttributeElement(reader, Attribute.VALUE.getLocalName());
        setInterfaceCriteria(new InetAddressMatchInterfaceCriteria(address));
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), address);
        streamWriter.writeEndElement();
    }

    @Override
    protected Class<InetAddressMatchCriteriaElement> getElementClass() {
        return InetAddressMatchCriteriaElement.class;
    }

}
