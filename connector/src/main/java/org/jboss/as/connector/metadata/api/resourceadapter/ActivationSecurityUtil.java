/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.connector.metadata.api.resourceadapter;

import java.util.List;

import org.jboss.jca.common.api.metadata.common.Security;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition;

/**
 * Utilities related to use of {@link WorkManagerSecurity}.
 *
 * @author Brian Stansberry
 */
public final class ActivationSecurityUtil {

    public static boolean isLegacySecurityRequired(Activation raxml) {
        boolean required = false;
        org.jboss.jca.common.api.metadata.resourceadapter.WorkManagerSecurity wmsecurity = raxml.getWorkManager() != null ? raxml.getWorkManager().getSecurity() : null;
        if (wmsecurity != null && !isElytronEnabled(wmsecurity)) {
            String domain = wmsecurity.getDomain();
            required = domain != null && domain.trim().length() > 0;
        }
        if (!required) {
            List<ConnectionDefinition> connDefs = raxml.getConnectionDefinitions();
            if (connDefs != null) {
                for (ConnectionDefinition cd : connDefs) {
                    Security cdsecurity = cd.getSecurity();
                    if (cdsecurity != null && !isElytronEnabled(cdsecurity)) {
                        String domain = cdsecurity.resolveSecurityDomain();
                        if (domain != null && domain.trim().length() > 0) {
                            required = true;
                            break;
                        }
                    }
                }
            }
        }
        return required;
    }

    public static boolean isLegacySecurityRequired(Security security) {
        boolean required = security != null && !isElytronEnabled(security);
        if (required) {
            String domain = security.resolveSecurityDomain();
            required = domain != null && domain.trim().length() > 0;
        }
        return required;
    }

    private static boolean isElytronEnabled(Object config) {
        return config instanceof WorkManagerSecurity && ((WorkManagerSecurity) config).isElytronEnabled();
    }

    private ActivationSecurityUtil() {}
}
