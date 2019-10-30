/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.singleton.dependson.session;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;

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
