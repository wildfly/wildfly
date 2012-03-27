/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.ejb.shutdown;

import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * @author Jaikiran Pai
 */
@Singleton
@Startup
@Remote(RemoteEcho.class)
@DependsOn({"RealEcho", "LatchBean"})
public class ShutdownBean {

    @EJB(lookup = "java:module/RealEcho")
    private RemoteEcho realEcho;


    @EJB(lookup = "java:module/LatchBean")
    private RemoteLatch latch;


    /**
     * Wait for the remote call before shutting down
     */
    @PreDestroy
    private void shutdown() throws InterruptedException {
        latch.setEchoMessage(this.realEcho.echo("hello"));
        LatchBean.getShutDownLatch().await(20, TimeUnit.SECONDS);
    }


}
