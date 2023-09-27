/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;

/**
 * Bean returning goodbye greeting.
 *
 * @author Ondrej Chaloupka
 */
public class GoodByeBean implements SessionBean {
    private static final long serialVersionUID = 1L;
    public static final String SAYING = "GoodBye";

    public String sayGoodBye() {
        return SAYING;
    }

    public void setSessionContext(SessionContext sessionContext) {
    }

    public void ejbCreate() {
    }

    public void ejbRemove() {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }
}
