/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2015, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.security.realm;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.auth.server.SecurityRealm;

/**
 * A {@link org.jboss.msc.service.Service} that instantiates a {@link org.jboss.as.security.realm.DomainContextRealm}.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
public class DomainContextRealmService implements Service<SecurityRealm> {

    private volatile SecurityRealm securityRealm;

    private InjectedValue<SecurityDomainContext> securityDomainContextInjector = new InjectedValue<>();

    @Override
    public void start(final StartContext startContext) throws StartException {
        final SecurityDomainContext context = this.securityDomainContextInjector.getValue();
        this.securityRealm = new DomainContextRealm(context);
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
