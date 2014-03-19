/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.metadata;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
class PatchXml_1_0 extends PatchXmlUtils implements XMLStreamConstants, XMLElementReader<PatchXml.Result<PatchMetadataResolver>>, XMLElementWriter<Patch> {

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final Patch patch) throws XMLStreamException {

        // Get started ...
        writer.writeStartDocument();
        writer.writeStartElement(Element.PATCH.name);
        writer.writeDefaultNamespace(PatchXml.Namespace.PATCH_1_0.getNamespace());

        writePatch(writer, patch);

        // Done
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, PatchXml.Result<PatchMetadataResolver> factory) throws XMLStreamException {
        final PatchBuilder builder = new PatchBuilder();
        doReadElement(reader, builder, factory.getOriginalIdentity());
        factory.setResult(builder);
    }

}
