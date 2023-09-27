/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ds;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.connector.logging.ConnectorLogger;
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

/**
 * Parser for -ds.xml
 *
 */
public class DsXmlParser extends DsParser {


    private final PropertyReplacer propertyReplacer;

    public DsXmlParser(PropertyReplacer propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }


    /**
     * Parse security
     *
     * @param reader The reader
     * @return The result
     * @throws XMLStreamException
     *          XMLStreamException
     * @throws ParserException
     *          ParserException
     * @throws ValidateException
     *          ValidateException
     */
    @Override
    protected DsSecurity parseDsSecurity(XMLStreamReader reader) throws XMLStreamException, ParserException,
            ValidateException {

        String userName = null;
        String password = null;
        String authenticationContext = null;
        Extension reauthPlugin = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) ==
                            DataSource.Tag.SECURITY) {

                        return new DsSecurityImpl(userName, password,authenticationContext, null, reauthPlugin);
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
                            break;
                        }
                        case USER_NAME: {
                            userName = elementAsString(reader);
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            throw new ParserException(ConnectorLogger.DS_DEPLOYER_LOGGER.legacySecurityNotSupported());
                        }
                        case ELYTRON_ENABLED: {
                            elementAsBoolean(reader);
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
        String authenticationContext = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY ||
                            Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.RECOVER_CREDENTIAL) {

                        return new CredentialImpl(userName, password, authenticationContext, null);
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
                            if (propertyReplacer != null && password != null) {
                                String resolvedPassword = propertyReplacer.replaceProperties(password);
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
                            throw new ParserException(ConnectorLogger.SUBSYSTEM_RA_LOGGER.legacySecurityNotAvailable());
                        }
                        case ELYTRON_ENABLED: {
                            elementAsBoolean(reader);
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
