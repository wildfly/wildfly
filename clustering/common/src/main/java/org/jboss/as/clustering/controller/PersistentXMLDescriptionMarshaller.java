/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.List;
import java.util.function.Supplier;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A marshaller based on a {@link PersistentResourceXMLDescription} that is both an {@link XMLElementReader} and {@link XMLElementWriter}.
 * @author Paul Ferraro
 */
public class PersistentXMLDescriptionMarshaller extends PersistentXMLDescriptionReader implements XMLElementWriter<SubsystemMarshallingContext>, Supplier<XMLElementReader<List<ModelNode>>> {
    private final PersistentResourceXMLDescription description;

    public <S extends PersistentSubsystemSchema<S>> PersistentXMLDescriptionMarshaller(PersistentSubsystemSchema<S> schema) {
        this(schema.getXMLDescription());
    }

    public PersistentXMLDescriptionMarshaller(PersistentResourceXMLDescription description) {
        super(description);
        this.description = description;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(this.description.getPathElement().getKeyValuePair()).set(context.getModelNode());
        this.description.persist(writer, model);
    }

    @Override
    public XMLElementReader<List<ModelNode>> get() {
        return this;
    }
}
