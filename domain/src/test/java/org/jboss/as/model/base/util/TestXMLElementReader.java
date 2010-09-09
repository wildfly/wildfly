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

package org.jboss.as.model.base.util;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Flexible implementation of {@link XMLElementReader} designed for use in
 * unit tests. Takes a {@link ReadElementCallback} in the constructor and uses
 * it to create the desired domain model object.
 *
 * @author Brian Stansberry
 */
public class TestXMLElementReader<T extends AbstractModelElement<T>> implements XMLElementReader<ParseResult<T>> {

    private final ReadElementCallback<T> callback;

    /**
     * Uses a {@link SimpleReadElementCallback} as the callback.
     *
     * @param clazz the class to base the callback on
     *
     * @throws NoSuchMethodException if <code>clazz</code> does not expose an
     *          appropriate constructor
     *
     * @see SimpleReadElementCallback#getCallback(Class)
     */
    public TestXMLElementReader(Class<T> clazz) throws NoSuchMethodException {
        this(SimpleReadElementCallback.getCallback(clazz));
    }

    /**
     * Creates a TestXMLElementReader that uses the given callback.
     *
     * @param callback the callback
     */
    public TestXMLElementReader(ReadElementCallback<T> callback) {
        this.callback = callback;
    }

    /**
     * Passes the <code>reader</code> to the {@link ReadElementCallback#readElement(XMLExtendedStreamReader) callback}.
     *
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, ParseResult<T> parseResult) throws XMLStreamException {
        parseResult.setResult(callback.readElement(reader));
    }
}
