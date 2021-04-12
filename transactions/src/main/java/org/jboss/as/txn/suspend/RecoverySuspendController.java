/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.suspend;

import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Listens for notifications from a {@code SuspendController} and a {@code ProcessStateNotifier} and reacts
 * to them by {@link RecoveryManagerService#suspend() suspending} or {@link RecoveryManagerService#resume() resuming}
 * the {@link RecoveryManagerService}.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class RecoverySuspendController implements ServerActivity, PropertyChangeListener {

    private final RecoveryManagerService recoveryManagerService;
    private boolean suspended;
    private boolean running;

    public RecoverySuspendController(RecoveryManagerService recoveryManagerService) {
        this.recoveryManagerService = recoveryManagerService;
    }

    /**
     * {@link RecoveryManagerService#suspend() Suspends} the {@link RecoveryManagerService}.
     */
    @Override
    public void preSuspend(ServerActivityCallback serverActivityCallback) {
        synchronized (this) {
            suspended = true;
        }
        recoveryManagerService.suspend();
        serverActivityCallback.done();
    }

    @Override
    public void suspended(ServerActivityCallback serverActivityCallback) {
        serverActivityCallback.done();
    }

    /**
     * {@link RecoveryManagerService#resume() Resumes} the {@link RecoveryManagerService} if the current
     * process state {@link ControlledProcessState.State#isRunning() is running}. Otherwise records that
     * the service can be resumed once a {@link #propertyChange(PropertyChangeEvent) notification is received} that
     * the process state is running.
     */
    @Override
    public void resume() {
        boolean doResume;
        synchronized (this) {
            suspended = false;
            doResume = running;
        }
        if (doResume) {
            resumeRecovery();
        }
    }

    /**
     * Receives notifications from a {@code ProcessStateNotifier} to detect when the process has reached a
     * {@link ControlledProcessState.State#isRunning()}  running state}, reacting to them by
     * {@link RecoveryManagerService#resume() resuming} the {@link RecoveryManagerService} if we haven't been
     * {@link #preSuspend(ServerActivityCallback) suspended}.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        boolean doResume;
        synchronized (this) {
            ControlledProcessState.State newState = (ControlledProcessState.State) evt.getNewValue();
            running = newState.isRunning();
            doResume = running && !suspended;
        }
        if (doResume) {
            resumeRecovery();
        }

    }

    private void resumeRecovery() {
        recoveryManagerService.resume();
    }
}
