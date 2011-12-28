package org.jboss.as.cmp.component;

import java.util.Set;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;

/**
 * @author Stuart Douglas
 */
public class CmpAllowedMethodsInformation extends AllowedMethodsInformation {

    public static final CmpAllowedMethodsInformation INSTANCE = new CmpAllowedMethodsInformation();

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);
        add(denied, InvocationType.ENTITY_EJB_CREATE, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.ENTITY_EJB_CREATE, MethodType.GET_PRIMARY_KEY);
    }

}
