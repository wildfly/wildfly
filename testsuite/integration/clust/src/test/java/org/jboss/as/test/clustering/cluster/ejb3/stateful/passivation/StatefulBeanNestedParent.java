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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.passivation;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;

import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
public class StatefulBeanNestedParent {
    private static final Logger log = Logger.getLogger(StatefulBeanNestedParent.class);
    
    private int passivatedCalled = 0;
    private int activatedCalled = 0;
    
    public void reset() {
        passivatedCalled = 0;
        activatedCalled = 0;
    }
    
    public String getNodeName() {
        return NodeNameGetter.getNodeName();
    }
    
    public int getPassivatedCalled() {
        return passivatedCalled;
    }
    
    public int getActivatedCalled() {
        return activatedCalled;
    }
    
    @PrePassivate
    public void prePassivate() {
        passivatedCalled++;
        log.info(this.getClass().getSimpleName() + " prePassivated() called " + passivatedCalled + " times");
    }
    
    @PostActivate
    public void postActivate() {
        activatedCalled++;
        log.info(this.getClass().getSimpleName() + " postActivate() called " + activatedCalled + " times");
    }
    
}
