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
public class AddressSettingsElement extends AbstractModelElement<AddressSettingsElement> implements ServiceActivator {
   private static final long serialVersionUID = 1L;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   public AddressSettingsElement(final Location location) {
      super(location);
   }

   public AddressSettingsElement(final XMLExtendedStreamReader reader, Configuration config) throws XMLStreamException {
      super(reader);
      System.out.println("Begin " + reader.getLocation() + reader.getLocalName());
      // Handle elements
      int tag = reader.getEventType();
      String localName = null;
      do {
         tag = reader.nextTag();
         localName = reader.getLocalName();
         final Element element = Element.forName(reader.getLocalName());
         /*
            <security-setting match="#">
            <permission type="createNonDurableQueue" roles="guest"/>
            <permission type="deleteNonDurableQueue" roles="guest"/>
            <permission type="consume" roles="guest"/>
            <permission type="send" roles="guest"/>
            </security-setting>
         */
         switch (element) {
         case SECURITY_ELEMENT_NAME:
            String match = reader.getAttributeValue(0);
            Pair<String, Set<Role>> roles = parseSecurityRoles(reader, match);
            config.getSecurityRoles().put(roles.a, roles.b);
            break;
         }
      } while (reader.hasNext() && localName.equals("security-setting"));
   }

   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected void appendDifference(Collection<AbstractModelUpdate<AddressSettingsElement>> target, AddressSettingsElement other) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<AddressSettingsElement> getElementClass() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
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
      while (reader.hasNext() && reader.getLocalName().equals(Element.PERMISSION_ELEMENT_NAME.getLocalName())) {
         int tag = reader.nextTag();
         String localName = reader.getLocalName();

         if (Element.PERMISSION_ELEMENT_NAME.getLocalName().equalsIgnoreCase(localName)) {
            int count = reader.getAttributeCount();
            int rolesIndex = 1, typeIndex = 0;
            for (int n = 0; n < count; n++) {
               if (reader.getAttributeLocalName(n).equals(Element.ROLES_ATTR_NAME.getLocalName())) {
                  rolesIndex = n;
               } else if (reader.getAttributeLocalName(n).equals(Element.TYPE_ATTR_NAME.getLocalName())) {
                  typeIndex = n;
               }
            }

            List<String> roles = reader.getListAttributeValue(rolesIndex);
            String type = reader.getAttributeValue(typeIndex);
            for (String role : roles) {
               if (Element.SEND_NAME.equals(type)) {
                  send.add(role.trim());
               } else if (Element.CONSUME_NAME.equals(type)) {
                  consume.add(role.trim());
               } else if (Element.CREATEDURABLEQUEUE_NAME.equals(type)) {
                  createDurableQueue.add(role);
               } else if (Element.DELETEDURABLEQUEUE_NAME.equals(type)) {
                  deleteDurableQueue.add(role);
               } else if (Element.CREATE_NON_DURABLE_QUEUE_NAME.equals(type)) {
                  createNonDurableQueue.add(role);
               } else if (Element.DELETE_NON_DURABLE_QUEUE_NAME.equals(type)) {
                  deleteNonDurableQueue.add(role);
               } else if (Element.CREATETEMPQUEUE_NAME.equals(type)) {
                  createNonDurableQueue.add(role);
               } else if (Element.DELETETEMPQUEUE_NAME.equals(type)) {
                  deleteNonDurableQueue.add(role);
               } else if (Element.MANAGE_NAME.equals(type)) {
                  manageRoles.add(role);
               }
               if (!allRoles.contains(role.trim())) {
                  allRoles.add(role.trim());
               }
            }
         }

      }

      for (String role : allRoles) {
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
