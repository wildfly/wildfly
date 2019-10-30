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

package org.jboss.as.ee.subsystem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
*/
class EESubsystemParser10 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    EESubsystemParser10() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // EE subsystem doesn't have any attributes, so make sure that the xml doesn't have any
        requireNoAttributes(reader);

        final ModelNode eeSubSystem = Util.createAddOperation(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM));
        // add the subsystem to the ModelNode(s)
        list.add(eeSubSystem);

        // elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case EE_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case GLOBAL_MODULES: {
                            final ModelNode model = parseGlobalModules(reader);
                            eeSubSystem.get(GlobalModulesDefinition.GLOBAL_MODULES).set(model);
                            break;
                        }
                        case EAR_SUBDEPLOYMENTS_ISOLATED: {
                            final String earSubDeploymentsIsolated = parseEarSubDeploymentsIsolatedElement(reader);
                            // set the ear subdeployment isolation on the subsystem operation
                            EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.parseAndSetParameter(earSubDeploymentsIsolated, eeSubSystem, reader);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    static ModelNode parseGlobalModules(XMLExtendedStreamReader reader) throws XMLStreamException {

        ModelNode globalModules = new ModelNode();

        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case MODULE: {
                    final ModelNode module = new ModelNode();
                    final int count = reader.getAttributeCount();
                    String name = null;
                    String slot = null;
                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case NAME:
                                if (name != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                name = value;
                                GlobalModulesDefinition.NAME_AD.parseAndSetParameter(name, module, reader);
                                break;
                            case SLOT:
                                if (slot != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                slot = value;
                                GlobalModulesDefinition.SLOT_AD.parseAndSetParameter(slot, module, reader);
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    if (name == null) {
                        throw missingRequired(reader, Collections.singleton(NAME));
                    }

                    globalModules.add(module);

                    requireNoContent(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return globalModules;
    }

    static String parseEarSubDeploymentsIsolatedElement(XMLExtendedStreamReader reader) throws XMLStreamException {

        // we don't expect any attributes for this element.
        requireNoAttributes(reader);

        final String value = reader.getElementText();
        if (value == null || value.trim().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.invalidValue(value, Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName(), reader.getLocation());
        }
        return value.trim();
    }
}
