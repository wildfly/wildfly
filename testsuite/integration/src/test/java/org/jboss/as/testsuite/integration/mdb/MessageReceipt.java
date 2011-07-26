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

package org.jboss.as.testsuite.integration.mdb;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.jms.MessageListener;
import java.util.HashMap;
import java.util.Map;

/**
 * User: jpai
 */
@Singleton
public class MessageReceipt {

    private Map<String, Integer> messageReceipts = new HashMap<String, Integer>();

    public void messageReceived(MessageListener messageListener) {
        final String className = messageListener.getClass().getName();
        Integer count = this.messageReceipts.get(className);
        if (count == null) {
            count = new Integer(0);
        }
        this.messageReceipts.put(messageListener.getClass().getName(), count + 1);
    }

    @Lock (value = LockType.READ)
    public int getMessageCount(final String mdbClassName) {
        final Integer count = this.messageReceipts.get(mdbClassName);
        return count == null ? 0 : count;
    }
}
