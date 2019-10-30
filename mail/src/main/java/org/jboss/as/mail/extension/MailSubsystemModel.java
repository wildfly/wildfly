/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import org.jboss.as.controller.PathElement;

/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 * @created 26.7.11 15:21
 */
interface MailSubsystemModel {
    String LOGIN = "login";
    String SERVER_TYPE = "server";
    String SMTP_SERVER = "smtp-server";
    String POP3_SERVER = "pop3-server";
    String IMAP_SERVER = "imap-server";
    String CUSTOM_SERVER = "custom-server";

    String MAIL_SESSION = "mail-session";
    String LOGIN_USERNAME = "name";
    String USER_NAME = "username";
    String PASSWORD = "password";
    String JNDI_NAME = "jndi-name";
    String DEBUG = "debug";
    String OUTBOUND_SOCKET_BINDING_REF = "outbound-socket-binding-ref";
    String SSL = "ssl";
    String TLS = "tls";
    String FROM = "from";
    String POP3 = "pop3";
    String SMTP = "smtp";
    String IMAP = "imap";
    String NAME = "name";
    String CUSTOM = "custom";
    String PROPERTY = "property";


    PathElement POP3_SERVER_PATH = PathElement.pathElement(SERVER_TYPE, POP3);
    PathElement SMTP_SERVER_PATH = PathElement.pathElement(SERVER_TYPE, SMTP);
    PathElement IMAP_SERVER_PATH = PathElement.pathElement(SERVER_TYPE, IMAP);
    PathElement CUSTOM_SERVER_PATH = PathElement.pathElement(CUSTOM);
}
