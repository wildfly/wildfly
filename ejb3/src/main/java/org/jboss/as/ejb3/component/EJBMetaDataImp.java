/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.component;

import java.io.Serializable;

import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;

/**
 * An implementation of the EJBMetaData interface which allows a
 * client to obtain the enterprise Bean's meta-data information.
 *
 * TODO: remove this on next EJB client release
 *
 * @author Rickard ???berg (rickard.oberg@telkel.com)
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author Stuart Douglas
 */
public class EJBMetaDataImp implements EJBMetaData, Serializable {
    private final Class remoteClass;

    private final Class homeClass;

    private final Class pkClass;

    private final boolean session;

    private final boolean statelessSession;

    private final EJBHome home;

    public EJBMetaDataImp(final Class remoteClass, final Class homeClass, final Class pkClass, final boolean session,
                          final boolean statelessSession, final EJBHome home) {
        this.remoteClass = remoteClass;
        this.homeClass = homeClass;
        this.pkClass = pkClass;
        this.session = session;
        this.statelessSession = statelessSession;
        this.home = home;
    }


    /**
     * Obtains the home interface of the enterprise Bean.
     */
    public EJBHome getEJBHome() {
        return home;
    }

    /**
     * Obtains the <code>Class</code> object for the enterprise Bean's home
     * interface.
     */
    public Class getHomeInterfaceClass() {
        return homeClass;
    }

    /**
     * Obtains the <code>Class</code> object for the enterprise Bean's remote
     * interface.
     */
    public Class getRemoteInterfaceClass() {
        return remoteClass;
    }

    /**
     * Obtains the <code>Class</code> object for the enterprise Bean's primary
     * key class.
     */
    public Class getPrimaryKeyClass() {
        if (session)
            throw new RuntimeException("A session bean does not have a primary key class");
        return pkClass;
    }

    /**
     * Tests if the enterprise Bean's type is "session".
     *
     * @return true if the type of the enterprise Bean is session bean.
     */
    public boolean isSession() {
        return session;
    }

    /**
     * Tests if the enterprise Bean's type is "stateless session".
     *
     * @return true if the type of the enterprise Bean is stateless session.
     */
    public boolean isStatelessSession() {
        return statelessSession;
    }
}