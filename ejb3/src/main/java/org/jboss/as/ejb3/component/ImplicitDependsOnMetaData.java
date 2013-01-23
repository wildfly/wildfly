package org.jboss.as.ejb3.component;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaData;

/**
 *
 * Metadata for implicit-depends-on
 *
 * If this is set then the EJB's views have a dependency on the EJB's start service,
 * so other components that inject the EJB will end up with an implicit dependency on the EJB
 *
 * @author Stuart Douglas
 */
public class ImplicitDependsOnMetaData extends AbstractEJBBoundMetaData {

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}
