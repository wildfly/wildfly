/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Remove;
import jakarta.ejb.SessionContext;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class SessionBean extends BaseBean {

    private SessionContext sessionContext;

    public EJBContext getEJBContext() {
        return sessionContext;
    }

    // TODO - Why do I need to override this method.
    public void performTests(final String beanMethod) {
        super.performTests(beanMethod);
    }

    @Resource
    public void setSessionContext(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
        performTests(DEPENDENCY_INJECTION);
    }

    @PostConstruct
    public void postConstruct() {
        performTests(LIFECYCLE_CALLBACK);
    }

    public void business() {
        performTests(BUSINESS);
    }

    @Remove
    public void remove() {
    }

    // TODO - Work out how to fit in afterCompletion.

}
