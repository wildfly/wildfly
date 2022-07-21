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

package org.jboss.as.ee.component.interceptors;

/**
 * Marker enum that can be used to identify special types of invocations
 *
 * @author Stuart Douglas
 */
public enum InvocationType {
    TIMER("timer"),
    REMOTE("remote"),
    ASYNC("async"),
    MESSAGE_DELIVERY("messageDelivery"),
    SET_ENTITY_CONTEXT("setEntityContext"),
    UNSET_ENTITY_CONTEXT("unsetEntityContext"),
    POST_CONSTRUCT("postConstruct"),
    PRE_DESTROY("preDestroy"),
    DEPENDENCY_INJECTION("setSessionContext"),
    SFSB_INIT_METHOD("stateful session bean init method"),
    FINDER_METHOD("entity bean finder method"),
    HOME_METHOD("entity bean home method"),
    ENTITY_EJB_CREATE("entity bean ejbCreate method"),
    ENTITY_EJB_ACTIVATE("entity bean ejbActivate method"),
    ENTITY_EJB_PASSIVATE("entity bean ejbPassivate method"),
    ENTITY_EJB_EJB_LOAD("entity bean ejbLoad method"),
    CONCURRENT_CONTEXT("ee concurrent invocation"),

    ;

    private final String label;

    InvocationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
