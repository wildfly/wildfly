/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc. and individual contributors
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

package org.jboss.as.clustering.lock;

import org.jboss.as.clustering.ClusterNode;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

class LockParamsMatcher extends ArgumentMatcher<Object[]> {
    
    public static Object[] eqLockParams(ClusterNode node, long timeout) {
        return Mockito.argThat(new LockParamsMatcher(node, timeout));
    }

    private final ClusterNode node;
    private final long timeout;

    LockParamsMatcher(ClusterNode node, long timeout) {
        this.node = node;
        this.timeout = timeout;
    }

    public void appendTo(StringBuffer buffer) {
        buffer.append("eqRemoteLockParams({\"test\",");
        buffer.append(node);
        buffer.append(',');
        buffer.append(timeout);
        buffer.append("})");
    }

    @Override
    public boolean matches(Object arg) {
        if (arg instanceof Object[]) {
            Object[] args = (Object[]) arg;
            if (args.length == 3) {
                if ("test".equals(args[0]) && node.equals(args[1]) && args[2] instanceof Long) {
                    long l = ((Long) args[2]).longValue();
                    return l >= 0 && l <= timeout;
                }
            }
        }
        return false;
    }

}