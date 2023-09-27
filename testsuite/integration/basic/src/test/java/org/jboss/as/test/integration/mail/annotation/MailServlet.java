/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.annotation;

import jakarta.mail.MailSessionDefinition;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@MailSessionDefinition(
        name = "java:/mail/test-mail-session-1",
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
        name = "java:/mail/test-mail-session-2",
        host = "localhost",
        transportProtocol = "smtp",
        properties = {
                "mail.smtp.host=localhost",
                "mail.smtp.port=5555",
                "mail.smtp.sendpartial=true",
                "mail.debug=true"
        })
@WebServlet
public final class MailServlet extends HttpServlet {


}
