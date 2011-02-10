package org.jboss.as.messaging.test;

import org.junit.Test;

/**
 * Tests of the messaging/hornetq configuration parsing.
 *
 * @author scott.stark@jboss.org
 */
public class ConfigParsingUnitTestCase {

    private final String namespace = "urn:jboss:domain:messaging:1.0";

    /**
    * Test the stax parsing of the standalone-with-messaging.xml configuration
    */
   @Test
   public void testStandaloneStaxParser() {
      try {

          // FIXME re-implement with detyped handlers
//         final XMLMapper mapper = createXMLMapper();
//         URL configURL = getClass().getResource("/standalone-with-messaging.xml");
//         Assert.assertNotNull("standalone-with-messaging.xml url is not null", configURL);
//         System.out.println("configURL = " + configURL);
//         BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
//         List<ModelNode> updates = new ArrayList<ModelNode>();
//         mapper.parseDocument(updates, XMLInputFactory.newInstance().createXMLStreamReader(reader));
//
//         final ServerModel model = new ServerModel();
//         for(final AbstractServerModelUpdate<?> update : updates) {
//             model.update(update);
//         }
//         MessagingSubsystemElement subsystem = (MessagingSubsystemElement) model.getProfile().getSubsystem(namespace);
//         Assert.assertNotNull(subsystem);
//
//         Assert.assertFalse(subsystem.isPersistenceEnabled());
//         Assert.assertEquals("bindings-directory", "hornetq/bindings", subsystem.getBindingsDirectory().getPath());
//         Assert.assertEquals("journal-type", JournalType.NIO, subsystem.getJournalType());
//         Assert.assertEquals("journal-min-files", 2, subsystem.getJournalMinFiles());
//         Assert.assertEquals("journal-file-size", 102400, subsystem.getJournalFileSize());
//         Assert.assertEquals("paging-directory", "hornetq/paging", subsystem.getPagingDirectory().getPath());
//         Map<String, Set<Role>> securityRoleMap = new HashMap<String, Set<Role>>();
//         for(SecuritySettingsElement sec : subsystem.getSecuritySettings()) {
//             securityRoleMap.put(sec.getMatch(), sec.getRoles());
//         }
//
//         // Security
//         Assert.assertEquals("1 security roles", 1, securityRoleMap.size());
//         Set<Role> securityRoles = securityRoleMap.values().iterator().next();
//         Role expectedRole = new Role("guest", true, true, false, false, true, true, false);
//         Set<Role> expectedRoles = new HashSet<Role>();
//         expectedRoles.add(expectedRole);
//         Assert.assertEquals("guest role", expectedRoles, securityRoles);
//
//         // Connectors
//         final Map<String, AbstractTransportElement<?>> connectors = new HashMap<String, AbstractTransportElement<?>>();
//         for(AbstractTransportElement<?> connector : subsystem.getConnectors()) {
//             Assert.assertNotNull(connector.getName());
//             connectors.put(connector.getName(), connector);
//         }
//         Assert.assertEquals("4 connectors", 4, connectors.size());
//
//         Assert.assertEquals("netty", connectors.get("netty").getSocketBindingRef());
//         Assert.assertEquals("netty-throughput", connectors.get("netty-throughput").getSocketBindingRef());
//         Assert.assertNull(connectors.get("in-vm").getSocketBindingRef());
//
//         Assert.assertEquals("generic", connectors.get("generic").getSocketBindingRef());
//         Assert.assertEquals("org.jboss.test.ConnectorFactory", connectors.get("generic").getFactoryClassName());
//
//         // The expected acceptor configuration
//         final Map<String, AbstractTransportElement<?>> acceptors = new HashMap<String, AbstractTransportElement<?>>();
//         for(final AbstractTransportElement<?> acceptor : subsystem.getAcceptors()) {
//             Assert.assertNotNull(acceptor.getName());
//             acceptors.put(acceptor.getName(), acceptor);
//         }
//         Assert.assertEquals("4 acceptors", 4, acceptors.size());
//
//         Assert.assertEquals("netty", acceptors.get("netty").getSocketBindingRef());
//         Assert.assertEquals("netty-throughput", acceptors.get("netty-throughput").getSocketBindingRef());
//         Assert.assertNull(acceptors.get("in-vm").getSocketBindingRef());
//
//         Assert.assertEquals("generic", acceptors.get("generic").getSocketBindingRef());
//         Assert.assertEquals("org.jboss.test.AcceptorFactory", acceptors.get("generic").getFactoryClassName());
      }
      catch (Exception e) {
         throw new RuntimeException("standalone-with-messaging.xml", e);
      }
      finally {
          //
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
//   protected XMLMapper createXMLMapper() throws Exception {
//      XMLMapper mapper = XMLMapper.Factory.create();
//      mapper.registerRootElement(new QName("urn:jboss:domain:1.0", "standalone"), ModelXmlParsers.SERVER_XML_READER);
//      mapper.registerRootElement(new QName(namespace, "subsystem"), MessagingSubsystemParser.getInstance());
//      return mapper;
//   }
}
