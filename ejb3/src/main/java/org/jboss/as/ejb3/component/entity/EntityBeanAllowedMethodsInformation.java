package org.jboss.as.ejb3.component.entity;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.component.session.SessionBeanAllowedMethodsInformation;

import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class EntityBeanAllowedMethodsInformation extends SessionBeanAllowedMethodsInformation {

    public static final EntityBeanAllowedMethodsInformation INSTANCE = new EntityBeanAllowedMethodsInformation();

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);

        add(denied, InvocationType.FINDER_METHOD, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.FINDER_METHOD, MethodType.GET_PRIMARY_KEY);
        add(denied, InvocationType.FINDER_METHOD, MethodType.GET_TIMER_SERVICE);
    }

}
