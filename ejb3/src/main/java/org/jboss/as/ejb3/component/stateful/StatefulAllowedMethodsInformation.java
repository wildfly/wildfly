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

    public static final StatefulAllowedMethodsInformation INSTANCE = new StatefulAllowedMethodsInformation();

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);
        add(denied, InvocationType.POST_CONSTRUCT, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.PRE_DESTROY, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.SFSB_INIT_METHOD, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.TIMER_SERVICE_METHOD);
    }

}
