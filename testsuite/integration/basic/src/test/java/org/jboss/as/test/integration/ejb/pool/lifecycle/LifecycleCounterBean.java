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

import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Simple bean to store callback count, which we can retrieve from arquilian test.
 * @author baranowb
 */
//singleton to share single instance between tested bean and arquilian.
@Singleton
@Startup
@Local
public class LifecycleCounterBean implements LifecycleCounter {

    private static final Logger log = Logger.getLogger(LifecycleCounterBean.class.getName());
    // TODO: add more if there is a need.
    private int preDestroyCount = 0;
    private int postCreateCount = 0;

    public int getPreDestroyCount() {
        return this.preDestroyCount;
    }

    public int getPostCreateCount() {
        return this.postCreateCount;
    }

    public void incrementPreDestroyCount() {
        this.log.info("Increment preDestroy: "+this.preDestroyCount);
        this.preDestroyCount++;
    }

    public void incrementPostCreateCount() {
        this.log.info("Increment postCreate: "+this.postCreateCount);
        this.postCreateCount++;
    }

    public void reset() {
        this.log.info("Reset callback count");
        this.preDestroyCount = 0;
        this.postCreateCount = 0;
    }

}