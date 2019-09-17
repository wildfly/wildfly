/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.clustering.ejb.infinispan;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks expiration of multiple id objects.
 *
 * @author Flavia Rainone
 */
class ExpirationTracker<I> {

    // the expiration timeout in milisseconds
    private final long timeout;
    // all expiration infos being tracked mapped by id
    private final Map<I, ExpirationInfo> trackedIds;
    // the first expiration to occur
    private ExpirationInfo firstExpiration;
    // the last expiration to occur
    private ExpirationInfo lastExpiration;
    // expiration info: a single node in a double linked list
    private class ExpirationInfo {
        ExpirationInfo nextExpiration;
        ExpirationInfo previousExpiration;
        volatile long expiration;
        final I id;

        ExpirationInfo(I id) {
            this.id = id;
        }
    }

    /**
     * Creates an ExpirationTracker with configured {@code timeout} for expiration
     * @param timeout the time it takes for an id to expire.
     */
    ExpirationTracker(Duration timeout) {
        this.timeout = timeout.toMillis();
        trackedIds = new HashMap<>();
    }

    /**
     * Tracks expiration of bean identified by {@code id} starting the count down
     * now.
     *
     * @param id the id of the bean that will expire
     */
    void trackExpiration(I id) {
        final ExpirationInfo expiration;
        synchronized (this) {
            // if we are already tracking this id, move the id to the lastExpiration
            // position, as it will be the last id to expire;
            if (trackedIds.containsKey(id)) {
                expiration = trackedIds.get(id);
                if (expiration != lastExpiration) {
                    // remove the node from the expiration linked list
                    if (expiration.nextExpiration != null) {
                        expiration.nextExpiration.previousExpiration = expiration.previousExpiration;
                    }
                    if (expiration.previousExpiration != null) {
                        expiration.previousExpiration.nextExpiration = expiration.nextExpiration;
                    } else if (firstExpiration == expiration) {
                        firstExpiration = firstExpiration.nextExpiration;
                    }
                    expiration.previousExpiration = null;
                    expiration.nextExpiration = null;
                }
            } else {
                // if we are not tracking the id at all, add it to the expiration list
                expiration = new ExpirationInfo(id);
                trackedIds.put(id, expiration);
            }
            // if the list is empty, now add a single expiration node
            if (lastExpiration == null) {
                firstExpiration = lastExpiration = expiration;
            } else if (lastExpiration != expiration) {
                // if the last expiration to occur is not current node, add it to the
                // last of the expiration list
                expiration.previousExpiration = lastExpiration;
                lastExpiration.nextExpiration = expiration;
                lastExpiration = expiration;
            }
        }
        // calculate expiration time
        synchronized(expiration) {
            expiration.expiration = System.currentTimeMillis() + timeout;
        }
    }

    /**
     * If expiration is being tracked for {@code id}, invalidates it.
     * This method does not remove the invalidated bean from this tracker and
     * it is expected that expiration will be reescheduled or canceled in the future.
     *
     * @param id the id whose expiration, if existent, will be invalidated
     */
    void invalidateExpiration(I id) {
        final ExpirationInfo expiration;
        synchronized (this) {
            // if we are already tracking this id, mark its expiration to -1
            if (trackedIds.containsKey(id)) {
                expiration = trackedIds.get(id);
                synchronized (expiration) {
                    expiration.expiration = -1;
                }
            }
        }
    }

    /**
     * Retry expiration of a failed to expire id. If id is already being
     * tracked, do nothing; if not, add it to this tracker as the next to
     * expire id.
     *
     * @param id the id of the bean that failed to expire
     */
    void retryExpiration(I id) {
        synchronized (this) {
            // do nothing if bean is already being tracked
            if (trackedIds.containsKey(id)) {
                return;
            }
            final ExpirationInfo expirationInfo = new ExpirationInfo(id);
            // bean is expired already
            synchronized (expirationInfo) {
                expirationInfo.expiration = 0;
            }
            expirationInfo.nextExpiration = firstExpiration;
            if (firstExpiration != null) {
                firstExpiration.previousExpiration = expirationInfo;
            }
            firstExpiration = expirationInfo;
            if (lastExpiration == null) {
                lastExpiration = expirationInfo;
            }
            trackedIds.put(id, expirationInfo);
        }
    }

    /**
     * Stop tracking the expiration of the bean identified by {@code id},
     * regardless of whether is has already expired or not.
     *
     * @param id the id of the bean that will no longer expire
     */
    void forget(I id) {
        synchronized (this) {
            if (!trackedIds.containsKey(id)) {
                return;
            }
            final ExpirationInfo expirationInfo = trackedIds.remove(id);
            if (expirationInfo.previousExpiration != null) {
                expirationInfo.previousExpiration.nextExpiration = expirationInfo.nextExpiration;
            }
            if (expirationInfo.nextExpiration != null) {
                expirationInfo.nextExpiration.previousExpiration = expirationInfo.previousExpiration;
            }
            if (firstExpiration == expirationInfo) {
                firstExpiration = expirationInfo.nextExpiration;
            }
            if (lastExpiration == expirationInfo) {
                lastExpiration = expirationInfo.previousExpiration;
            }
        }
    }

    /**
     * Returns an id that has expired. Automatically removes the expired id from
     * this tracker.
     *
     * @param currentTimeInMillis the current time in milliseconds (provided to
     *                            prevent multiple calls to System.currentTimeInMillis())
     *
     * @return the id of the expired bean; {@code null} if no id has expired
     */
    I getExpiredId(long currentTimeInMillis) {
        synchronized (this) {
            // skip any expiration that is invalidated (i.e., expiration is -1)
            ExpirationInfo expiration = this.firstExpiration;
            while (expiration != null && readExpiration(expiration) == -1) {
                expiration = expiration.nextExpiration;
            }
            // if first expiration has expired
            if (expiration != null && readExpiration(expiration) <= currentTimeInMillis) {
                final I expiredId = expiration.id;
                if (expiration.previousExpiration != null) {
                    expiration.previousExpiration.nextExpiration = expiration.nextExpiration;
                }
                if (expiration.nextExpiration != null) {
                    expiration.nextExpiration.previousExpiration = expiration.previousExpiration;
                }
                if (firstExpiration == expiration) { // it might not be case if expiration.expiration is -1
                    firstExpiration = expiration.nextExpiration;
                }
                if (lastExpiration == expiration) { // dont set it to null if expiration.previousExpiration is not null
                    lastExpiration = expiration.previousExpiration;
                }
                trackedIds.remove(expiredId);
                return expiredId;
            }
            return null;
        }
    }

    private long readExpiration(ExpirationInfo info) {
        synchronized(info) {
            return info.expiration;
        }
    }

    /**
     * Returns all ids whose expiration is being tracked.
     *
     * @return an  {@code unmodifiable set} containing all beans.
     */
    Collection<I> getTrackedIds() {
        return Collections.unmodifiableSet(trackedIds.keySet());
    }

    /**
     * Returns the next expiration time to occur.
     * @return when will the first expiration occur in milliseconds; returns
     *         {@code -1} if no ids are being tracked. Notice that the expiration
     *         may have already occurred or may still occur (i.e., this tracker
     *         does not compare the expiration with current time; it just
     *         returns the expiration of the oldest id being tracked).
     */
    synchronized long getNextExpirationInMillis() {
        ExpirationInfo firstExpiration = this.firstExpiration;
        long expiration = -1;
        while (firstExpiration != null && (expiration = readExpiration(firstExpiration)) == -1)
            firstExpiration = firstExpiration.nextExpiration;
        return expiration;
    }

}
