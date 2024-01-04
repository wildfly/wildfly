/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.jacc.propagation;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementation of {@link Manage} interface which has injected {@link TargetBean} EJB and calls it's methods as
 * {@link Manage#ROLE_MANAGER} role (using {@link jakarta.annotation.security.RunAs} annotation). This class is protected, it
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
