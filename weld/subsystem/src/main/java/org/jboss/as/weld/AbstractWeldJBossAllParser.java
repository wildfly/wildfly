/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamReader;

abstract class AbstractWeldJBossAllParser {

    protected Boolean getAttributeAsBoolean(XMLExtendedStreamReader reader, String attributeName) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final String name = reader.getAttributeLocalName(i);
            final String value = reader.getAttributeValue(i);
            if (attributeName.equals(name)) {
                return Boolean.valueOf(value);
            }
        }
        return null;
    }

    protected void parseWeldElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != javax.xml.stream.XMLStreamConstants.END_ELEMENT) {
        }
    }
}
