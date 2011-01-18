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

package org.jboss.as.security;

import static org.jboss.as.model.ParseUtils.unexpectedElement;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.server.ExtensionContext;
import org.jboss.as.server.ExtensionContext.SubsystemConfiguration;
import org.jboss.security.config.parser.ApplicationPolicyParser;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The root element parser for the Security subsystem.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class SecuritySubsystemParser implements XMLStreamConstants,
        XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<SecuritySubsystemElement>>> {

    private static final SecuritySubsystemParser INSTANCE = new SecuritySubsystemParser();

    /**
     * Private constructor to create a singleton
     */
    private SecuritySubsystemParser() {
    }

    /**
     * Get the instance
     *
     * @return the instance
     */
    public static SecuritySubsystemParser getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, ParseResult<SubsystemConfiguration<SecuritySubsystemElement>> result)
            throws XMLStreamException {
        final List<AbstractSubsystemUpdate<SecuritySubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<SecuritySubsystemElement, ?>>();

        // no attributes
        ParseUtils.requireNoAttributes(reader);

        // read elements
        boolean securityManagementParsed = false;
        boolean subjectFactoryParsed = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case SECURITY_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SECURITY_MANAGEMENT: {
                            updates.add(parseSecurityManagement(reader));
                            securityManagementParsed = true;
                            break;
                        }
                        case SUBJECT_FACTORY: {
                            updates.add(parseSubjectFactory(reader));
                            subjectFactoryParsed = true;
                            break;
                        }
                        case JAAS: {
                            updates.add(parseJaas(reader));
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
        if (!securityManagementParsed)
            updates.add(new AddSecurityManagementUpdate("default", false, "default"));
        if (!subjectFactoryParsed)
            updates.add(new AddSubjectFactoryUpdate("default"));

        final SecuritySubsystemAdd subsystem = new SecuritySubsystemAdd();
        result.setResult(new ExtensionContext.SubsystemConfiguration<SecuritySubsystemElement>(subsystem, updates));
    }

    AddSecurityManagementUpdate parseSecurityManagement(XMLExtendedStreamReader reader) throws XMLStreamException {
        // read attributes
        String authenticationManagerClassName = null;
        boolean deepCopySubjectMode = false;
        String defaultCallbackHandlerClassName = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case AUTHENTICATION_MANAGER_CLASS_NAME: {
                        authenticationManagerClassName = value;
                        break;
                    }
                    case DEEP_COPY_SUBJECT_MODE: {
                        deepCopySubjectMode = Boolean.parseBoolean(value);
                        break;
                    }
                    case DEFAULT_CALLBACK_HANDLER_CLASS_NAME: {
                        defaultCallbackHandlerClassName = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);

        if (authenticationManagerClassName == null)
            authenticationManagerClassName = "default";
        if (defaultCallbackHandlerClassName == null)
            defaultCallbackHandlerClassName = "default";
        return new AddSecurityManagementUpdate(authenticationManagerClassName, deepCopySubjectMode,
                defaultCallbackHandlerClassName);
    }

    AddSubjectFactoryUpdate parseSubjectFactory(XMLExtendedStreamReader reader) throws XMLStreamException {
        // read attributes
        String subjectFactoryClassName = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SUBJECT_FACTORY_CLASS_NAME: {
                        subjectFactoryClassName = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);

        if (subjectFactoryClassName == null)
            subjectFactoryClassName = "default";
        return new AddSubjectFactoryUpdate(subjectFactoryClassName);
    }

    AddJaasUpdate parseJaas(XMLExtendedStreamReader reader) throws XMLStreamException {
        // no attributes
        ParseUtils.requireNoAttributes(reader);
        ApplicationPolicyParser parser = new ApplicationPolicyParser();
        AddJaasUpdate jaasUpdate = new AddJaasUpdate();
        jaasUpdate.setApplicationPolicies(parser.parse(reader));
        return jaasUpdate;
    }
}
