/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.io;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class IOSubsystemParser_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    static final IOSubsystemParser_1_0 INSTANCE = new IOSubsystemParser_1_0();

    private final PersistentResourceXMLDescription xmlDescription;

    private IOSubsystemParser_1_0() {
        xmlDescription = builder(IORootDefinition.INSTANCE)
                .addChild(
                        builder(WorkerResourceDefinition.INSTANCE)
                                .addAttribute(WorkerResourceDefinition.WORKER_IO_THREADS, new AttributeParser.DiscardOldDefaultValueParser("3"))
                                .addAttributes(
                                        WorkerResourceDefinition.WORKER_TASK_KEEPALIVE,
                                        WorkerResourceDefinition.WORKER_TASK_MAX_THREADS,
                                        WorkerResourceDefinition.STACK_SIZE)
                )
                .addChild(
                        builder(BufferPoolResourceDefinition.INSTANCE)
                                .addAttribute(BufferPoolResourceDefinition.BUFFER_SIZE, new AttributeParser.DiscardOldDefaultValueParser("16384"))
                                .addAttribute(BufferPoolResourceDefinition.BUFFER_PER_SLICE, new AttributeParser.DiscardOldDefaultValueParser("128"))
                                .addAttribute(BufferPoolResourceDefinition.DIRECT_BUFFERS)
                )
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        xmlDescription.parse(reader, PathAddress.EMPTY_ADDRESS, list);
    }
}

