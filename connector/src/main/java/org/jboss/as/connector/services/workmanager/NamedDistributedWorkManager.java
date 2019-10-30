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

package org.jboss.as.connector.services.workmanager;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkListener;
import java.util.concurrent.CountDownLatch;

import org.jboss.jca.core.spi.security.SecurityIntegration;
import org.jboss.jca.core.workmanager.DistributedWorkManagerImpl;

/**
 * A named WorkManager.
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class NamedDistributedWorkManager extends DistributedWorkManagerImpl {

    private final boolean elytronEnabled;


    /**
     * Constructor
     * @param name The name of the WorkManager
     */
    public NamedDistributedWorkManager(String name, final boolean elytronEnabled) {
        super();
        setName(name);
        this.elytronEnabled = elytronEnabled;
    }
    protected WildflyWorkWrapper createWorKWrapper(SecurityIntegration securityIntegration, Work work,
                                                   ExecutionContext executionContext, WorkListener workListener, CountDownLatch startedLatch,
                                                   CountDownLatch completedLatch) {
        return new WildflyWorkWrapper(this, securityIntegration, work, executionContext, workListener,
                startedLatch, completedLatch, System.currentTimeMillis());
    }
    public boolean isElytronEnabled() {
        return elytronEnabled;
    }
}
