package org.jboss.as.messaging.jms.test;

import java.util.SortedSet;
import java.util.TreeSet;

import org.hornetq.ra.HornetQResourceAdapter;
import org.jboss.as.messaging.jms.ConnectionFactoryAttribute;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryService;
import org.junit.Test;

public class PooledConnectionFactoryAttributesTestCase extends AttributesTestBase{

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
        // these 2 props will *not* be supported since AS7 relies on vaulted passwords + expressions instead
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("passwordCodec");
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("useMaskedPassword");

        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("connectionPoolName");

        // FIXME HORNETQ-1048 we need to bind these properties to AS7 clustering subsystem
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("jgroupsChannelName");
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("jgroupsFile");

        KNOWN_ATTRIBUTES = new TreeSet<String>();
        // these are supported but it is not found by JavaBeans introspector because of the type
        // difference b/w the getter and the setters (Long vs long)
        KNOWN_ATTRIBUTES.add(Pooled.SETUP_ATTEMPTS_PROP_NAME);
        KNOWN_ATTRIBUTES.add(Pooled.SETUP_INTERVAL_PROP_NAME);
        KNOWN_ATTRIBUTES.add(Pooled.USE_JNDI_PROP_NAME);

    }

    @Test
    public void compareAS7PooledConnectionFactoryAttributesAndHornetQConnectionFactoryProperties() throws Exception {
        SortedSet<String> pooledConnectionFactoryAttributes = findAllResourceAdapterProperties(PooledConnectionFactoryDefinition.ATTRIBUTES);
        pooledConnectionFactoryAttributes.removeAll(KNOWN_ATTRIBUTES);

        SortedSet<String> hornetQRAProperties = findAllPropertyNames(HornetQResourceAdapter.class);
        hornetQRAProperties.removeAll(UNSUPPORTED_HORNETQ_RA_PROPERTIES);

        compare("AS7 PooledConnectionFactoryAttributes", pooledConnectionFactoryAttributes,
                "HornetQ Resource Adapter", hornetQRAProperties);
    }

    private static final SortedSet<String> findAllResourceAdapterProperties(ConnectionFactoryAttribute... attrs) {
        SortedSet<String> names = new TreeSet<String>();
        for (ConnectionFactoryAttribute attr : attrs) {
            if (attr.isResourceAdapterProperty()) {
                names.add(attr.getPropertyName());
            }
        }
        return names;
    }
}
