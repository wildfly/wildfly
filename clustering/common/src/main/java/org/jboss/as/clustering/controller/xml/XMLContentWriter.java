/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Feature;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A writable model group of XML content.
 * @author Paul Ferraro
 */
public interface XMLContentWriter<C> extends XMLElementWriter<C>, Feature {

    /**
     * Indicates whether the specified content is empty.
     */
    boolean isEmpty(C content);

    static <C> XMLContentWriter<C> empty() {
        return of(List.of());
    }

    static <C> XMLContentWriter<C> of(Collection<? extends XMLContentWriter<C>> writers) {
        return new DefaultXMLContentWriter<>(writers);
    }

    class DefaultXMLContentWriter<C> implements XMLContentWriter<C> {
        private final Collection<? extends XMLContentWriter<C>> contentWriters;

        DefaultXMLContentWriter(Collection<? extends XMLContentWriter<C>> contentWriters) {
            this.contentWriters = contentWriters;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, C content) throws XMLStreamException {
            for (XMLContentWriter<C> contentWriter : this.contentWriters) {
                if (!contentWriter.isEmpty(content)) {
                    contentWriter.writeContent(writer, content);
                }
            }
        }

        @Override
        public boolean isEmpty(C content) {
            for (XMLContentWriter<C> contentWriter : this.contentWriters) {
                if (!contentWriter.isEmpty(content)) return false;
            }
            return true;
        }

        @Override
        public Stability getStability() {
            Stability stability = null;
            for (XMLContentWriter<C> contentWriter : this.contentWriters) {
                if (stability == null || stability.enables(contentWriter.getStability())) {
                    stability = contentWriter.getStability();
                }
            }
            return (stability != null) ? stability : Stability.DEFAULT;
        }
    }
}
