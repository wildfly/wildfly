package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.InterceptorContext;

/**
 * @author Stuart Douglas
 */
public interface ComponentFactory {

    ManagedReference create(final InterceptorContext context);

}
