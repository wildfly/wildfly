/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * A {@link Service} that returns a mapping of security domains by name.
 *
 * <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class SecurityDomainsService implements Service<Map<String, SecurityDomain>> {

    private final Map<String, InjectedValue<SecurityDomain>> injectedSecurityDomainsByName = new HashMap<>();
    private volatile Map<String, SecurityDomain> securityDomainsByName;

    @Override
    public void start(final StartContext context) throws StartException {
        if ((injectedSecurityDomainsByName != null) && ! injectedSecurityDomainsByName.isEmpty()) {
            securityDomainsByName = new HashMap<>(injectedSecurityDomainsByName.size());
            injectedSecurityDomainsByName.forEach((key, value) -> securityDomainsByName.put(key, value.getValue()));
        }
    }

    @Override
    public void stop(final StopContext context) {
        securityDomainsByName = null;
    }

    @Override
    public Map<String, SecurityDomain> getValue() throws IllegalStateException, IllegalArgumentException {
        return securityDomainsByName == null ? Collections.<String, SecurityDomain>emptyMap() : Collections.unmodifiableMap(securityDomainsByName);
    }

    Injector<SecurityDomain> createSecurityDomainInjector(final String securityDomainAlias) {
        if (injectedSecurityDomainsByName.containsKey(securityDomainAlias)) {
            return null;
        }
        InjectedValue<SecurityDomain> securityDomainInjector = new InjectedValue<>();
        injectedSecurityDomainsByName.put(securityDomainAlias, securityDomainInjector);
        return securityDomainInjector;
    }
}
