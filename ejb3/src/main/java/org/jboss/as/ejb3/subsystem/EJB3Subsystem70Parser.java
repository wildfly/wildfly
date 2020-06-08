/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;

import java.util.EnumSet;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for ejb3:7.0 namespace.
 */
public class EJB3Subsystem70Parser extends EJB3Subsystem60Parser {

    EJB3Subsystem70Parser() {
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_7_0;
    }

    @Override
    protected void parseStatefulBean(final XMLExtendedStreamReader reader, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT, EJB3SubsystemXMLAttribute.CACHE_REF);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_ACCESS_TIMEOUT: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case DEFAULT_SESSION_TIMEOUT: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case CACHE_REF: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case CLUSTERED_CACHE_REF: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                case PASSIVATION_DISABLED_CACHE_REF: {
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
            // found the mandatory attribute
            missingRequiredAttributes.remove(attribute);
        }
        requireNoContent(reader);
        if (!missingRequiredAttributes.isEmpty()) {
            throw missingRequired(reader, missingRequiredAttributes);
        }
    }
}
