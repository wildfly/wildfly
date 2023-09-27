/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.exception;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface BeanieLocal {
    void callThrowException();
    void callThrowExceptionNever();
    void throwException() throws Exception;
    void throwXmlAppException();
}
