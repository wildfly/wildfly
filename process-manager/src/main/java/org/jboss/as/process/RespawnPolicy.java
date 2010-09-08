/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.process;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface RespawnPolicy {

    /**
     * Get the timeout for this retry attempt.
     *
     * @param retryCount the current retry count. This will be 1 for the first time and then be incremented
     * each time we try to respawn the process
     * @return a number >= 0 containing the milliseconds to wait before respawning. If -1 is
     * returned we have retried too many times and should give up.
     * @throws IllegalArgumentException if retryCount <= 0
     */
    long getTimeOutMs(int retryCount);

    class DefaultRespawnPolicy implements RespawnPolicy{
        public static final RespawnPolicy INSTANCE = new DefaultRespawnPolicy();

        private DefaultRespawnPolicy() {
        }

        @Override
        public long getTimeOutMs(int retryCount) {
            if (retryCount > 0) {
                if (retryCount < 5)
                    return 150 * retryCount;
                else if (retryCount < 10)
                    return 300 * retryCount;
                else if (retryCount < 15)
                    return 400 * retryCount;
                else
                    return -1;
            }
            else {
                throw new IllegalArgumentException("Retry count must be >= 0, was " + retryCount);
            }
        }
    }
}
