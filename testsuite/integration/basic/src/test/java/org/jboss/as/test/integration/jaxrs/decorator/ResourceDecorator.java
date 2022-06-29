package org.jboss.as.test.integration.jaxrs.decorator;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

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
