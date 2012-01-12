package org.jboss.as.ejb3.component.entity;

import java.util.Set;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;

/**
 * @author Stuart Douglas
 */
public class EntityBeanAllowedMethodsInformation extends AllowedMethodsInformation {

    public static final EntityBeanAllowedMethodsInformation INSTANCE = new EntityBeanAllowedMethodsInformation();

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);

        add(denied, InvocationType.FINDER_METHOD, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.FINDER_METHOD, MethodType.GET_PRIMARY_KEY);
        add(denied, InvocationType.FINDER_METHOD, MethodType.GET_TIMER_SERVICE);
        add(denied, InvocationType.ENTITY_EJB_ACTIVATE, MethodType.GET_CALLER_PRINCIPLE);
        add(denied, InvocationType.ENTITY_EJB_ACTIVATE, MethodType.IS_CALLER_IN_ROLE);
        add(denied, InvocationType.ENTITY_EJB_PASSIVATE, MethodType.GET_CALLER_PRINCIPLE);
        add(denied, InvocationType.ENTITY_EJB_PASSIVATE, MethodType.IS_CALLER_IN_ROLE);
    }

}
