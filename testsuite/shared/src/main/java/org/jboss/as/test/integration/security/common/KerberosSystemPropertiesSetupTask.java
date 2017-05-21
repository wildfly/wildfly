/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.security.common;

import java.util.HashMap;
import java.util.Map;

/**
 * ServerSetup task which configures server system properties for Kerberos testing - path to {@code krb5.conf} file etc.
 *
 * @author Josef Cacek
 */
public class KerberosSystemPropertiesSetupTask extends AbstractSystemPropertiesServerSetupTask {

    /**
     * Returns "java.security.krb5.conf" and "sun.security.krb5.debug" properties.
     *
     * @return Kerberos properties
     * @see org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask#getSystemProperties()
     */
    @Override
    protected SystemProperty[] getSystemProperties() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("java.security.krb5.conf", AbstractKrb5ConfServerSetupTask.getKrb5ConfFullPath());
        map.put("sun.security.krb5.debug", "true");
        return mapToSystemProperties(map);
    }

}