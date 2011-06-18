/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.remoting;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.xnio.OptionMap;

/**
 * A service to create the OptionMap based on the security realm capabilities.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RealmOptionMapService implements Service<OptionMap> {

    private final InjectedValue<RealmAuthenticationProvider> realmAuthenticationProviderInjectedValue = new InjectedValue<RealmAuthenticationProvider>();

    public void start(StartContext startContext) throws StartException {
    }

    public void stop(StopContext stopContext) {
    }

    public OptionMap getValue() throws IllegalStateException, IllegalArgumentException {
        return realmAuthenticationProviderInjectedValue.getValue().getSaslOptionMap();
    }

    public InjectedValue<RealmAuthenticationProvider> getRealmAuthenticationProviderInjectedValue() {
        return realmAuthenticationProviderInjectedValue;
    }
}
