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

import javax.ejb.EJB;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Stateful;

import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
@Clustered
@Stateful
public class StatefulBean implements StatefulBeanRemote {
    private static Logger log = Logger.getLogger(StatefulBean.class);    
    private int number;
    private String passivatedBy = "unknown";
    private String actIfIsNode;
    private int postActivateCalled = 0;
    private int prePassivateCalled = 0;
    
    @EJB
    private StatefulBeanNested beanNested;

    @EJB(lookup = "java:global/cluster-passivation-test-helper/StatefulBeanDeepNested!org.jboss.as.test.clustering.cluster.ejb3.stateful.passivation.StatefulBeanDeepNestedRemote")
    private StatefulBeanDeepNestedRemote beanNestedRemote;
    
    /**
     * Getting number.
     */
    public int getNumber() {
        return number;
    }

    public String incrementNumber() {
        number++;
        log.info("Incrementing number: " + Integer.toString(number));
        return NodeNameGetter.getNodeName();
    }

    /**
     * Setting number and returns node name where the method was called.
     */
    public String setNumber(int number) {
        log.info("Setting number: " + Integer.toString(number));
        this.number = number;
        return NodeNameGetter.getNodeName();
    }

    public String getPassivatedBy() {
        return this.passivatedBy;
    }

    public void setPassivationNode(String nodeName) {
        this.actIfIsNode = nodeName;
    }

    @PrePassivate
    public void prePassivate() {
        prePassivateCalled++;
        log.info("Passivating with number: " + number + " and was passivated by " + getPassivatedBy() + ", prePassivate method called " + prePassivateCalled + " times");

        // when we should act on passivation - we change value of isPassivated variable
        if (NodeNameGetter.getNodeName().equals(actIfIsNode)) {
            passivatedBy = NodeNameGetter.getNodeName();
            log.info("I'm node " + actIfIsNode + " => changing passivatedBy to " + passivatedBy);
        }
    }

    @PostActivate
    public void postActivate() {
        postActivateCalled++;
        log.info("Activating with number: " + number + " and was passivated by " + getPassivatedBy() + ", postActivate method called " + postActivateCalled + " times");
    }
    
    public void resetNestedBean() {
        beanNested.reset();
        beanNested.resetDeepNested();
        beanNestedRemote.reset();
    }
    public int getNestedBeanActivatedCalled() {
        return beanNested.getActivatedCalled();
    }
    public int getNestedBeanPassivatedCalled() {
        return beanNested.getPassivatedCalled();
    }
    public int getDeepNestedBeanActivatedCalled() {
        return beanNested.getDeepNestedActivatedCalled();
    }
    public int getDeepNestedBeanPassivatedCalled() {
        return beanNested.getDeepNestedPassivatedCalled();
    }
    public String getNestedBeanNodeName() {
        return beanNested.getNodeName();
    }
    public int getRemoteNestedBeanPassivatedCalled() {
        return beanNestedRemote.getPassivatedCalled();
    }
    public int getRemoteNestedBeanActivatedCalled() {
        return beanNestedRemote.getActivatedCalled();
    }
    public String getRemoteNestedBeanNodeName() {
        return beanNestedRemote.getNodeName();
    }
}
