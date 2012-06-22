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
package org.jboss.as.test.integration.security.common;

/**
 * Common constants for AS security tests.
 * 
 * @author Josef Cacek
 */
public class SecurityTestConstants {

    /** A web.xml content (web-app version=3.0), which sets authentication method to BASIC. */
    public static final String WEB_XML_BASIC_AUTHN = "<?xml version='1.0'?>\n"
            + "<web-app xmlns='http://java.sun.com/xml/ns/javaee' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
            + "    xsi:schemaLocation='http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd'\n"
            + "    version='3.0'>\n" // 
            + "  <login-config>\n" //
            + "    <auth-method>BASIC</auth-method>\n" //
            + "    <realm-name>Test realm</realm-name>\n" //
            + "  </login-config>\n" //
            + "</web-app>\n";

}
