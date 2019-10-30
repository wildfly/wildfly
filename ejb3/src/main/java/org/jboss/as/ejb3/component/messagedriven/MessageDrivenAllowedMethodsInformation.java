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

package org.jboss.as.ejb3.component.messagedriven;

import java.util.Set;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;

/**
 * @author Stuart Douglas
 */
public class MessageDrivenAllowedMethodsInformation extends AllowedMethodsInformation {

    public static final MessageDrivenAllowedMethodsInformation INSTANCE_BMT = new MessageDrivenAllowedMethodsInformation(true);
    public static final MessageDrivenAllowedMethodsInformation INSTANCE_CMT = new MessageDrivenAllowedMethodsInformation(false);

    protected MessageDrivenAllowedMethodsInformation(boolean beanManagedTransaction) {
        super(beanManagedTransaction);
    }

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.GET_CALLER_PRINCIPLE);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.IS_CALLER_IN_ROLE);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.GET_USER_TRANSACTION);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.GET_TIMER_SERVICE);
        add(denied, InvocationType.POST_CONSTRUCT, MethodType.GET_CALLER_PRINCIPLE);
        add(denied, InvocationType.PRE_DESTROY, MethodType.GET_CALLER_PRINCIPLE);
        add(denied, InvocationType.POST_CONSTRUCT, MethodType.IS_CALLER_IN_ROLE);
        add(denied, InvocationType.PRE_DESTROY, MethodType.IS_CALLER_IN_ROLE);
    }
}
