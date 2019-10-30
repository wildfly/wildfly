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

package org.jboss.as.connector.deployers.ds;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.api.ds.DsSecurity;
import org.jboss.as.connector.metadata.common.CredentialImpl;
import org.jboss.as.connector.metadata.ds.DsSecurityImpl;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.ParserException;
import org.jboss.jca.common.metadata.ds.DsParser;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.metadata.property.PropertyResolver;

/**
 * Parser for -ds.xml
 *
 */
public class DsXmlParser extends DsParser {


    private final PropertyResolver propertyResolver;
    private final PropertyReplacer propertyReplacer;

    public DsXmlParser(PropertyResolver propertyResolver, PropertyReplacer propertyReplacer) {
        this.propertyResolver = propertyResolver;
        this.propertyReplacer = propertyReplacer;
    }


    /**
     * Parse security
     *
     * @param reader The reader
     * @return The result
     * @throws javax.xml.stream.XMLStreamException
     *          XMLStreamException
     * @throws org.jboss.jca.common.metadata.ParserException
     *          ParserException
     * @throws org.jboss.jca.common.api.validator.ValidateException
     *          ValidateException
     */
    @Override
    protected DsSecurity parseDsSecurity(XMLStreamReader reader) throws XMLStreamException, ParserException,
            ValidateException {

        String userName = null;
        String password = null;
        String securityDomain = null;
        boolean elytronEnabled = false;
        String authenticationContext = null;
        Extension reauthPlugin = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) ==
                            DataSource.Tag.SECURITY) {

                        return new DsSecurityImpl(userName, password,elytronEnabled? authenticationContext: securityDomain,
                                elytronEnabled, null, reauthPlugin);
                    } else {
                        if (DsSecurity.Tag.forName(reader.getLocalName()) == DsSecurity.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    DsSecurity.Tag tag = DsSecurity.Tag.forName(reader.getLocalName());
                    switch (tag) {
                        case PASSWORD: {
                            password = elementAsString(reader);
                            boolean resolved = false;
                            if (propertyReplacer != null && password != null && password.trim().length() != 0) {
                                String resolvedPassword = propertyReplacer.replaceProperties(password);
                                if (resolvedPassword != null) {
                                    password = resolvedPassword;
                                    resolved = true;
                                }
                            }
                            // Previous releases directly passed the text into PropertyResolver, which would not
                            // deal properly with ${ and }, :defaultValue etc. But it would resolve e.g. "sys.prop.foo"
                            // to "123" if there was a system property "sys.prop.foo". So, to avoid breaking folks
                            // who learned to use that behavior, pass any unresolved password in to the PropertyResolver
                            if (!resolved && propertyResolver != null && password != null && password.trim().length() != 0) {
                                String resolvedPassword = propertyResolver.resolve(password);
                                if (resolvedPassword != null) {
                                    password = resolvedPassword;
                                }
                            }
                            break;
                        }
                        case USER_NAME: {
                            userName = elementAsString(reader);
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            securityDomain = elementAsString(reader);
                            break;
                        }
                        case ELYTRON_ENABLED: {
                            Boolean value = elementAsBoolean(reader);
                            elytronEnabled = value == null? true : value;
                            break;
                        }
                        case AUTHENTICATION_CONTEXT: {
                            authenticationContext = elementAsString(reader);
                            break;
                        }
                        case REAUTH_PLUGIN: {
                            reauthPlugin = parseExtension(reader, tag.getLocalName());
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    /**
     * parse credential tag
     *
     * @param reader reader
     * @return the parse Object
     * @throws XMLStreamException in case of error
     * @throws ParserException    in case of error
     * @throws ValidateException  in case of error
     */
    @Override
    protected Credential parseCredential(XMLStreamReader reader) throws XMLStreamException, ParserException,
            ValidateException {

        String userName = null;
        String password = null;
        String securityDomain = null;
        boolean elytronEnabled = false;
        String authenticationContext = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY ||
                            Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.RECOVER_CREDENTIAL) {

                        return new CredentialImpl(userName, password, elytronEnabled? authenticationContext: securityDomain,
                                elytronEnabled, null);
                    } else {
                        if (Credential.Tag.forName(reader.getLocalName()) == Credential.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Credential.Tag.forName(reader.getLocalName())) {
                        case PASSWORD: {
                            password = elementAsString(reader);
                            if (propertyResolver != null && password != null) {
                                String resolvedPassword = propertyResolver.resolve(password);
                                if (resolvedPassword != null)
                                    password = resolvedPassword;
                            }
                            break;
                        }
                        case USER_NAME: {
                            userName = elementAsString(reader);
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            securityDomain = elementAsString(reader);
                            break;
                        }
                        case ELYTRON_ENABLED: {
                            Boolean value = elementAsBoolean(reader);
                            elytronEnabled = value == null? true : value;
                            break;
                        }
                        case AUTHENTICATION_CONTEXT: {
                            authenticationContext = elementAsString(reader);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }
}
