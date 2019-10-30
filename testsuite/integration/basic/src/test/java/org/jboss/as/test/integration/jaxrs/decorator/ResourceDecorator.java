package org.jboss.as.test.integration.jaxrs.decorator;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Decorator
public class ResourceDecorator implements ResourceInterface {

    @Inject
    @Delegate
    @Any
    private ResourceInterface delegate;

    @Override
    public String getMessage() {
        return "DECORATED " + delegate.getMessage();
    }
}
