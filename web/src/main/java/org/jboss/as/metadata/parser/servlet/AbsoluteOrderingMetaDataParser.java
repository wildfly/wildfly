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

package org.jboss.as.metadata.parser.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.web.spec.AbsoluteOrderingMetaData;
import org.jboss.metadata.web.spec.NameMetaData;
import org.jboss.metadata.web.spec.OrderingElementMetaData;
import org.jboss.metadata.web.spec.OthersMetaData;

/**
 * @author Remy Maucherat
 */
public class AbsoluteOrderingMetaDataParser extends MetaDataElementParser {

    public static AbsoluteOrderingMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        AbsoluteOrderingMetaData ordering = new AbsoluteOrderingMetaData();

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NAME:
                    List<OrderingElementMetaData> order = ordering.getOrdering();
                    if (order == null) {
                        order = new ArrayList<OrderingElementMetaData>();
                        ordering.setOrdering(order);
                    }
                    NameMetaData name = new NameMetaData();
                    name.setName(reader.getElementText());
                    order.add(name);
                    break;
                case OTHERS:
                    List<OrderingElementMetaData> order2 = ordering.getOrdering();
                    if (order2 == null) {
                        order2 = new ArrayList<OrderingElementMetaData>();
                        ordering.setOrdering(order2);
                    }
                    OthersMetaData others = new OthersMetaData();
                    order2.add(others);
                    requireNoContent(reader);
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return ordering;
    }

}
