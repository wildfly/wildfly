/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas;

import static jakarta.ejb.TransactionAttributeType.SUPPORTS;

import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;

import org.jboss.as.test.integration.ejb.security.Entry;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@LocalBean
@RunAs("Role2")
@TransactionAttribute(SUPPORTS)
@SecurityDomain("ejb3-tests")
public class EntryBean extends org.jboss.as.test.integration.ejb.security.base.EntryBean implements Entry {
    @EJB
    private WhoAmIBean whoAmIBean;

    public void callOnlyRole1() {
        whoAmIBean.onlyRole1();
    }
}
