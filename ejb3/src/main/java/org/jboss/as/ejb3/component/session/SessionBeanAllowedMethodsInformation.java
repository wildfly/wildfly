package org.jboss.as.ejb3.component.session;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;

import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class SessionBeanAllowedMethodsInformation  extends AllowedMethodsInformation {

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);
        add(denied, InvocationType.SET_SESSION_CONTEXT, MethodType.GET_EJB_LOCAL_OBJECT);
        add(denied, InvocationType.SET_SESSION_CONTEXT, MethodType.GET_EJB_OBJECT);
    }
}
