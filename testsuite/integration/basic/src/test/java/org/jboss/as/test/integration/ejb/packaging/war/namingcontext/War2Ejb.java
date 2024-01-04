/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.packaging.war.namingcontext;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.UserTransaction;

/**
 * @author Stuart Douglas
 */
@Stateless
@LocalBean
@TransactionManagement(value = TransactionManagementType.BEAN)
public class War2Ejb implements EjbInterface {

    @Resource
    public UserTransaction ut2;


    @Override
    public UserTransaction lookupUserTransaction() throws NamingException {
        return (UserTransaction) new InitialContext().lookup("java:comp/env/" + getClass().getName() + "/ut2");
    }

    @Override
    public UserTransaction lookupOtherUserTransaction() throws NamingException {
        return (UserTransaction) new InitialContext().lookup("java:comp/env/" + getClass().getPackage().getName() + "War1Ejb/ut1");
    }


}
