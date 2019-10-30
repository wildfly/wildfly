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

package org.jboss.as.ejb3.interceptor;

import org.jboss.metadata.ejb.parser.spec.AbstractMetaDataParser;
import org.jboss.metadata.ejb.parser.spec.InterceptorBindingMetaDataParser;
import org.jboss.metadata.ejb.spec.InterceptorBindingMetaData;
import org.jboss.metadata.property.PropertyReplacer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Responsible for parsing the <code>container-interceptors</code> in jboss-ejb3.xml
 *
 * @author Jaikiran Pai
 */
public class ContainerInterceptorsParser extends AbstractMetaDataParser<ContainerInterceptorsMetaData> {

    public static final ContainerInterceptorsParser INSTANCE = new ContainerInterceptorsParser();
    public static final String NAMESPACE_URI_1_0 = "urn:container-interceptors:1.0";

    private static final String ELEMENT_CONTAINER_INTERCEPTORS = "container-interceptors";
    private static final String ELEMENT_INTERCEPTOR_BINDING = "interceptor-binding";

    @Override
    public ContainerInterceptorsMetaData parse(XMLStreamReader reader, PropertyReplacer propertyReplacer) throws XMLStreamException {
        // make sure it's the right namespace
        if (!reader.getNamespaceURI().equals(NAMESPACE_URI_1_0)) {
            throw unexpectedElement(reader);
        }
        // only process relevant elements
        if (!reader.getLocalName().equals(ELEMENT_CONTAINER_INTERCEPTORS)) {
            throw unexpectedElement(reader);
        }

        final ContainerInterceptorsMetaData metaData = new ContainerInterceptorsMetaData();
        processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(final ContainerInterceptorsMetaData containerInterceptorsMetadata, final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String localName = reader.getLocalName();
        if (!localName.equals(ELEMENT_INTERCEPTOR_BINDING)) {
            throw unexpectedElement(reader);
        }
        // parse the interceptor-binding
        final InterceptorBindingMetaData interceptorBinding = this.readInterceptorBinding(reader, propertyReplacer);
        // add the interceptor binding to the container interceptor metadata
        containerInterceptorsMetadata.addInterceptorBinding(interceptorBinding);
    }

    /**
     * Parses the <code>interceptor-binding</code> element and returns the corresponding {@link InterceptorBindingMetaData}
     *
     * @param reader
     * @param propertyReplacer
     * @return
     * @throws XMLStreamException
     */
    private InterceptorBindingMetaData readInterceptorBinding(final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        return InterceptorBindingMetaDataParser.INSTANCE.parse(reader, propertyReplacer);
    }
}
