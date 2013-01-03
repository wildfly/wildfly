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

import org.jboss.as.security.vault.RuntimeVaultReader;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.v10.DataSource;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.ParserException;
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.jca.common.metadata.ds.DsSecurityImpl;
import org.jboss.jca.common.metadata.ds.v11.DsParser;
import org.jboss.logging.Logger;
import org.jboss.metadata.property.PropertyResolver;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * TODO class javadoc.
 *
 */
public class DsXmlParser extends DsParser {


    private final PropertyResolver propertyResolver;


    public DsXmlParser(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
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
        Extension reauthPlugin = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (org.jboss.jca.common.api.metadata.ds.v10.DataSource.Tag.forName(reader.getLocalName()) ==
                            org.jboss.jca.common.api.metadata.ds.v10.DataSource.Tag.SECURITY) {

                        return new DsSecurityImpl(userName, password, securityDomain, reauthPlugin);
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

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY ||
                            Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.RECOVER_CREDENTIAL) {

                        return new CredentialImpl(userName, password, securityDomain);
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
