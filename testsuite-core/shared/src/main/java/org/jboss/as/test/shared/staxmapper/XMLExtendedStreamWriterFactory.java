/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.shared.staxmapper;

import java.lang.reflect.Constructor;
import javax.xml.stream.XMLStreamWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Expose the package protected {@link org.jboss.staxmapper.FormattingXMLStreamWriter} to tests.
 * @author Paul Ferraro
 */
public class XMLExtendedStreamWriterFactory {
    public static XMLExtendedStreamWriter create(XMLStreamWriter writer) throws Exception {
        // Use reflection to access package protected class FormattingXMLStreamWriter
        // TODO: at some point the staxmapper API could be enhanced to make this unnecessary
        Class clazz = Class.forName("org.jboss.staxmapper.FormattingXMLStreamWriter");
        Object [] args = new Object [1];
        args[0] = writer;
        Constructor ctr = clazz.getConstructor( XMLStreamWriter.class );
        ctr.setAccessible(true);
        return (XMLExtendedStreamWriter)ctr.newInstance(args);
    }
}