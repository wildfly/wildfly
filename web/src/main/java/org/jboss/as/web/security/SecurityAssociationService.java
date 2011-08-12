/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.security;

import org.apache.catalina.core.StandardContext;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The purpose of this service is to add a final valve to the StandardContext after it has been started to perform any required
 * SecurityContext association of a previously authenticated user.
 * <p/>
 * This is necessary as the Realm is only called when authentication is actually required, if the authentication has already
 * occurred the realm is not called again so the SecurityContext is not initialised and associated.
 * <p/>
 * (Loosely based on the SecurityAssociationValve from AS6)
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityAssociationService implements Service<SecurityAssociationService> {

    private final StandardContext context;

    private final JBossWebMetaData metaData;

    public SecurityAssociationService(final StandardContext context, final JBossWebMetaData metaData) {
        this.context = context;
        this.metaData = metaData;
    }

    public void start(StartContext startContext) throws StartException {
        SecurityContextAssociationValve valve = new SecurityContextAssociationValve(metaData);
        context.addValve(valve);
    }

    public void stop(StopContext stopContext) {
    }

    public SecurityAssociationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
