package org.jboss.as.clustering.ejb3.cache.backing.infinispan;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

import org.jboss.as.clustering.SimpleMarshalledValue;
import org.jboss.as.clustering.infinispan.io.SimpleExternalizer;
import org.jboss.as.network.ClientMapping;
import org.jboss.ejb.client.SessionID;
import org.junit.Test;

public class SimpleExternalizerTestCase {
    @Test
    public void load() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (SimpleExternalizer<?> externalizer: ServiceLoader.load(SimpleExternalizer.class)) {
            classes.add(externalizer.getTargetClass());
        }
        assertTrue(classes.contains(UUID.class));
        assertTrue(classes.contains(ClientMapping.class));
        assertTrue(classes.contains(SessionID.class));
        assertTrue(classes.contains(SimpleMarshalledValue.class));
    }
}
