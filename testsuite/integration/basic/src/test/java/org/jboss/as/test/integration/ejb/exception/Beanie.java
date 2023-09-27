/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.exception;

import static jakarta.ejb.TransactionAttributeType.NEVER;
import static jakarta.ejb.TransactionAttributeType.NOT_SUPPORTED;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
public class Beanie implements BeanieLocal {
    @Resource
    private SessionContext ctx;

    public void callThrowException() {
        try {
            ctx.getBusinessObject(BeanieLocal.class).throwException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @TransactionAttribute(NEVER)
    public void callThrowExceptionNever() {
        try {
            ctx.getBusinessObject(BeanieLocal.class).throwException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @TransactionAttribute(NOT_SUPPORTED)
    public void throwException() throws Exception {
        throw new Exception("This is an app exception");
    }

    @Override
    public void throwXmlAppException() {
        throw new XmlAppException();
    }
}
