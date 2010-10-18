/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.metadata.parser.servlet;

import java.util.HashMap;

public class Location {
    private static final HashMap<String, Version> bindings = new HashMap<String, Version>();
    static {
        bindings.put("http://java.sun.com/j2ee/dtds/web-app_2_2.dtd", Version.SERVLET_2_2);
        bindings.put("http://java.sun.com/dtd/web-app_2_3.dtd", Version.SERVLET_2_3);
        bindings.put("http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd", Version.SERVLET_2_4);
        bindings.put("http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd", Version.SERVLET_2_5);
        bindings.put("http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd", Version.SERVLET_3_0);
    }
    public static Version getVersion(String location) {
        return bindings.get(location);
    }
}
