/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class MailSubsystemParser2_0 extends PersistentResourceXMLParser {
    private final PersistentResourceXMLDescription xmlDescription;

    MailSubsystemParser2_0() {
        xmlDescription = builder(MailExtension.SUBSYSTEM_PATH, Namespace.MAIL_2_0.getUriString())
                .addChild(
                        builder(MailExtension.MAIL_SESSION_PATH)
                                .addAttributes(MailSessionDefinition.DEBUG, MailSessionDefinition.JNDI_NAME, MailSessionDefinition.FROM)
                                .addChild(
                                        builder(MailSubsystemModel.SMTP_SERVER_PATH)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD)
                                                .setXmlElementName(MailSubsystemModel.SMTP_SERVER)

                                )
                                .addChild(
                                        builder(MailSubsystemModel.POP3_SERVER_PATH)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD)
                                                .setXmlElementName(MailSubsystemModel.POP3_SERVER)
                                )
                                .addChild(
                                        builder(MailSubsystemModel.IMAP_SERVER_PATH)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD)
                                                .setXmlElementName(MailSubsystemModel.IMAP_SERVER)
                                )
                                .addChild(
                                        builder(MailSubsystemModel.CUSTOM_SERVER_PATH)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF_OPTIONAL, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD, MailServerDefinition.PROPERTIES)
                                                .setXmlElementName(MailSubsystemModel.CUSTOM_SERVER)
                                )
                )
                .build();
    }


    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }

}
