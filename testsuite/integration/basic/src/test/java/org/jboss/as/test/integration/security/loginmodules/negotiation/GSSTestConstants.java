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

    public static final String PROPERTY_PORT = "gsstestserver.port";
    public static final String PROPERTY_PRINCIPAL = "gsstestserver.principal";
    public static final String PROPERTY_PASSWORD = "gsstestserver.password";

    public static final int PORT = Integer.getInteger(PROPERTY_PORT, 10961);
    public static final String PRINCIPAL = System.getProperty(PROPERTY_PRINCIPAL, "gsstestserver/xxx@JBOSS.ORG");
    public static final String PASSWORD = System.getProperty(PROPERTY_PASSWORD, "gsstestpwd");

    public static final Charset CHAR_ENC = StandardCharsets.UTF_8;
    public static final int CMD_NOOP = 0;
    public static final int CMD_NAME = 1;
    public static final int CMD_STOP = 2;

    public static final int SOCKET_TIMEOUT = 30000; //30s

}
