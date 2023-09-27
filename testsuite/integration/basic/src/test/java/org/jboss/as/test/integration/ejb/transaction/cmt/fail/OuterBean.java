/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.cmt.fail;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OuterBean {

    @EJB InnerBean innerBean;

    public void outerMethodXA() {
        innerBean.innerMethodXA();
    }

    public void outerMethod2pcXA() {
        innerBean.innerMethod2pcXA();
    }

    public void outerMethodLocal() {
        innerBean.innerMethodLocal();
    }
}
