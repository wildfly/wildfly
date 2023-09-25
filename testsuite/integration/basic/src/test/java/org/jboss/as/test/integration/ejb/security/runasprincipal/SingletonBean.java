/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runasprincipal;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.junit.Assert;

/**
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@Singleton
@Startup
@Remote(WhoAmI.class)
@RolesAllowed("Users")
@RunAs("Admin")
@RunAsPrincipal("Helloween")
@SecurityDomain("other")
public class SingletonBean implements WhoAmI {
    @EJB(beanName = "StatelessBBean")
    private WhoAmI beanB;

    private String principal;

    @PostConstruct
    public void init() {
        principal = beanB.getCallerPrincipal();
        Assert.assertEquals("Helloween", principal);
    }

    public String getCallerPrincipal() {
        return principal;
    }
}
