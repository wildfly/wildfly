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
package org.wildfly.extension.picketlink.federation.config;

import org.picketlink.config.federation.IDPType;
import org.picketlink.config.federation.KeyProviderType;
import org.picketlink.config.federation.TrustType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.picketlink.common.util.StringUtil.isNullOrEmpty;

/**
 * <p> This class is responsible to store all information about a given Identity Provider deployment. The state is populated with
 * values from the subsystem configuration. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 12, 2012
 */
public class IDPConfiguration extends IDPType implements ProviderConfiguration {

    private final String alias;
    private volatile String securityDomain;
    private final Map<String, String> trustDomainAlias = new HashMap<String, String>();
    private volatile boolean external;
    private volatile boolean supportMetadata;

    public IDPConfiguration(String alias) {
        this.alias = alias;
        setTrust(new TrustType());
        getTrust().setDomains("");
    }

    @Override
    public String getAlias() {
        return this.alias;
    }

    @Override
    public String getSecurityDomain() {
        return this.securityDomain;
    }

    @Override
    public void setKeyProvider(KeyProviderType keyProviderType) {
        super.setKeyProvider(keyProviderType);
    }

    public void setSecurityDomain(String securityDomain) {
        this.securityDomain = securityDomain;
    }

    public void addTrustDomain(String domain) {
        String domainsList = getDomains();

        if (!isNullOrEmpty(domainsList)) {
            domainsList = domainsList + ",";
        }

        getTrust().setDomains(domainsList + domain);

        this.trustDomainAlias.put(domain.trim(), domain.trim());
    }

    public void removeTrustDomain(String domain) {
        String domainsList = getDomains();

        if (!isNullOrEmpty(domainsList)) {
            getTrust().setDomains("");

            for (String currentDomain : domainsList.split(",")) {
                if (!domain.equals(currentDomain) && !isNullOrEmpty(currentDomain)) {
                    addTrustDomain(currentDomain);
                }
            }
        }
    }

    private String getDomains() {
        if (getTrust().getDomains() == null) {
            getTrust().setDomains("");
        }

        return getTrust().getDomains();
    }

    public Map<String, String> getTrustDomainAlias() {
        return Collections.unmodifiableMap(this.trustDomainAlias);
    }

    public void setExternal(boolean external) {
        this.external = external;
    }

    public boolean isExternal() {
        return this.external;
    }

    public void setSupportMetadata(boolean supportMetadata) {
        this.supportMetadata = supportMetadata;
    }

    public boolean isSupportMetadata() {
        return supportMetadata;
    }
}
