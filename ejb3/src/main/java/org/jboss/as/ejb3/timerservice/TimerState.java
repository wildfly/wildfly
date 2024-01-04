/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Timer states.
 * <p/>
 * <p/>
 * Following is the possible timer state transitions:
 * <p/>
 * <ul>
 * <li> {@link #CREATED}  - on create</li>
 * <li> {@link #CREATED} -> {@link #ACTIVE}  - when started without Tx</li>
 * <li> {@link #ACTIVE} -> {@link #CANCELED} - on cancel() without Tx</li>
 * <li> {@link #ACTIVE} -> {@link #IN_TIMEOUT} - on TimerTask run</li>
 * <li> {@link #IN_TIMEOUT} -> {@link #ACTIVE} - on Tx commit if intervalDuration > 0</li>
 * <li> {@link #IN_TIMEOUT} -> {@link #EXPIRED} -> on Tx commit if intervalDuration == 0</li>
 * <li> {@link #IN_TIMEOUT} -> {@link #RETRY_TIMEOUT} -> on Tx rollback</li>
 * <li> {@link #RETRY_TIMEOUT} -> {@link #ACTIVE} -> on Tx commit/rollback if intervalDuration > 0</li>
 * <li> {@link #RETRY_TIMEOUT} -> {@link #EXPIRED} -> on Tx commit/rollback if intervalDuration == 0</li>
 * </ul>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public enum TimerState {

    /**
     * State indicating that a timer has been created.
     */
    CREATED,

    /**
     * State indicating that the timer is active and will receive
     * any timeout notifications
     */
    ACTIVE,

    /**
     * State indicating that the timer has been cancelled and will not
     * receive any future timeout notifications
     */
    CANCELED,

    /**
     * State indicating that there aren't any scheduled timeouts for this timer
     */
    EXPIRED,

    /**
     * State indicating that the timer has received a timeout notification
     * and is processing the timeout task
     */
    IN_TIMEOUT,

    /**
     * State indicating that the timeout task has to be retried
     */
    RETRY_TIMEOUT,
    ;

    /**
     * An unmodifiable set that contains timer states {@link #IN_TIMEOUT}, {@link #RETRY_TIMEOUT},
     * {@link #CREATED} and {@link #ACTIVE}.
     */
    public static final Set<TimerState> CREATED_ACTIVE_IN_TIMEOUT_RETRY_TIMEOUT =
            Collections.unmodifiableSet(EnumSet.of(IN_TIMEOUT, RETRY_TIMEOUT, CREATED, ACTIVE));

    /**
     * An unmodifiable set that contains timer states {@link #EXPIRED} and {@link #CANCELED}.
     */
    public static final Set<TimerState> EXPIRED_CANCELED =
            Collections.unmodifiableSet(EnumSet.of(EXPIRED, CANCELED));
}
