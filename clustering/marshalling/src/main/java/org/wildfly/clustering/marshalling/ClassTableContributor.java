package org.wildfly.clustering.marshalling;

import java.util.Collection;

public interface ClassTableContributor {
    Collection<Class<?>> getKnownClasses();
}
