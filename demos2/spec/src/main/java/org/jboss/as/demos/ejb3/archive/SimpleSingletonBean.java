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

package org.jboss.as.demos.ejb3.archive;

import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

/**
 * @author Jaikiran Pai
 */
@Singleton
@Local(SimpleSingletonLocal.class)
public class SimpleSingletonBean implements SimpleSingletonLocal {

    private int count;

    private static int numInstancesCreated;

    public SimpleSingletonBean() {
        numInstancesCreated++;
    }

    @Override
    public int getBeanInstanceCount() {
        return this.numInstancesCreated;
    }

    @Override
    public void increment() {
        this.count++;
    }

    @Override
    @Lock(value = LockType.READ)
    public int getCount() {
        return this.count;
    }

    @Override
    public void doNothing() {

    }
}
