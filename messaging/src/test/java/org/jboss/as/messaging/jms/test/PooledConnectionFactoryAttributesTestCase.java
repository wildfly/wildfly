package org.jboss.as.messaging.jms.test;

import static java.beans.Introspector.getBeanInfo;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.beans.PropertyDescriptor;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hornetq.ra.HornetQResourceAdapter;
import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.as.messaging.jms.PooledConnectionFactoryAttribute;
import org.jboss.as.messaging.jms.PooledConnectionFactoryService;
import org.junit.Test;

public class PooledConnectionFactoryAttributesTestCase {

    private static final SortedSet<String> UNSUPPORTED_HORNETQ_RA_PROPERTIES;
    private static final SortedSet<String> KNOWN_ATTRIBUTES;

    static {
        UNSUPPORTED_HORNETQ_RA_PROPERTIES = new TreeSet<String>();

        // we configure discovery group using discoveryGroupName instead of individual params:
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.GROUP_ADDRESS);
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.GROUP_PORT);
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.REFRESH_TIMEOUT);
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.DISCOVERY_LOCAL_BIND_ADDRESS);
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.DISCOVERY_INITIAL_WAIT_TIMEOUT);
        // these properties must not be exposed by the AS7 messaging subsystem
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.CONNECTION_PARAMETERS);
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.CONNECTOR_CLASSNAME);
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.TRANSACTION_MANAGER_LOCATOR_CLASS);
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.TRANSACTION_MANAGER_LOCATOR_METHOD);
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("managedConnectionFactory");

        KNOWN_ATTRIBUTES = new TreeSet<String>();
        // these are supported but it is not found by JavaBeans introspector because of the type
        // difference b/w the getter and the setters (Long vs long)
        KNOWN_ATTRIBUTES.add(JMSServices.SETUP_ATTEMPTS_PROP_NAME);
        KNOWN_ATTRIBUTES.add(JMSServices.SETUP_INTERVAL_PROP_NAME);
        KNOWN_ATTRIBUTES.add(JMSServices.USE_JNDI_PROP_NAME);

    }

    @Test
    public void compareAS7PooledConnectionFactoryAttributesAndHornetQConnectionFactoryProperties() throws Exception {
        SortedSet<String> pooledConnectionFactoryAttributes = findAllResourceAdapterProperties(JMSServices.POOLED_CONNECTION_FACTORY_ATTRS);
        pooledConnectionFactoryAttributes.removeAll(KNOWN_ATTRIBUTES);

        SortedSet<String> hornetQRAProperties = findAllPropertyNames(HornetQResourceAdapter.class);
        hornetQRAProperties.removeAll(UNSUPPORTED_HORNETQ_RA_PROPERTIES);

        compare("AS7 PooledConnectionFactoryAttributes", pooledConnectionFactoryAttributes,
              "HornetQ Resource Adapter", hornetQRAProperties);
    }

    private static void compare(String name1, SortedSet<String> set1,
            String name2, SortedSet<String> set2) {
        Set<String> onlyInSet1 = new TreeSet<String>(set1);
        onlyInSet1.removeAll(set2);

        Set<String> onlyInSet2 = new TreeSet<String>(set2);
        onlyInSet2.removeAll(set1);

        if (!onlyInSet1.isEmpty() || !onlyInSet2.isEmpty()) {
            fail(String.format("in %s only: %s\nin %s only: %s", name1, onlyInSet1, name2, onlyInSet2));
        }

        assertEquals(set2, set1);
    }

    private SortedSet<String> findAllPropertyNames(Class<?> clazz) throws Exception {
        SortedSet<String> names = new TreeSet<String>();
        for (PropertyDescriptor propDesc : getBeanInfo(clazz).getPropertyDescriptors()) {
            if (propDesc == null
                || propDesc.getWriteMethod() == null) {
                continue;
            }
            names.add(propDesc.getDisplayName());
        }
        return names;
    }

    private static final SortedSet<String> findAllResourceAdapterProperties(PooledConnectionFactoryAttribute... attrs) {
        SortedSet<String> names = new TreeSet<String>();
        for (PooledConnectionFactoryAttribute attr : attrs) {
            if (attr.isResourceAdapterProperty()) {
                names.add(attr.getPropertyName());
            }
        }
        return names;
    }
}
