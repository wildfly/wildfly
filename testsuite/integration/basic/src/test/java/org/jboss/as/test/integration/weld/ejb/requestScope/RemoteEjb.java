package org.jboss.as.test.integration.weld.ejb.requestScope;

import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Stateless
public class RemoteEjb implements RemoteInterface {

    @Inject
    private RequestScopedBean bean;

    @Override
    public String getMessage() {
        return bean.getMessage();
    }
}
