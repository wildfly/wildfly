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

package org.jboss.as.ejb3.component.stateful;

import java.util.Set;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.component.session.SessionBeanAllowedMethodsInformation;

/**
 * @author Stuart Douglas
 */
public class StatefulAllowedMethodsInformation extends SessionBeanAllowedMethodsInformation {

    public static final StatefulAllowedMethodsInformation INSTANCE_BMT = new StatefulAllowedMethodsInformation(true);
    public static final StatefulAllowedMethodsInformation INSTANCE_CMT = new StatefulAllowedMethodsInformation(false);

    protected StatefulAllowedMethodsInformation(boolean beanManagedTransaction) {
        super(beanManagedTransaction);
    }

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);
        add(denied, InvocationType.POST_CONSTRUCT, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.PRE_DESTROY, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.SFSB_INIT_METHOD, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.TIMER_SERVICE_METHOD);
    }

}
