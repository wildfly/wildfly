/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.ee8.mail;

import javax.mail.MailSessionDefinition;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

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
