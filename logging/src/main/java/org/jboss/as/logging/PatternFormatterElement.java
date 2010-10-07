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

package org.jboss.as.logging;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.logging.Formatter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PatternFormatterElement extends AbstractFormatterElement<PatternFormatterElement> {

    private static final long serialVersionUID = 2260770166892253338L;

    private static final QName ELEMENT_NAME = new QName(Namespace.CURRENT.getUriString(), Element.PATTERN_FORMATTER.getLocalName());

    private final String pattern;

    protected PatternFormatterElement(final String pattern) {
        super(ELEMENT_NAME);
        if (pattern == null) {
            throw new IllegalArgumentException("pattern is null");
        }
        this.pattern = pattern;
    }

    protected Formatter createFormatter() {
        return new PatternFormatter(pattern);
    }

    protected Class<PatternFormatterElement> getElementClass() {
        return PatternFormatterElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute("pattern", pattern);
        streamWriter.writeEndElement();
    }

    public AbstractFormatterSpec getSpecification() {
        return new PatternFormatterSpec(pattern);
    }
}
