/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.session;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;

import org.jboss.as.test.integration.ejb.singleton.dependson.mdb.CallCounterProxy;
import org.jboss.logging.Logger;

/**
 * Base for session beans.
 *
 * @author baranowb
 */
//@DependsOn("CallCounterProxy")
public abstract class BeanBase implements Trigger {
    protected final Logger logger = Logger.getLogger(getClass());
    @EJB
    protected CallCounterProxy counter;

    @PostConstruct
    public void postConstruct() {
        this.logger.trace("Session.postConstruct");
        this.counter.setPostConstruct();
    }

    @PreDestroy
    public void preDestroy() {
        this.logger.trace("Session.preDestroy");
        this.counter.setPreDestroy();
    }
}
