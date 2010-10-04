package org.jboss.as.messaging;

import org.hornetq.api.core.Pair;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.security.Role;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class SecuritySettingsElement extends AbstractModelElement<SecuritySettingsElement> implements ServiceActivator {
   private static final long serialVersionUID = 1L;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");


   public SecuritySettingsElement(final XMLExtendedStreamReader reader, Configuration config) throws XMLStreamException {
      log.tracef("Begin %s:%s", reader.getLocation(), reader.getLocalName());
      // Handle elements
      int tag = reader.getEventType();
      String localName = null;
      do {
         tag = reader.nextTag();
         localName = reader.getLocalName();
         final org.jboss.as.messaging.Element element = org.jboss.as.messaging.Element.forName(reader.getLocalName());
         /*
            <security-setting match="#">
            <permission type="createNonDurableQueue" roles="guest"/>
            <permission type="deleteNonDurableQueue" roles="guest"/>
            <permission type="consume" roles="guest"/>
            <permission type="send" roles="guest"/>
            </security-setting>
         */
         switch (element) {
         case SECURITY_SETTING:
            String match = reader.getAttributeValue(0);
            Pair<String, Set<Role>> roles = parseSecurityRoles(reader, match);
            config.getSecurityRoles().put(roles.a, roles.b);
            break;
         }
      } while (reader.hasNext() && localName.equals(Element.SECURITY_SETTING.getLocalName()));
      log.tracef("End %s:%s", reader.getLocation(), reader.getLocalName());
   }

   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<SecuritySettingsElement> getElementClass() {
      return SecuritySettingsElement.class;
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void activate(ServiceActivatorContext serviceActivatorContext) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public Set<TransportConfiguration> getTransportConfiguration() {
      return null;  //To change body of created methods use File | Settings | File Templates.
   }

   public Pair<String, Set<Role>> parseSecurityRoles(final XMLExtendedStreamReader reader, String match)
      throws XMLStreamException {


      HashSet<Role> securityRoles = new HashSet<Role>();

      Pair<String, Set<Role>> securityMatch = new Pair<String, Set<Role>>(match, securityRoles);

      ArrayList<String> send = new ArrayList<String>();
      ArrayList<String> consume = new ArrayList<String>();
      ArrayList<String> createDurableQueue = new ArrayList<String>();
      ArrayList<String> deleteDurableQueue = new ArrayList<String>();
      ArrayList<String> createNonDurableQueue = new ArrayList<String>();
      ArrayList<String> deleteNonDurableQueue = new ArrayList<String>();
      ArrayList<String> manageRoles = new ArrayList<String>();
      ArrayList<String> allRoles = new ArrayList<String>();

      int tag = reader.getEventType();
      String localName = null;
      do {
         tag = reader.nextTag();
         localName = reader.getLocalName();
         if(localName.equals(Element.PERMISSION_ELEMENT_NAME.getLocalName()) == false)
            break;
         final Element element = Element.forName(reader.getLocalName());

         List<String> roles = null;
         String type = null;
         final int count = reader.getAttributeCount();
         for (int i = 0; i < count; i++) {
            if (reader.getAttributeNamespace(i) != null) {
               throw unexpectedAttribute(reader, i);
            } else {
               final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
               switch (attribute) {
               case ROLES_ATTR_NAME:
                  roles = reader.getListAttributeValue(i);
                  break;
               case TYPE_ATTR_NAME:
                  type = reader.getAttributeValue(i);
                  break;
               default:
                  throw unexpectedAttribute(reader, i);
               }
            }
         }


         for (String role : roles) {
            if (Attribute.SEND_NAME.getLocalName().equals(type)) {
               send.add(role.trim());
            } else if (Attribute.CONSUME_NAME.getLocalName().equals(type)) {
               consume.add(role.trim());
            } else if (Attribute.CREATEDURABLEQUEUE_NAME.getLocalName().equals(type)) {
               createDurableQueue.add(role);
            } else if (Attribute.DELETEDURABLEQUEUE_NAME.getLocalName().equals(type)) {
               deleteDurableQueue.add(role);
            } else if (Attribute.CREATE_NON_DURABLE_QUEUE_NAME.getLocalName().equals(type)) {
               createNonDurableQueue.add(role);
            } else if (Attribute.DELETE_NON_DURABLE_QUEUE_NAME.getLocalName().equals(type)) {
               deleteNonDurableQueue.add(role);
            } else if (Attribute.CREATETEMPQUEUE_NAME.getLocalName().equals(type)) {
               createNonDurableQueue.add(role);
            } else if (Attribute.DELETETEMPQUEUE_NAME.getLocalName().equals(type)) {
               deleteNonDurableQueue.add(role);
            } else if (Attribute.MANAGE_NAME.getLocalName().equals(type)) {
               manageRoles.add(role);
            }
            if (!allRoles.contains(role.trim())) {
               allRoles.add(role.trim());
            }
         }
         // Scan to element end
         reader.discardRemainder();
      } while(reader.hasNext());

   for(String role : allRoles) {
      securityRoles.add(new Role(role,
         send.contains(role),
         consume.contains(role),
         createDurableQueue.contains(role),
         deleteDurableQueue.contains(role),
         createNonDurableQueue.contains(role),
         deleteNonDurableQueue.contains(role),
         manageRoles.contains(role)));
   }

   return securityMatch;
}

}
