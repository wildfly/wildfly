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
package org.jboss.as.test.integration.ejb.container.interceptor.security.api;

import javax.annotation.security.PermitAll;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * Implementation of {@link Manage} interface which calls (remotely) the {@link TargetBean} EJB methods. Methods of this class
 * are not protected (@PermitAll used on class level).
 *
 * @author Josef Cacek
 */
@Stateless(name = Manage.BEAN_NAME_BRIDGE)
@Remote(Manage.class)
@PermitAll
public class BridgeBean implements Manage {

    private static Logger LOGGER = Logger.getLogger(BridgeBean.class);

    // Public methods --------------------------------------------------------

    public String role1() {
        return getTargetBean().role1();
    }

    public String role2() {
        return getTargetBean().role2();
    }

    public String allRoles() {
        return getTargetBean().allRoles();
    }

    // Private methods -------------------------------------------------------

    private Manage getTargetBean() {
        try {
            return EJBUtil.lookupEJB(TargetBean.class, Manage.class);
        } catch (NamingException e) {
            LOGGER.error("Error", e);
            throw new RuntimeException(e);
        }
    }

}
