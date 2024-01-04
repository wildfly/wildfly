/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.methodparams;

import jakarta.ejb.EJBException;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;

public class SecondBean implements SessionBean {


    public void ejbCreate() {

    }

    public void ejbPassivate() {
    }

    public void ejbActivate() {
    }

    public void ejbRemove() {
    }

    public boolean test(String[] s) {
        return true;
    }

    public boolean test(String s) {
        return true;
    }

    public boolean test(int x) {
        return true;
    }

    public void setSessionContext(SessionContext arg0) throws EJBException {

    }

}
