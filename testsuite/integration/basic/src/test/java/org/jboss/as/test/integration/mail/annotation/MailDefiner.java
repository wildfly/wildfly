/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.annotation;

import jakarta.ejb.Stateless;
import jakarta.mail.MailSessionDefinition;


/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless
@MailSessionDefinition(
        name = "java:app/mail/MySession",
        host = "somewhere.myco.com",
        from = "some.body@myco.com")

public class MailDefiner {

}
