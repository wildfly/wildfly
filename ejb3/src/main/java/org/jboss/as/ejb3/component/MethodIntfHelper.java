/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.invocation.InterceptorContext;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MethodIntfHelper {
    // centralize this hack
    public static MethodInterfaceType of(final InterceptorContext invocation) {
        //for timer invocations there is no view, so the methodInf is attached directly
        //to the context. Otherwise we retrieve it from the invoked view
        MethodInterfaceType methodIntf = invocation.getPrivateData(MethodInterfaceType.class);
        if (methodIntf == null) {
            final ComponentView componentView = invocation.getPrivateData(ComponentView.class);
            if (componentView != null) {
                methodIntf = componentView.getPrivateData(MethodInterfaceType.class);
            } else {
                methodIntf = MethodInterfaceType.Bean;
            }
        }
        return methodIntf;
    }
}
