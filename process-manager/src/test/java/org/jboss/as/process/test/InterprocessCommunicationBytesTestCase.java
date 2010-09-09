/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.process.test;

import org.jboss.as.process.support.LoggingTestRunner;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(LoggingTestRunner.class)
public class InterprocessCommunicationBytesTestCase extends InterprocessCommunicationTest {

    @Override
    protected void broadcastMessage(String sender, String... messages) throws InterruptedException {
        for (String msg : messages)
            broadcastMessage(sender, msg.getBytes());
    }

    @Override
    protected void sendMessage(String sender, String recipient, String... messages) throws InterruptedException {
        for (String msg : messages)
            sendMessage(sender, recipient, msg.getBytes());
    }

}
