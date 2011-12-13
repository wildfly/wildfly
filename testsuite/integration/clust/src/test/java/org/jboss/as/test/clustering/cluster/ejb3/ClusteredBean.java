/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb3;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.ejb.PrePassivate;
import javax.ejb.Stateful;
import javax.ejb.StatefulTimeout;

import org.jboss.ejb3.annotation.Clustered;

/**
 * @author Paul Ferraro
 *
 */
@Stateful
@StatefulTimeout(value = 1000L, unit = TimeUnit.MILLISECONDS)
@Clustered
public class ClusteredBean implements Serializable {
    private static final long serialVersionUID = 2910170208821101469L;

    public static volatile boolean preDestroy = false;
    public static volatile boolean prePassivate = false;
    private int count = 0;
    
    @PreDestroy
    public void preDestroy() {
        preDestroy = true;
    }

    @PrePassivate
    public void prePassivate() {
        prePassivate = true;
    }

    public int increment() {
        return count++;
    }
}
