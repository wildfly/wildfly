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

package org.jboss.as.model;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

/**
 * A parser which can be sent in to {@link XMLMapper#registerRootElement(QName, XMLElementReader)}
 * for {@code &lt;domain&gt;} root elements.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainParser implements XMLElementReader<ParseResult<Domain>>, XMLStreamConstants {

    private DomainParser() {
    }

    private static final DomainParser INSTANCE = new DomainParser();

    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static DomainParser getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<Domain> value) throws XMLStreamException {
        value.setResult(new Domain(reader));
    }
}
