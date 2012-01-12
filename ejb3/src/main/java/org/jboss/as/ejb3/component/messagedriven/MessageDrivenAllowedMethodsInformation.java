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

    public static final MessageDrivenAllowedMethodsInformation INSTANCE = new MessageDrivenAllowedMethodsInformation();

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
