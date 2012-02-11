/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.timerservice;

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
}
