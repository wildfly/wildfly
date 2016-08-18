/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.sar.injection.pojos;

/**
 * @author <a href="mailto:opalka.richard@gmail.com">Richard Opalka</a>
 */
abstract class AbstractLifycycleMethodsA {

    private boolean startCalled;
    private boolean stopCalled;
    private boolean createCalled;
    private boolean destroyCalled;

    public final void start() {
        startCalled = true;
    }

    public final void stop() {
        stopCalled = true;
    }

    public final void create() {
        createCalled = true;
    }

    public final void destroy() {
        destroyCalled = true;
    }

    public final boolean getStartCalled() {
        return startCalled;
    }

    public final boolean getStopCalled() {
        return stopCalled;
    }

    public final boolean getCreateCalled() {
        return createCalled;
    }

    public final boolean getDestroyCalled() {
        return destroyCalled;
    }

}
