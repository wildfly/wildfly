package org.jboss.as.ejb3.component.stateless;

import org.jboss.as.ejb3.component.allowedmethods.DeniedMethodKey;
import org.jboss.as.ejb3.component.session.SessionBeanAllowedMethodsInformation;

import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class StatelessAllowedMethodsInformation extends SessionBeanAllowedMethodsInformation {

    public static final StatelessAllowedMethodsInformation INSTANCE = new StatelessAllowedMethodsInformation();

    @Override
    protected void setup(Set<DeniedMethodKey> denied) {
        super.setup(denied);
    }
}
