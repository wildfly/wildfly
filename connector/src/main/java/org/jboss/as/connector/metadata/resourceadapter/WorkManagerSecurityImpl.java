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
package org.jboss.as.connector.metadata.resourceadapter;

import java.util.List;
import java.util.Map;

import org.jboss.as.connector.metadata.api.resourceadapter.WorkManagerSecurity;

/**
 * Extension of {@link org.jboss.jca.common.metadata.resourceadapter.WorkManagerSecurityImpl} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public class WorkManagerSecurityImpl extends org.jboss.jca.common.metadata.resourceadapter.WorkManagerSecurityImpl implements
        WorkManagerSecurity {

    private static final long serialVersionUID = -6928615726774510406L;

    // indicates if Elytron is enabled. In this case, securityContext, defined as securityDomain in super class,
    // refers to Elytron authenticationContext
    private final boolean elytronEnabled;

    /**
     * Constructor.
     *
     * @param mappingRequired   is mapping required
     * @param securityContext   specific information used by implementation to define in which context this security info
     *                          belongs
     * @param elytronEnabled    indicates if Elytron is responsible for taking care of work manager security
     * @param defaultPrincipal  default principal
     * @param defaultGroups     default groups
     * @param userMappings      user mappings
     * @param groupMappings     group mappings
     */
    public WorkManagerSecurityImpl(boolean mappingRequired, String securityContext, boolean elytronEnabled, String defaultPrincipal,
            List<String> defaultGroups, Map<String, String> userMappings, Map<String, String> groupMappings) {
        super(mappingRequired, securityContext, defaultPrincipal, defaultGroups, userMappings, groupMappings);
        this.elytronEnabled = elytronEnabled;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElytronEnabled() {
        return elytronEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        if (elytronEnabled) {
            result += 31;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null)
            return false;
        if (!(o instanceof WorkManagerSecurityImpl))
            return false;
        WorkManagerSecurityImpl other = (WorkManagerSecurityImpl) o;
        return other.elytronEnabled == elytronEnabled && super.equals(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (!elytronEnabled) {
            return super.toString();
        }
        StringBuilder sb = new StringBuilder(1024);

        sb.append("<security>");

        sb.append("<").append(WorkManagerSecurity.Tag.MAPPING_REQUIRED).append(">");
        sb.append(isMappingRequired());
        sb.append("</").append(WorkManagerSecurity.Tag.MAPPING_REQUIRED).append(">");

        if (getDomain() != null) {
            if (elytronEnabled) {
                sb.append("<").append(WorkManagerSecurity.Tag.ELYTRON_SECURITY_DOMAIN).append(">");
                sb.append(getDomain());
                sb.append("</").append(WorkManagerSecurity.Tag.ELYTRON_SECURITY_DOMAIN).append(">");

            } else {
                sb.append("<").append(WorkManagerSecurity.Tag.DOMAIN).append(">");
                sb.append(getDomain());
                sb.append("</").append(WorkManagerSecurity.Tag.DOMAIN).append(">");
            }
        }

        if (getDefaultPrincipal() != null)
        {
            sb.append("<").append(WorkManagerSecurity.Tag.DEFAULT_PRINCIPAL).append(">");
            sb.append(getDefaultPrincipal());
            sb.append("</").append(WorkManagerSecurity.Tag.DEFAULT_PRINCIPAL).append(">");
        }

        if (getDefaultGroups() != null && getDefaultGroups().size() > 0)
        {
            sb.append("<").append(WorkManagerSecurity.Tag.DEFAULT_GROUPS).append(">");
            for (String group : getDefaultGroups())
            {
                sb.append("<").append(WorkManagerSecurity.Tag.GROUP).append(">");
                sb.append(group);
                sb.append("</").append(WorkManagerSecurity.Tag.GROUP).append(">");
            }
            sb.append("</").append(WorkManagerSecurity.Tag.DEFAULT_GROUPS).append(">");
        }

        if ((getUserMappings() != null && getUserMappings().size() > 0) || (getGroupMappings() != null && getGroupMappings().size() > 0))
        {
            sb.append("<").append(WorkManagerSecurity.Tag.MAPPINGS).append(">");

            if (getUserMappings() != null && getUserMappings().size() > 0)
            {
                sb.append("<").append(WorkManagerSecurity.Tag.USERS).append(">");

                for (Map.Entry<String, String> entry : getUserMappings().entrySet())
                {
                    sb.append("<").append(WorkManagerSecurity.Tag.MAP);

                    sb.append(" ").append(WorkManagerSecurity.Attribute.FROM).append("=\"");
                    sb.append(entry.getKey()).append("\"");

                    sb.append(" ").append(WorkManagerSecurity.Attribute.TO).append("=\"");
                    sb.append(entry.getValue()).append("\"");

                    sb.append("/>");
                }

                sb.append("</").append(WorkManagerSecurity.Tag.USERS).append(">");
            }

            if (getGroupMappings() != null && getGroupMappings().size() > 0)
            {
                sb.append("<").append(WorkManagerSecurity.Tag.GROUPS).append(">");

                for (Map.Entry<String, String> entry : getGroupMappings().entrySet())
                {
                    sb.append("<").append(WorkManagerSecurity.Tag.MAP);

                    sb.append(" ").append(WorkManagerSecurity.Attribute.FROM).append("=\"");
                    sb.append(entry.getKey()).append("\"");

                    sb.append(" ").append(WorkManagerSecurity.Attribute.TO).append("=\"");
                    sb.append(entry.getValue()).append("\"");

                    sb.append("/>");
                }

                sb.append("</").append(WorkManagerSecurity.Tag.GROUPS).append(">");
            }

            sb.append("</").append(WorkManagerSecurity.Tag.MAPPINGS).append(">");
        }

        sb.append("</security>");

        return sb.toString();
    }
}
