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
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class RecoverySuspendController implements ServerActivity {

    private final RecoveryManagerService recoveryManagerService;

    public RecoverySuspendController(RecoveryManagerService recoveryManagerService) {
        this.recoveryManagerService = recoveryManagerService;
    }

    @Override
    public void preSuspend(ServerActivityCallback serverActivityCallback) {
        recoveryManagerService.suspend();
        serverActivityCallback.done();
    }

    @Override
    public void suspended(ServerActivityCallback serverActivityCallback) {
        serverActivityCallback.done();
    }

    @Override
    public void resume() {
        recoveryManagerService.resume();
    }

}
