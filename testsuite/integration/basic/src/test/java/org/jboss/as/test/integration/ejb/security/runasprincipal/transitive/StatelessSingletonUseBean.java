/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runasprincipal.transitive;

import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import org.jboss.as.test.integration.ejb.security.runasprincipal.WhoAmI;
import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */

@Stateless
@Remote(WhoAmI.class)
@RunAs("Admin")
@RunAsPrincipal("IronMaiden")
@SecurityDomain("other")
public class StatelessSingletonUseBean implements WhoAmI {

    @EJB(beanName = "SimpleSingletonBean")
    private WhoAmI singleton;

    public String getCallerPrincipal() {
        return singleton.getCallerPrincipal();
    }
}
