/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
