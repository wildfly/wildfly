/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runasprincipal.transitive;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import org.jboss.as.test.integration.ejb.security.runasprincipal.WhoAmI;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@Singleton
@Remote(WhoAmI.class)
@SecurityDomain("other")
public class SimpleSingletonBean implements WhoAmI {

    @EJB(beanName = "StatelessBBean")
    private WhoAmI beanB;

    private String principal;

    @PostConstruct
    public void init() {
        principal = beanB.getCallerPrincipal();
    }

    public String getCallerPrincipal() {
        return principal;
    }

}
