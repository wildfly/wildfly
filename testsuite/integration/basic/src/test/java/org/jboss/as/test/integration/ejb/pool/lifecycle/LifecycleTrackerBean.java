/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.pool.lifecycle;


import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.jboss.logging.Logger;

/**
 * Lifecycle tracking singleton bean
 *
 * @author baranowb
 * @author Jaikiran Pai - Updates related to https://issues.jboss.org/browse/WFLY-1506
 */
@Singleton
@Startup
@Remote(LifecycleTracker.class)
public class LifecycleTrackerBean implements LifecycleTracker {

    private static final Logger logger = Logger.getLogger(LifecycleTrackerBean.class);

    private List<String> postConstructedBeans;
    private List<String> preDestroyedBeans;

    @PostConstruct
    private void initialize() {
        postConstructedBeans = new ArrayList<>();
        preDestroyedBeans = new ArrayList<>();
    }

    @Override
    public void trackPostConstructOn(String beanImplementationClassName) {
        if (beanImplementationClassName == null) {
            return;
        }
        this.postConstructedBeans.add(beanImplementationClassName);
    }

    @Override
    public void trackPreDestroyOn(String beanImplementationClassName) {
        if (beanImplementationClassName == null) {
            return;
        }
        this.preDestroyedBeans.add(beanImplementationClassName);
    }

    @Override
    public void clearState() {
        logger.trace("Clearing state on " + this);
        this.postConstructedBeans.clear();
        this.preDestroyedBeans.clear();
    }

    @Override
    public boolean wasPostConstructInvokedOn(String beanImplClassName) {
        return this.postConstructedBeans.contains(beanImplClassName);
    }

    @Override
    public boolean wasPreDestroyInvokedOn(String beanImplClassName) {
        return this.preDestroyedBeans.contains(beanImplClassName);
    }

    @PreDestroy
    private void destroy() {
        this.clearState();
    }

}
