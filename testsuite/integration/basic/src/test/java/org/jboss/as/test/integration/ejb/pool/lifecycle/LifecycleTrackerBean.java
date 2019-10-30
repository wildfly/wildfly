/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.pool.lifecycle;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

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