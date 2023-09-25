/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.defaultinterceptor;

/**
 * @author Stuart Douglas
 */
public interface SessionBean {

    void setPostConstructCalled();
}
