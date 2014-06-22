package org.jboss.as.messaging.jms.test;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hornetq.core.config.Configuration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.PathDefinition;
import org.junit.Test;

public class ConfigurationAttributesTestCase extends AttributesTestBase {

    private static final SortedSet<String> UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES;
    private static final SortedSet<String> KNOWN_ATTRIBUTES;
    private static final HashMap<String, String> DODGY_NAME = new HashMap<String, String>();

    static {
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES = new TreeSet<String>();
        //List type stuff we dont care about
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("acceptorConfigurations");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("addressesSettings");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("bridgeConfigurations");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("broadcastGroupConfigurations");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("clusterConfigurations");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("connectorConfigurations");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("connectorServiceConfigurations");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("discoveryGroupConfigurations");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("divertConfigurations");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("queueConfigurations");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("groupingHandlerConfiguration");

        //stuff we dont want supported
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("fileDeployerScanPeriod");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("fileDeploymentEnabled");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("securityRoles");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("securityInvalidationInterval");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("passwordCodec");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("journalBufferSize_AIO");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("journalBufferTimeout_AIO");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("journalMaxIO_AIO");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("journalBufferSize_NIO");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("journalBufferTimeout_NIO");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("journalMaxIO_NIO");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("name");
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("maskPassword");
        // messaging protocols are automatically resolved by HornetQ using a ServiceLoader
        UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES.add("resolveProtocols");

        //stuff we arent bothered about
        KNOWN_ATTRIBUTES = new TreeSet<String>();
        KNOWN_ATTRIBUTES.add("journalBufferSize");
        KNOWN_ATTRIBUTES.add("journalBufferTimeout");
        KNOWN_ATTRIBUTES.add("journalMaxIo");
        KNOWN_ATTRIBUTES.add("securityDomain");
        KNOWN_ATTRIBUTES.add("securityInvalidationInterval");
        KNOWN_ATTRIBUTES.add("liveConnectorRef");
        KNOWN_ATTRIBUTES.add("clustered");
        KNOWN_ATTRIBUTES.add("statisticsEnabled");
        KNOWN_ATTRIBUTES.add("overrideInVmSecurity");

        //where we have slightly different names between as7/HornetQ
        DODGY_NAME.put("allowFailback", "allowAutoFailBack");
        DODGY_NAME.put("connectionTtlOverride", "connectionTTLOverride");
        DODGY_NAME.put("persistIdCache", "persistIDCache");
        DODGY_NAME.put("perfBlastPages", "journalPerfBlastPages");
        DODGY_NAME.put("idCacheSize", "IDCacheSize");
        DODGY_NAME.put("jmxDomain", "JMXDomain");
        DODGY_NAME.put("jmxManagementEnabled", "JMXManagementEnabled");
        DODGY_NAME.put("asyncConnectionExecutionEnabled", "enabledAsyncConnectionExecution");
        DODGY_NAME.put("pageMaxConcurrentIo", "pageMaxConcurrentIO");
        DODGY_NAME.put("failoverOnShutdown", "failoverOnServerShutdown");
        DODGY_NAME.put("wildCardRoutingEnabled", "wildcardRoutingEnabled");
        DODGY_NAME.put("remotingInterceptors", "interceptorClassNames");
        DODGY_NAME.put("remotingIncomingInterceptors", "incomingInterceptorClassNames");
        DODGY_NAME.put("remotingOutgoingInterceptors", "outgoingInterceptorClassNames");
    }

    @Test
    public void compareAS7ConfigurationHornetQConfigurationProperties() throws Exception {
        SortedSet<String> attributes = findAllConfigurationProperties(CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES);
        for (String path : PathDefinition.PATHS.keySet()) {
            attributes.add(path);
        }
        convert(attributes);
        attributes.removeAll(KNOWN_ATTRIBUTES);

        SortedSet<String> configurationProperties = findAllPropertyNames(Configuration.class);
        configurationProperties.removeAll(UNSUPPORTED_HORNETQ_CONFIG_PROPERTIES);

        compare("AS7 Configuration Attributes", attributes,
                "ConfigurationImpl", configurationProperties);
    }

    private void convert(SortedSet<String> attributes) {
        SortedSet<String> newAttributes = new TreeSet<String>();
        for (String attribute : attributes) {
            String[] split = attribute.split("-");
            StringBuilder newString = new StringBuilder();
            for (int i = 0, splitLength = split.length; i < splitLength; i++) {
                String s = split[i];
                if(i > 0) {
                    String camelVersion = Character.toUpperCase(s.charAt(0)) + s.substring(1);
                    newString.append(camelVersion);
                }
                else {
                    newString.append(s);
                }
            }
            String name = newString.toString();
            if(DODGY_NAME.containsKey(name)) {
                name = DODGY_NAME.get(name);
            }
            newAttributes.add(name);
        }
        attributes.clear();
        attributes.addAll(newAttributes);
    }


    private static final SortedSet<String> findAllConfigurationProperties(AttributeDefinition... attrs) throws Exception{
        SortedSet<String> names = new TreeSet<String>();
        for (AttributeDefinition attr : attrs) {
            String name = attr.getName();
            names.add(name);
        }
        return names;
    }
}
