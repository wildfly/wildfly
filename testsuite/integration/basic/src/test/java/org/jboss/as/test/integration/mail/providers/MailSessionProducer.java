/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.providers;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.mail.Session;

@ApplicationScoped
public class MailSessionProducer {

    @Produces
    @Resource(mappedName = "java:jboss/mail/Default")
    private Session sessionFieldProducer;

    @Resource(mappedName = "java:jboss/mail/Default")
    private Session sessionMethodProducer;

    @Produces
    @MethodInjectQualifier
    public Session methodProducer() {
        return sessionMethodProducer;
    }
}
