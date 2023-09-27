/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
class MailSubsystemParser4_0 extends PersistentResourceXMLParser {

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return builder(MailExtension.SUBSYSTEM_PATH, Namespace.MAIL_4_0.getUriString())
                .addChild(
                        builder(MailExtension.MAIL_SESSION_PATH)
                                .addAttributes(MailSessionDefinition.DEBUG, MailSessionDefinition.JNDI_NAME, MailSessionDefinition.FROM)
                                .addChild(
                                        builder(MailSubsystemModel.SMTP_SERVER_PATH)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD, MailServerDefinition.CREDENTIAL_REFERENCE)
                                                .setXmlElementName(MailSubsystemModel.SMTP_SERVER)

                                )
                                .addChild(
                                        builder(MailSubsystemModel.POP3_SERVER_PATH)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD, MailServerDefinition.CREDENTIAL_REFERENCE)
                                                .setXmlElementName(MailSubsystemModel.POP3_SERVER)
                                )
                                .addChild(
                                        builder(MailSubsystemModel.IMAP_SERVER_PATH)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD, MailServerDefinition.CREDENTIAL_REFERENCE)
                                                .setXmlElementName(MailSubsystemModel.IMAP_SERVER)
                                )
                                .addChild(
                                        builder(MailSubsystemModel.CUSTOM_SERVER_PATH)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF_OPTIONAL, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD, MailServerDefinition.CREDENTIAL_REFERENCE, MailServerDefinition.PROPERTIES)
                                                .setXmlElementName(MailSubsystemModel.CUSTOM_SERVER)
                                )
                )
                .build();
    }
}
