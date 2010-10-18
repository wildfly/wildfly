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

package org.jboss.as.metadata.parser.jbossweb;

import java.util.HashMap;

public class Location {
    private static final HashMap<String, Version> bindings = new HashMap<String, Version>();
    static {
        bindings.put("http://www.jboss.org/j2ee/dtd/jboss-web_3_0.dtd", Version.JBOSS_WEB_3_0);
        bindings.put("http://www.jboss.org/j2ee/dtd/jboss-web_3_2.dtd", Version.JBOSS_WEB_3_2);
        bindings.put("http://www.jboss.org/j2ee/dtd/jboss-web_4_0.dtd", Version.JBOSS_WEB_4_0);
        bindings.put("http://www.jboss.org/j2ee/dtd/jboss-web_4_2.dtd", Version.JBOSS_WEB_4_2);
        bindings.put("http://www.jboss.org/j2ee/dtd/jboss-web_5_0.dtd", Version.JBOSS_WEB_5_0);
        bindings.put("http://www.jboss.org/j2ee/schema/jboss-web_5_1.xsd", Version.JBOSS_WEB_5_1);
        bindings.put("http://www.jboss.org/j2ee/schema/jboss-web_6_0.xsd", Version.JBOSS_WEB_6_0);
    }
    public static Version getVersion(String location) {
        return bindings.get(location);
    }
}
