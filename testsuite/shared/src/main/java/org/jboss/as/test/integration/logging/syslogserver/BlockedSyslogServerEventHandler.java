/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.logging.syslogserver;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;

/**
 * Implementation of {@link SyslogServerEventHandlerIF} which is backed by a static/final {@link BlockingQueue} instance.
 *
 * @author Josef Cacek
 * @see #getQueue()
 */
public class BlockedSyslogServerEventHandler implements SyslogServerEventHandlerIF {

    private static final long serialVersionUID = -3814601581286016000L;
    private static final BlockingQueue<SyslogServerEventIF> queue = new LinkedBlockingQueue<SyslogServerEventIF>();

    public static BlockingQueue<SyslogServerEventIF> getQueue() {
        return queue;
    }

    public void event(SyslogServerIF syslogServer, SyslogServerEventIF event) {
        queue.offer(event);
    }
}
