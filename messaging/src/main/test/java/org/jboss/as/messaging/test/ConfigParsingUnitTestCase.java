package org.jboss.as.messaging.test;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.security.Role;
import org.jboss.as.messaging.ConfigurationElement;
import org.jboss.as.messaging.MessagingSubsystemElement;
import org.jboss.as.messaging.MessagingSubsystemParser;
import org.jboss.as.messaging.Namespace;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.ServerModelParser;
import org.jboss.as.server.StandardElementReaderRegistrar;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests of the messaging/hornetq configuration parsing.
 *
 * @author scott.stark@jboss.org
 * @version $Id$
 */
public class ConfigParsingUnitTestCase {
   private final StandardElementReaderRegistrar extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();

   /**
    * Test the stax parsing of the domain-with-messaging.xml configuration
    */
   @Test
   public void testStaxParser() {
      final ParseResult<ServerModel> parseResult = new ParseResult<ServerModel>();
      try {
         final XMLMapper mapper = createXMLMapper();
         URL configURL = getClass().getResource("/domain-with-messaging.xml");
         Assert.assertNotNull("domain-with-messaging.xml url is not null", configURL);
         System.out.println("configURL = " + configURL);
         BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
         mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(reader));
         // Validate the configuration
         MessagingSubsystemElement mse = MessagingSubsystemParser.getLastSubsystemElement();
         ConfigurationElement config = mse.getConfiguration();
         Configuration jmsConfig = config.getConfiguration();
         Assert.assertEquals("bindings-directory", "${jboss.server.data.dir}/hornetq/bindings", jmsConfig.getBindingsDirectory());
         Assert.assertEquals("journal-min-files", 10, jmsConfig.getJournalMinFiles());
         Assert.assertEquals("paging-directory", "${jboss.server.data.dir}/hornetq/paging", jmsConfig.getPagingDirectory());
         Map<String, Set<Role>> securityRoleMap = jmsConfig.getSecurityRoles();
         // Security
         Assert.assertEquals("1 security roles", 1, securityRoleMap.size());
         Set<Role> securityRoles = securityRoleMap.values().iterator().next();
         Role expectedRole = new Role("guest", true, true, false, false, true, true, false);
         Set<Role> expectedRoles = new HashSet<Role>();
         expectedRoles.add(expectedRole);
         Assert.assertEquals("guest role", expectedRoles, securityRoles);
         Map<String, TransportConfiguration> connectors = jmsConfig.getConnectorConfigurations();
         Assert.assertEquals("3 connectors", 3, connectors.size());
         // The expected connector configuration
         Map<String, Object> c0params = new HashMap<String, Object>();
         c0params.put("host", "${jboss.bind.address:localhost}");
         c0params.put("port", "${hornetq.remoting.netty.port:5445}");
         TransportConfiguration c0 = new TransportConfiguration("org.hornetq.core.remoting.impl.netty.NettyConnectorFactory",
            c0params , "netty");
         Map<String, Object> c1params = new HashMap<String, Object>();
         c1params.put("host", "${jboss.bind.address:localhost}");
         c1params.put("port", "${hornetq.remoting.netty.batch.port:5455}");
         c1params.put("batch-delay", "50");
         TransportConfiguration c1 = new TransportConfiguration("org.hornetq.core.remoting.impl.netty.NettyConnectorFactory",
            c1params , "netty-throughput");
         Map<String, Object> c2params = new HashMap<String, Object>();
         c2params.put("server-id", "${hornetq.server-id:0}");
         TransportConfiguration c2 = new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory",
            c2params , "in-vm");
         Map<String, TransportConfiguration> expectedConnectors = new HashMap<String, TransportConfiguration>();
         expectedConnectors.put(c0.getName(), c0);
         expectedConnectors.put(c1.getName(), c1);
         expectedConnectors.put(c2.getName(), c2);
         for(String connKey : expectedConnectors.keySet()) {
            TransportConfiguration tcex = expectedConnectors.get(connKey);
            TransportConfiguration tc = connectors.get(connKey);
            Assert.assertEquals(connKey, tcex, tc);
         }

         // The expected acceptor configuration
         Set<TransportConfiguration> acceptors = jmsConfig.getAcceptorConfigurations();
         Assert.assertEquals("3 acceptors", 3, acceptors.size());
                  Map<String, Object> a0params = new HashMap<String, Object>();
         a0params.put("host", "${jboss.bind.address:localhost}");
         a0params.put("port", "${hornetq.remoting.netty.port:5445}");
         TransportConfiguration a0 = new TransportConfiguration("org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory",
            a0params , "netty");
         Map<String, Object> a1params = new HashMap<String, Object>();
         a1params.put("host", "${jboss.bind.address:localhost}");
         a1params.put("port", "${hornetq.remoting.netty.batch.port:5455}");
         a1params.put("batch-delay", "50");
         a1params.put("direct-deliver", "false");
         TransportConfiguration a1 = new TransportConfiguration("org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory",
            a1params , "netty-throughput");
         Map<String, Object> a2params = new HashMap<String, Object>();
         a2params.put("server-id", "0");
         TransportConfiguration a2 = new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory",
            a2params , "in-vm");
         Map<String, TransportConfiguration> expectedAcceptors = new HashMap<String, TransportConfiguration>();
         expectedAcceptors.put(a0.getName(), a0);
         expectedAcceptors.put(a1.getName(), a1);
         expectedAcceptors.put(a2.getName(), a2);
         for(TransportConfiguration tc : acceptors) {
            TransportConfiguration tcex = expectedAcceptors.get(tc.getName());
            Assert.assertEquals(tc.getName(), tcex, tc);
         }
      }
      catch (Exception e) {
         throw new RuntimeException("domain-with-messaging.xml", e);
      }

   }

   /**
    * Create an XMLMapper that has the root {urn:jboss:domain:1.0}standalone and
    * {urn:jboss:domain:threads:1.0}subsystem {urn:jboss:domain:naming:1.0}subystem and
    * {urn:jboss:domain:remoting:1.0}subsystem parsers registered to be a NullSubsystemParser
    * 
    * @return
    * @throws Exception
    */
   protected XMLMapper createXMLMapper() throws Exception {
      XMLMapper mapper = XMLMapper.Factory.create();
      ServerModelParser domainParser = ServerModelParser.getInstance();
      mapper.registerRootElement(new QName("urn:jboss:domain:1.0", "standalone"), domainParser);
      NullSubsystemParser<Object> threadsParser = new NullSubsystemParser();
      mapper.registerRootElement(new QName("urn:jboss:domain:threads:1.0", "subsystem"), threadsParser);
      NullSubsystemParser<Object> namingParser = new NullSubsystemParser();
      mapper.registerRootElement(new QName("urn:jboss:domain:naming:1.0", "subsystem"), namingParser);
      NullSubsystemParser<Object> remotingParser = new NullSubsystemParser();
      mapper.registerRootElement(new QName("urn:jboss:domain:remoting:1.0", "subsystem"), remotingParser);
      mapper.registerRootElement(Namespace.MESSAGING_1_0.getQName(), MessagingSubsystemParser.getInstance());
      return mapper;
   }
}
