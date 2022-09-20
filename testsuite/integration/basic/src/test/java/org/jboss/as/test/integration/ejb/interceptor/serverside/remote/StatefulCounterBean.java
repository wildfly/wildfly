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
package org.jboss.as.test.integration.ejb.interceptor.serverside.remote;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

@Stateful
@Remote(SimpleCounter.class)
public class StatefulCounterBean implements SimpleCounter {

    private int count = 0;

    @Override
    public String getSimpleName() {
        return StatefulCounterBean.class.getSimpleName();
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void increment() {
        count++;
    }

    @Override
    public void decrement() {
        count--;
    }
}
