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

import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Security;
import org.jboss.jca.common.api.metadata.common.SecurityMetadata;
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
        required = isLegacySecurityRequired(wmsecurity);
        if (!required) {
            List<ConnectionDefinition> connDefs = raxml.getConnectionDefinitions();
            if (connDefs != null) {
                for (ConnectionDefinition cd : connDefs) {
                    Security cdSecurity = cd.getSecurity();
                    Credential cdRecoveryCredential = cd.getRecovery() == null? null : cd.getRecovery().getCredential();
                    if (isLegacySecurityRequired(cdSecurity) || isLegacySecurityRequired(cdRecoveryCredential)) {
                        required = true;
                        break;
                    }
                }
            }
        }
        return required;
    }

    public static boolean isLegacySecurityRequired(SecurityMetadata security) {
        // no security config
        if (security == null)
            return false;
        // security uses elytron
        if (security instanceof org.jboss.as.connector.metadata.api.common.SecurityMetadata &&
                ((org.jboss.as.connector.metadata.api.common.SecurityMetadata) security).isElytronEnabled())
            return false;
        // check if legacy domain is non-null
        final String domain = security.resolveSecurityDomain();
        return domain != null && domain.trim().length() > 0;
    }

    private static boolean isLegacySecurityRequired(org.jboss.jca.common.api.metadata.resourceadapter.WorkManagerSecurity config) {
        // no security config
        if (config == null)
            return false;
        // security config uses elytron
        if (config instanceof WorkManagerSecurity && ((WorkManagerSecurity) config).isElytronEnabled())
            return false;
        // check if legacy domain is non-null
        final String domain = config.getDomain();
        return domain != null && domain.trim().length() > 0;
    }

    private ActivationSecurityUtil() {}
}
