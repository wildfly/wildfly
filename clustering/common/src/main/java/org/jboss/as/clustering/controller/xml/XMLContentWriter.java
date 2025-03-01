/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A writable model group of XML content.
 * @author Paul Ferraro
 * @param <C> the writer context.
 */
public interface XMLContentWriter<C> extends XMLElementWriter<C> {

    interface Provider<C> {
        XMLContentWriter<C> getWriter();
    }

    /**
     * Indicates whether the specified content is empty.
     */
    boolean isEmpty(C content);

    /**
     * Returns an empty content writer.
     * @param <C> the writer context.
     * @return an empty content writer.
     */
    static <C> XMLContentWriter<C> empty() {
        return new XMLContentWriter<>() {
            @Override
            public void writeContent(XMLExtendedStreamWriter streamWriter, C value) throws XMLStreamException {
                // Nothing to write
            }

            @Override
            public boolean isEmpty(C content) {
                return true;
            }
        };
    }
}
