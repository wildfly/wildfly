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

package org.jboss.as.controller.parsing;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;

import javax.xml.namespace.QName;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLMapper;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ExtensionParsingContextImpl implements ExtensionParsingContext {
    private final XMLMapper xmlMapper;

    public ExtensionParsingContextImpl(final XMLMapper xmlMapper) {
        this.xmlMapper = xmlMapper;
    }

    /**
     * Set the parser for the profile-wide subsystem configuration XML element.  The element is always called {@code
     * "subsystem"}.  The reader should populate the given model node with the appropriate "subsystem add" update, without
     * the address or operation name as that information will be automatically populated.
     *
     * @param reader the element reader
     */
    @Override
    public void setSubsystemXmlMapping(final String namespaceUri, final XMLElementReader<List<ModelNode>> reader) {
        xmlMapper.registerRootElement(new QName(namespaceUri, SUBSYSTEM), reader);
    }

    /**
     * Set the parser for the per-deployment configuration for this element, if any.
     * <p/>
     * (TODO: round this out.)
     *
     * @param reader the element reader
     */
    @Override
    public void setDeploymentXmlMapping(final String namespaceUri, final XMLElementReader<ModelNode> reader) {
    }
}
