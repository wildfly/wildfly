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
package org.jboss.as.connector.metadata.ds;

import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.api.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.validator.ValidateException;

/**
 * Extension of {@link org.jboss.jca.common.metadata.ds.DsSecurityImpl} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public class DsSecurityImpl
        extends org.jboss.jca.common.metadata.ds.DsSecurityImpl implements DsSecurity, Credential {

    private static final long serialVersionUID = 312322268048179001L;

    /**
     * Indicates if the Credential data belongs to Elytron or PicketBox.
     */
    private boolean elytronEnabled;

    /**
     * Create a new DsSecurityImpl.
     *
     * @param userName        user name
     * @param password        user password
     * @param securityContext specific information used by implementation to define in which context this user/password info
     *                        belongs
     * @param elytronEnabled  indicates if elytron is enabled. In this case, {@param securityContext}, defined as
     *                        securityDomain in super class, refers to an Elytron authentication context
     * @param reauthPlugin    reauthentication plugin
     * @throws ValidateException in case of validation error
     */
    public DsSecurityImpl(String userName, String password, String securityContext, boolean elytronEnabled,
            Extension reauthPlugin) throws ValidateException {
        super(userName, password, securityContext, reauthPlugin);
        this.elytronEnabled = elytronEnabled;
    }

    /**
     * Indicates if Elytron is enabled. In this case, {@link #getSecurityDomain()}, refers to an Elytron authentication context
     *
     * @return {@code true} if is Elytron enabled
     */
    @Override
    public final boolean isElytronEnabled() {
        return elytronEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + (elytronEnabled? 1: 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof DsSecurityImpl))
            return false;
        DsSecurityImpl other = (DsSecurityImpl) obj;
        return elytronEnabled == other.elytronEnabled && super.equals(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (!elytronEnabled)
            return super.toString();

        StringBuilder sb = new StringBuilder();

        sb.append("<security>");
        sb.append("<").append(DsSecurity.Tag.ELYTRON_ENABLED).append("/>");
        if (getSecurityDomain() != null) {
            sb.append("<").append(DsSecurity.Tag.AUTHENTICATION_CONTEXT).append(">");
            sb.append(getSecurityDomain());
            sb.append("</").append(DsSecurity.Tag.AUTHENTICATION_CONTEXT).append(">");
        }

        if (getReauthPlugin() != null) {
            sb.append("<").append(DsSecurity.Tag.REAUTH_PLUGIN);
            sb.append(" ").append(Extension.Attribute.CLASS_NAME).append("=\"");
            sb.append(getReauthPlugin().getClassName()).append("\"");
            sb.append(">");

            if (getReauthPlugin().getConfigPropertiesMap().size() > 0) {
                java.util.Iterator<java.util.Map.Entry<String, String>> it = getReauthPlugin().getConfigPropertiesMap().entrySet().iterator();

                while (it.hasNext()) {
                    java.util.Map.Entry<String, String> entry = it.next();

                    sb.append("<").append(Extension.Tag.CONFIG_PROPERTY);
                    sb.append(" name=\"").append(entry.getKey()).append("\">");
                    sb.append(entry.getValue());
                    sb.append("</").append(Extension.Tag.CONFIG_PROPERTY).append(">");
                }
            }

            sb.append("</").append(DsSecurity.Tag.REAUTH_PLUGIN).append(">");
        }

        sb.append("</security>");

        return sb.toString();
    }
}
