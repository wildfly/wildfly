package org.jboss.as.controller.descriptions.common;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public final class ControllerResolver {
    static final String RESOURCE_NAME = ControllerResolver.class.getPackage().getName() + ".LocalDescriptions";


    public static ResourceDescriptionResolver getResolver(final String... keyPrefix) {
        return getResolver(false, keyPrefix);
    }

    public static ResourceDescriptionResolver getResolver(boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.').append(kp);
            } else {
                prefix.append(kp);
            }
        }

        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, ControllerResolver.class.getClassLoader(), true, useUnprefixedChildTypes);
    }
}
