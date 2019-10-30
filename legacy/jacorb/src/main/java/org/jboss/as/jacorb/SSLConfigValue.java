/*
* JBoss, Home of Professional Open Source.
* Copyright 2014, Red Hat, Inc., and individual contributors
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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.jboss.as.jacorb;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Enumeration of the SSL configuration values. Each enum contains the corresponding IIOP value, which is represented
 * as an int.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
enum SSLConfigValue {

    NONE("None", "0"), SERVERAUTH("ServerAuth", "20"), CLIENTAUTH("ClientAuth", "40"), MUTUALAUTH("MutualAuth", "60");

    private String name;
    private String iiopValue;

    SSLConfigValue(String name, String iiopValue) {
        this.name = name;
        this.iiopValue = iiopValue;
    }

    public String getIIOPValue() {
        return this.iiopValue;
    }

    private static Map<String, SSLConfigValue> MAP;

    static {
        final Map<String, SSLConfigValue> map = new HashMap<String, SSLConfigValue>();
        for (SSLConfigValue configValue : values()) {
            map.put(configValue.getIIOPValue(), configValue);
        }
        MAP = map;
    }

    public static SSLConfigValue fromValue(String value) {
        return MAP.get(value);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
