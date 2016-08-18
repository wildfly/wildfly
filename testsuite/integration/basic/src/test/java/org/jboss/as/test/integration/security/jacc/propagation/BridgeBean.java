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
package org.jboss.as.test.integration.security.jacc.propagation;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Implementation of {@link Manage} interface which has injected {@link TargetBean} EJB and calls it's methods as
 * {@link Manage#ROLE_MANAGER} role (using {@link javax.annotation.security.RunAs} annotation). This class is protected, it
 * allows access to all test roles. Methods of this class are not protected.
 *
 * @author Josef Cacek
 */
@Stateless(name = Manage.BEAN_NAME_BRIDGE)
@DeclareRoles({Manage.ROLE_ADMIN, Manage.ROLE_MANAGER, Manage.ROLE_USER})
@RunAs(Manage.ROLE_MANAGER)
@RolesAllowed({Manage.ROLE_ADMIN, Manage.ROLE_MANAGER, Manage.ROLE_USER})
public class BridgeBean implements Manage {

    @EJB(beanName = Manage.BEAN_NAME_TARGET)
    private Manage targetBean = null;

    // Public methods --------------------------------------------------------

    public String admin() {
        return targetBean.admin();
    }

    public String manage() {
        return targetBean.manage();
    }

    public String work() {
        return targetBean.work();
    }

}
