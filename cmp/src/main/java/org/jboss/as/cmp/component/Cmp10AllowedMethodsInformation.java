package org.jboss.as.cmp.component;

import java.util.Set;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;

/**
 * @author Stuart Douglas
 */
public class Cmp10AllowedMethodsInformation extends CmpAllowedMethodsInformation {

    public static final Cmp10AllowedMethodsInformation INSTANCE = new Cmp10AllowedMethodsInformation();

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);
        add(denied, InvocationType.ENTITY_EJB_CREATE, MethodType.GET_TIMER_SERVICE);
    }

}
