package org.wildfly.clustering.marshalling;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public class DynamicClassTable extends SimpleClassTable {
    public DynamicClassTable(ClassLoader loader) {
        super(findClasses(loader));
    }

    private static Class<?>[] findClasses(ClassLoader loader) {
        List<Class<?>> classes = new LinkedList<>();
        classes.add(Serializable.class);
        classes.add(Externalizable.class);
        for (ClassTableContributor contributor: ServiceLoader.load(ClassTableContributor.class, loader)) {
            classes.addAll(contributor.getKnownClasses());
        }
        return classes.toArray(new Class<?>[classes.size()]);
    }
}
