package org.wildfly.clustering.singleton;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.clustering.marshalling.ClassTableContributor;

public class SingletonClassTableContributor implements ClassTableContributor {

    @Override
    public Collection<Class<?>> getKnownClasses() {
        return Arrays.<Class<?>>asList(SingletonValueCommand.class, StopSingletonCommand.class);
    }
}
