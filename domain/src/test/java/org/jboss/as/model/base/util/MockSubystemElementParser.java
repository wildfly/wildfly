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

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.server.legacy.ExtensionContext.SubsystemConfiguration;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

/**
 */
public final class MockSubystemElementParser implements XMLElementReader<ParseResult<SubsystemConfiguration<MockSubsystemElement>>>, XMLStreamConstants {

    public static void registerXMLElementReaders(XMLMapper mapper) {
        mapper.registerRootElement(new QName(MockSubsystemElement.NAMESPACE, MockSubsystemElement.SUBSYSTEM), new MockSubystemElementParser(MockSubsystemElement.NAMESPACE));
        mapper.registerRootElement(new QName(MockSubsystemElement.ANOTHER_NAMESPACE, MockSubsystemElement.SUBSYSTEM), new MockSubystemElementParser(MockSubsystemElement.ANOTHER_NAMESPACE));
    }

    private final String namespaceURI;

    private MockSubystemElementParser(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    /** {@inheritDoc} */
    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<SubsystemConfiguration<MockSubsystemElement>> updates) throws XMLStreamException {
        ParseUtils.requireNoContent(reader);
        List<AbstractSubsystemUpdate<MockSubsystemElement, ?>> list = Collections.emptyList();
        SubsystemConfiguration<MockSubsystemElement> result = new SubsystemConfiguration<MockSubsystemElement>(new MockSubsystemAdd(), list);
        updates.setResult(result);
    }

    private class MockSubsystemAdd extends AbstractSubsystemAdd<MockSubsystemElement> {

        private static final long serialVersionUID = 1L;

        MockSubsystemAdd() {
            super(namespaceURI);
        }
        @Override
        protected <P> void applyUpdate(UpdateContext updateContext,
                UpdateResultHandler<? super Void, P> resultHandler, P param) {
            // no-op
        }

        @Override
        protected MockSubsystemElement createSubsystemElement() {
            return new MockSubsystemElement(namespaceURI);
        }

    }
}
