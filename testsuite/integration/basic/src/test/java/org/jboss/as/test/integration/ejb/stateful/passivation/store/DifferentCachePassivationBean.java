/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.passivation.store;

import org.jboss.as.test.integration.ejb.stateful.passivation.Bean;
import org.jboss.ejb3.annotation.Cache;

import jakarta.ejb.Local;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;

@Stateful
@Cache("another-passivating-cache")
@Local(Bean.class)
public class DifferentCachePassivationBean implements Bean {

    private boolean prePrePassivateInvoked;
    private boolean postActivateInvoked;

    @PrePassivate
    private void beforePassivate() {
        this.prePrePassivateInvoked = true;
    }

    @PostActivate
    private void afterActivate() {
        this.postActivateInvoked = true;
    }

    @Override
    public boolean wasPassivated() {
        return this.prePrePassivateInvoked;
    }

    @Override
    public boolean wasActivated() {
        return this.postActivateInvoked;
    }

    @Remove
    @Override
    public void close() {
    }
}
