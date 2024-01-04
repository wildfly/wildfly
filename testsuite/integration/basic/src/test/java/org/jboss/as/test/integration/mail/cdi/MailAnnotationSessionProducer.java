/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.cdi;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.mail.MailSessionDefinition;
import jakarta.mail.Session;

@MailSessionDefinition(
        name = "java:/mail/test-mail-cdi-session-1",
        host = "localhost",
        transportProtocol = "smtp",
        properties = {
                "mail.smtp.host=localhost",
                "mail.smtp.port=5554",
                "mail.smtp.sendpartial=true",
                "mail.debug=true"
        }
)
@MailSessionDefinition(
        name = "java:/mail/test-mail-cdi-session-2",
        host = "localhost",
        transportProtocol = "smtp",
        properties = {
                "mail.smtp.host=localhost",
                "mail.smtp.port=5555",
                "mail.smtp.sendpartial=true",
                "mail.debug=true"
        })
@ApplicationScoped
public class MailAnnotationSessionProducer {

    @Produces
    @Resource(mappedName = "java:/mail/test-mail-cdi-session-1")
    private Session sessionFieldProducer;

    @Resource(mappedName = "java:/mail/test-mail-cdi-session-2")
    private Session sessionMethodProducer;

    @Produces
    @MethodInjectQualifier
    public Session methodProducer() {
        return sessionMethodProducer;
    }
}
