package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.session.SessionBeanAllowedMethodsInformation;

import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class SingletonAllowedMethodsInformation extends SessionBeanAllowedMethodsInformation {

    public static final SingletonAllowedMethodsInformation INSTANCE = new SingletonAllowedMethodsInformation();

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);
    }
}
