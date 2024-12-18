/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A readable model group of XML content.
 */
public interface XMLContentReader<C> extends XMLElementReader<C> {

    /**
     * Returns the cardinality of this model group.
     * @return the cardinality of this model group.
     */
    XMLCardinality getCardinality();

    static <C> XMLContentReader<C> empty() {
        return new XMLContentReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, C value) throws XMLStreamException {
                ParseUtils.requireNoContent(reader);
            }

            @Override
            public XMLCardinality getCardinality() {
                return XMLCardinality.NONE;
            }
        };
    }
}
