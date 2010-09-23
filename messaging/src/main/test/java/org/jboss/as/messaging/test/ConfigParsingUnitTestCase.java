package org.jboss.as.messaging.test;

import org.hornetq.core.config.Configuration;
import org.jboss.as.messaging.ConfigurationElement;
import org.jboss.as.messaging.MessagingSubsystemElement;
import org.jboss.as.messaging.MessagingSubsystemParser;
import org.jboss.as.messaging.Namespace;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.Standalone;
import org.jboss.as.model.StandaloneParser;
import org.jboss.as.server.StandardElementReaderRegistrar;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: starksm Date: Sep 14, 2010 Time: 7:08:11 AM To change this template use File |
 * Settings | File Templates.
 */
public class ConfigParsingUnitTestCase {
   private final StandardElementReaderRegistrar extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();

   /**
    * Test the stax
    */
   @Test
   public void testStaxParser() {
      final ParseResult<Standalone> parseResult = new ParseResult<Standalone>();
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

      }
      catch (Exception e) {
         throw new RuntimeException("domain-with-messaging.xml", e);
      }

   }

   /**
    * Create an XMLMapper that has the root {urn:jboss:domain:1.0}standalone and
    * {urn:jboss:domain:threads:1.0}subsystem parsers registered to be a NullSubsystemParser
    * 
    * @return
    * @throws Exception
    */
   protected XMLMapper createXMLMapper() throws Exception {
      XMLMapper mapper = XMLMapper.Factory.create();
      StandaloneParser domainParser = StandaloneParser.getInstance();
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
