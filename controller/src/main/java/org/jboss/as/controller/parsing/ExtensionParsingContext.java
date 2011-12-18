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

import java.util.List;

import org.jboss.as.controller.Extension;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Context in effect when the {@code extension} element for a given {@link Extension} is being parsed. Allows the
 * extension to {@link Extension#initializeParsers(ExtensionParsingContext) initialize the XML parsers} that can
 * be used for parsing the {@code subsystem} elements that contain the configuration for its subsystems.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ExtensionParsingContext {

    /**
     * Set the parser for the profile-wide subsystem configuration XML element.  The element is always
     * called {@code "subsystem"}.  The reader should populate the given model node with the appropriate
     * "subsystem add" update, without the address or operation name as that information will be automatically
     * populated.
     *
     * @param namespaceUri the URI of the susbsystem's XML namespace, in string form. Cannot be {@code null}
     * @param reader the element reader. Cannot be {@code null}
     *
     * @deprecated use {@link #setSubsystemXmlMapping(String, String, XMLElementReader)}
     */
    @Deprecated
    void setSubsystemXmlMapping(String namespaceUri, XMLElementReader<List<ModelNode>> reader);

    /**
     * Set the parser for the profile-wide subsystem configuration XML element.  The element is always
     * called {@code "subsystem"}.  The reader should populate the given model node with the appropriate
     * "subsystem add" update, without the address or operation name as that information will be automatically
     * populated.
     *
     * @param subsystemName the name of the subsystem. Cannot be {@code null}
     * @param namespaceUri the URI of the susbsystem's XML namespace, in string form. Cannot be {@code null}
     * @param reader the element reader. Cannot be {@code null}
     *
     * @throws IllegalStateException if another {@link Extension} has already registered a subsystem with the given
     *                               {@code subsystemName}
     */
    void setSubsystemXmlMapping(String subsystemName, String namespaceUri, XMLElementReader<List<ModelNode>> reader);

    /**
     * Set the parser for the per-deployment configuration for this element, if any.
     *
     * @param namespaceUri the URI of the susbsystem's XML namespace, in string form. Cannot be {@code null}
     * @param reader the element reader. Cannot be {@code null}
     *
     * @throws IllegalStateException if another {@link Extension} has already registered a subsystem with the given
     *                               {@code subsystemName}
     *
     * @deprecated use {@link #setSubsystemXmlMapping(String, String, XMLElementReader)}
     */
    @Deprecated
    void setDeploymentXmlMapping(String namespaceUri, XMLElementReader<ModelNode> reader);

    /**
     * Set the parser for the per-deployment configuration for this element, if any.
     * <p>
     * <strong>Note that this method is not currently implemented.</strong>
     * </p>
     *
     * (TODO: round this out or remove it.)
     *
     * @param subsystemName the name of the subsystem. Cannot be {@code null}
     * @param namespaceUri the URI of the susbsystem's XML namespace, in string form. Cannot be {@code null}
     * @param reader the element reader. Cannot be {@code null}
     *
     * @throws IllegalStateException if another {@link Extension} has already registered a subsystem with the given
     *                               {@code subsystemName}
     *
     * @deprecated currently not used and will be removed in a future release if not used.
     */
    @Deprecated
    void setDeploymentXmlMapping(String subsystemName, String namespaceUri, XMLElementReader<ModelNode> reader);

}
