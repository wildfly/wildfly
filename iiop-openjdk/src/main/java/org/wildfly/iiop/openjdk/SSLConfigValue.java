/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Enumeration of the SSL configuration values. Each enum contains the corresponding IIOP value, which is represented
 * as an int.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public enum SSLConfigValue {

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
