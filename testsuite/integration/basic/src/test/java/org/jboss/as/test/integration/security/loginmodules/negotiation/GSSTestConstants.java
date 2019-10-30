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
package org.jboss.as.test.integration.security.loginmodules.negotiation;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A GSSTestConstants.
 *
 * @author Josef Cacek
 */
public interface GSSTestConstants {

    String PROPERTY_PORT = "gsstestserver.port";
    String PROPERTY_PRINCIPAL = "gsstestserver.principal";
    String PROPERTY_PASSWORD = "gsstestserver.password";

    int PORT = Integer.getInteger(PROPERTY_PORT, 10961);
    String PRINCIPAL = System.getProperty(PROPERTY_PRINCIPAL, "gsstestserver/xxx@JBOSS.ORG");
    String PASSWORD = System.getProperty(PROPERTY_PASSWORD, "gsstestpwd");

    Charset CHAR_ENC = StandardCharsets.UTF_8;
    int CMD_NOOP = 0;
    int CMD_NAME = 1;
    int CMD_STOP = 2;

    int SOCKET_TIMEOUT = 30000; //30s

}
