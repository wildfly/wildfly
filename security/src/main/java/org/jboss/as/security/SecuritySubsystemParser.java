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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.server.ExtensionContext;
import org.jboss.as.server.ExtensionContext.SubsystemConfiguration;
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

    static final String defaultAuthenticationManagerClassName = "org.jboss.security.plugins.auth.JaasSecurityManagerBase";

    static final String defaultCallbackHandlerClassName = "org.jboss.security.auth.callback.JBossCallbackHandler";

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
        if (authenticationManagerClassName == null)
            authenticationManagerClassName = SecuritySubsystemParser.defaultAuthenticationManagerClassName;
        if (defaultCallbackHandlerClassName == null)
            defaultCallbackHandlerClassName = SecuritySubsystemParser.defaultCallbackHandlerClassName;
        final SecuritySubsystemAdd subsystem = new SecuritySubsystemAdd(authenticationManagerClassName, deepCopySubjectMode,
                defaultCallbackHandlerClassName);

        // no sub elements yet
        ParseUtils.requireNoContent(reader);

        result.setResult(new ExtensionContext.SubsystemConfiguration<SecuritySubsystemElement>(subsystem, updates));
    }

}
