/*
 * Copyright 2016 Red Hat, Inc.
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
package org.jboss.as.security.elytron;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.auth.server.SecurityRealm;

/**
 * A {@link org.jboss.msc.service.Service} that instantiates a {@link SecurityDomainContextRealm}. This realm is registered
 * as a capability by the {@link ElytronRealmAdd} handler and can be consumed in the Elytron subsystem just like any
 * other regular Elytron realm.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
public class SecurityDomainContextRealmService implements Service<SecurityRealm> {

    private volatile SecurityRealm securityRealm;

    private InjectedValue<SecurityDomainContext> securityDomainContextInjector = new InjectedValue<>();

    @Override
    public void start(final StartContext startContext) throws StartException {
        final SecurityDomainContext context = this.securityDomainContextInjector.getValue();
        this.securityRealm = new SecurityDomainContextRealm(context);
    }

    @Override
    public void stop(final StopContext stopContext) {
        this.securityRealm = null;
    }

    @Override
    public SecurityRealm getValue() throws IllegalStateException, IllegalArgumentException {
        return this.securityRealm;
    }

    public Injector<SecurityDomainContext> getSecurityDomainContextInjector() {
        return this.securityDomainContextInjector;
    }
}
