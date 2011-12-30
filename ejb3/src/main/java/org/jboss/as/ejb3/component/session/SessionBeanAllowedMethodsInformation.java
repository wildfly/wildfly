package org.jboss.as.ejb3.component.session;

import java.util.Set;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;

/**
 * @author Stuart Douglas
 */
public class SessionBeanAllowedMethodsInformation  extends AllowedMethodsInformation {

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.GET_EJB_LOCAL_OBJECT);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.GET_EJB_OBJECT);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.GET_CALLER_PRINCIPLE);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.IS_CALLER_IN_ROLE);
        add(denied, InvocationType.DEPENDENCY_INJECTION, MethodType.GET_USER_TRANSACTION);
    }
}
