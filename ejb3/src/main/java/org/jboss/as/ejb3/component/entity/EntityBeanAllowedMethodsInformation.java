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

package org.jboss.as.ejb3.component.entity;

import java.util.Set;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;

/**
 * @author Stuart Douglas
 */
public class EntityBeanAllowedMethodsInformation extends AllowedMethodsInformation {

    public static final EntityBeanAllowedMethodsInformation INSTANCE = new EntityBeanAllowedMethodsInformation();

    protected EntityBeanAllowedMethodsInformation() {
        super(false);
    }

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);

        add(denied, InvocationType.FINDER_METHOD, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.FINDER_METHOD, MethodType.GET_PRIMARY_KEY);
        add(denied, InvocationType.FINDER_METHOD, MethodType.GET_TIMER_SERVICE);
        add(denied, InvocationType.ENTITY_EJB_ACTIVATE, MethodType.GET_CALLER_PRINCIPLE);
        add(denied, InvocationType.ENTITY_EJB_ACTIVATE, MethodType.IS_CALLER_IN_ROLE);
        add(denied, InvocationType.ENTITY_EJB_PASSIVATE, MethodType.GET_CALLER_PRINCIPLE);
        add(denied, InvocationType.ENTITY_EJB_PASSIVATE, MethodType.IS_CALLER_IN_ROLE);
    }

}
