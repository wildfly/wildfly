/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runasprincipal;


import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;


@Stateless
@Remote(WhoAmI.class)
@RunAsPrincipal("Admin")
@SecurityDomain("other")
public class CallerRunAsPrincipal implements WhoAmI {
    @EJB(beanName = "StatelessBBean")
    private WhoAmI beanB;

    public String getCallerPrincipal() {
        return beanB.getCallerPrincipal();
    }
}
