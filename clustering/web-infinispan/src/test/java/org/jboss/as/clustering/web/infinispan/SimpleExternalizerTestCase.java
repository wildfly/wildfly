package org.jboss.as.clustering.web.infinispan;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.as.clustering.SimpleMarshalledValue;
import org.jboss.as.clustering.infinispan.io.SimpleExternalizer;
import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.junit.Test;

public class SimpleExternalizerTestCase {
    @Test
    public void load() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (SimpleExternalizer<?> externalizer: ServiceLoader.load(SimpleExternalizer.class)) {
            classes.add(externalizer.getTargetClass());
        }
        assertTrue(classes.contains(DistributableSessionMetadata.class));
        assertTrue(classes.contains(SimpleMarshalledValue.class));
    }

}
