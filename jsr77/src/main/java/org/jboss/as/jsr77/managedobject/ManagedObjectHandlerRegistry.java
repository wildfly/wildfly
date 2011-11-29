/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.jsr77.managedobject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.as.controller.ModelController;
import org.jboss.as.jsr77.subsystem.Constants;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ManagedObjectHandlerRegistry {

    private final Map<String, Handler> handlers;


    public static final ManagedObjectHandlerRegistry INSTANCE = new ManagedObjectHandlerRegistry();

    private ManagedObjectHandlerRegistry() {
        Map<String, Handler> handlers = new HashMap<String, Handler>();
        handlers.put(JVMHandler.J2EE_TYPE, JVMHandler.INSTANCE);
        handlers.put(J2EEDomainHandler.J2EE_TYPE, J2EEDomainHandler.INSTANCE);
        handlers.put(J2EEServerHandler.J2EE_TYPE, J2EEServerHandler.INSTANCE);
        handlers.put(J2EEDeployedObjectHandlers.J2EE_TYPE_J2EE_APPLICATION, J2EEDeployedObjectHandlers.INSTANCE);
        handlers.put(J2EEDeployedObjectHandlers.J2EE_TYPE_APP_CLIENT_MODULE, J2EEDeployedObjectHandlers.INSTANCE);
        handlers.put(J2EEDeployedObjectHandlers.J2EE_TYPE_EJB_MODULE, J2EEDeployedObjectHandlers.INSTANCE);
        handlers.put(J2EEDeployedObjectHandlers.J2EE_TYPE_RA_MODULE, J2EEDeployedObjectHandlers.INSTANCE);
        handlers.put(J2EEDeployedObjectHandlers.J2EE_TYPE_WEB_MODULE, J2EEDeployedObjectHandlers.INSTANCE);
        this.handlers = Collections.unmodifiableMap(handlers);
    }

    public boolean isMyDomain(ObjectName objectName) {
        if (!objectName.isDomainPattern()) {
            return objectName.getDomain().equals(Constants.JMX_DOMAIN);
        }

        Pattern p = Pattern.compile(objectName.getDomain().replace("*", ".*"));
        return p.matcher(Constants.JMX_DOMAIN).matches();
    }

    public Set<ObjectName> queryNames(ModelController controller, ObjectName name, QueryExp query){
        Set<ObjectName> result = new HashSet<ObjectName>();
        for (Handler handler : getHandlers(name)) {
            result.addAll(handler.queryObjectNames(new ModelReader(controller), name, query));
        }
        return result;
    }

    public Object getAttribute(ModelController controller, ObjectName name, String attribute) throws InstanceNotFoundException, MBeanException, AttributeNotFoundException {
        Handler handler = handlers.get(name.getKeyProperty(Handler.J2EE_TYPE));
        if (handler == null) {
            throw new InstanceNotFoundException("No mbean found with ObjectName " + name);
        }
        return handler.getAttribute(new ModelReader(controller), name, attribute);
    }


    public int getMBeanCount(ModelController controller) {
        return queryNames(controller, null, null).size();
    }

    public boolean isRegistered(ModelController controller, ObjectName name) {
        Handler handler = handlers.get(name.getKeyProperty(Handler.J2EE_TYPE));
        if (handler == null) {
            return false;
        }
        return handler.queryObjectNames(new ModelReader(controller), name, null).contains(name);
    }

    public MBeanInfo getMBeanInfo(ModelController controller, ObjectName name) throws InstanceNotFoundException {
        Handler handler = handlers.get(name.getKeyProperty(Handler.J2EE_TYPE));
        if (handler == null) {
            throw new InstanceNotFoundException("No mbean found with ObjectName " + name);
        }
        return handler.getMBeanInfo(new ModelReader(controller), name);
    }

    private Set<Handler> getHandlers(final ObjectName name){
        if (name == null) {
            return getHandlersForName(IllAcceptAnythingNameMatcher.INSTANCE);
        }

        if (!isMyDomain(name)) {
            return Collections.emptySet();
        }

        String property = name.getKeyProperty(Handler.J2EE_TYPE);
        if (property != null) {
            if (property.contains("*")) {
                return getHandlersForName(new WildcardPatternNameMatcher(Pattern.compile(property.replace("*", ".*"))));
            }
            return getHandlersForName(new ExactNameMatcher(property));
        }

        if (name.isPropertyListPattern()) {
            return getHandlersForName(IllAcceptAnythingNameMatcher.INSTANCE);
        }

        return Collections.emptySet();
    }

    private Set<Handler> getHandlersForName(NameMatcher matcher){
        Set<Handler> result = null;
        for (Entry<String, Handler> handlerEntry : this.handlers.entrySet()) {
            if (matcher.matches(handlerEntry.getKey())) {
                if (result == null) {
                    result = new HashSet<Handler>();
                }
                result.add(handlerEntry.getValue());
            }
        }
        return result == null ? Collections.<Handler>emptySet() : result;
    }

    private interface NameMatcher {
        boolean matches(String candidate);
    }

    private static class ExactNameMatcher implements NameMatcher {
        final String search;

        public ExactNameMatcher(String search) {
            this.search = search;
        }

        public boolean matches(String candidate) {
            return search.matches(candidate);
        }
    }

    private static class IllAcceptAnythingNameMatcher implements NameMatcher {
        private static final IllAcceptAnythingNameMatcher INSTANCE = new IllAcceptAnythingNameMatcher();
        public boolean matches(String candidate) {
            return true;
        }
    }

    private static class WildcardPatternNameMatcher implements NameMatcher {
        final Pattern pattern;

        public WildcardPatternNameMatcher(Pattern pattern) {
            this.pattern = pattern;
        }

        public boolean matches(String candidate) {
            return pattern.matcher(candidate).matches();
        }
    }

//    public static void main(String[] args) {
//        ManagedObjectHandlerRegistry registry = new ManagedObjectHandlerRegistry();
//        System.out.println(registry.isMyDomain(createObjectName(JMX_DOMAIN + ":j2eeType=JVM")));
//        System.out.println(registry.isMyDomain(createObjectName("jboss.*:j2eeType=JVM")));
//        System.out.println(registry.isMyDomain(createObjectName("*:j2eeType=JVM")));
//        System.out.println(registry.isMyDomain(createObjectName("x*:j2eeType=JVM")));
//
//        System.out.println(registry.getHandlers(createObjectName(JMX_DOMAIN + ":j2eeType=JVM"))); //Should add just JVM
//        System.out.println(registry.getHandlers(createObjectName(JMX_DOMAIN + ":j2eeType=J*")));  //Should add any starting with J
//        System.out.println(registry.getHandlers(createObjectName(JMX_DOMAIN + ":j2eeType=*")));   //Should add all
//        System.out.println(registry.getHandlers(createObjectName(JMX_DOMAIN + ":*")));            //Should add all
//        System.out.println(registry.getHandlers(createObjectName(JMX_DOMAIN + ":j2eeType=None")));//Should add none
//        System.out.println(registry.getHandlers(createObjectName(JMX_DOMAIN + ":j2eeType=N*")));//Should add none
//        System.out.println(registry.getHandlers(createObjectName(JMX_DOMAIN + ":thing=true")));    //Should add none
//        System.out.println(registry.getHandlers(createObjectName(JMX_DOMAIN + ":j2eeType=None,*")));   //Should add none
//        System.out.println(registry.getHandlers(createObjectName(JMX_DOMAIN + ":j2eeType=N*,*")));   //Should add none
//    }

}
