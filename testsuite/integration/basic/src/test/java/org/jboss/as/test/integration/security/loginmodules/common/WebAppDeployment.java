package org.jboss.as.test.integration.security.loginmodules.common;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.AbstractLoginModuleTestServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.test.integration.security.loginmodules.common.Utils.applyUpdates;

/**
 * Created by IntelliJ IDEA.
 * User: jlanik
 * Date: 11/25/11
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebAppDeployment {

   private String deploymentName;

   private String modelControllerHost = "localhost";

   private int modelControllerPort = 9999;

   private Class servletClass;
   
   private Class loginModuleClass;

   private List<Class> classes;

   private WebArchive war;

   private Map<String, String> moduleOptionsCache = new HashMap<String, String>();

   private boolean setUp = false;
   
   private Logger log = Logger.getLogger(WebAppDeployment.class);


   public WebAppDeployment(String deploymentName, Class servletClass, Class loginModule, Class... classes){
      this.deploymentName = deploymentName;
      this.servletClass = servletClass;
      this.loginModuleClass = loginModule;
      this.classes = Arrays.asList(classes);
   }

   public WebAppDeployment(String deploymentName, Class servletClass, Class loginModule, String modelControllerHost, int modelControllerPort, Class... classes){
      this.deploymentName = deploymentName;
      this.servletClass = servletClass;
      this.loginModuleClass = loginModule;
      this.modelControllerHost = modelControllerHost;
      this.modelControllerPort = modelControllerPort;
      this.classes = Arrays.asList(classes);
   }


   public String getDeploymentName() {
      return deploymentName;
   }

   public String getSecurityDomainName(){
      return deploymentName + "-domain";
   }

   public String getUrl(){
      return "http://localhost:8080/" + deploymentName;  
   }

   private void setup(){
      log.debug("start setup()");

      try {
         // create required security domains
         createSecurityDomain();
      } catch (Exception e) {
         log.error("updateSecurityDomain() throws exception: " + e.getMessage());
         throw new RuntimeException(e);
      }

      war = ShrinkWrap.create(WebArchive.class, deploymentName + ".war");
      war.addClass(servletClass);
      
      // common servlet superclass
      war.addClass(AbstractLoginModuleTestServlet.class);

      ClassLoader tccl = Thread.currentThread().getContextClassLoader();

      war.addAsWebResource(tccl.getResource("web-secure.war/login.jsp"), "login.jsp");
      war.addAsWebResource(tccl.getResource("web-secure.war/error.jsp"), "error.jsp");

      File jbossWeb;
      try {
         jbossWeb = File.createTempFile("jboss-web","xml");
         jbossWeb.deleteOnExit();
         PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(jbossWeb)));
         writer.println("<?xml version=\"1.0\"?>");
         writer.println("<jboss-web>");
         writer.println("<security-domain>" + getSecurityDomainName() + "</security-domain>");
         writer.println("</jboss-web>");
         writer.close();
      } catch (IOException e) {
         throw new RuntimeException("temporary file could not be created or modified by the test");
      }

      war.addAsWebInfResource(jbossWeb,"jboss-web.xml");

      for(Class clazz : classes){
         war.addClass(clazz);
      }

      war.setWebXML(tccl.getResource("web-secure.war/web.xml"));

      setUp = true;
      log.debug("setup completed");
   }


   private void createSecurityDomain() throws Exception {
      log.debug("entering createSecurityDomain()");
      final ModelControllerClient client = getModelControllerClient();

      List<ModelNode> updates = new ArrayList<ModelNode>();
      ModelNode op = new ModelNode();

      op.get(OP).set(ADD);
      op.get(OP_ADDR).add(SUBSYSTEM, "security");
      op.get(OP_ADDR).add(SECURITY_DOMAIN, getSecurityDomainName());
      updates.add(op);

      op = new ModelNode();
      op.get(OP).set(ADD);
      op.get(OP_ADDR).add(SUBSYSTEM, "security");
      op.get(OP_ADDR).add(SECURITY_DOMAIN, getSecurityDomainName());
      op.get(OP_ADDR).add(Constants.AUTHENTICATION, Constants.CLASSIC);

      ModelNode loginModule = op.get(Constants.LOGIN_MODULES).add();
      loginModule.get(ModelDescriptionConstants.CODE).set(loginModuleClass.getName());
      loginModule.get(FLAG).set("required");
      op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

      ModelNode moduleOptions = loginModule.get("module-options");
      for (Map.Entry<String, String> entry : moduleOptionsCache.entrySet()) {
         moduleOptions.get(entry.getKey()).set(entry.getValue());
         log.debug("module option added: " + entry.getKey() + "=" + entry.getValue());
      }

      updates.add(op);
      applyUpdates(updates, client);

      //clear cache
      moduleOptionsCache = new HashMap<String, String>();
      
      log.debug("leaving createSecurityDomain()");
   }

   private ModelControllerClient getModelControllerClient() throws UnknownHostException{
      return ModelControllerClient.Factory.create(InetAddress.getByName(modelControllerHost), modelControllerPort,
         org.jboss.as.arquillian.container.Authentication.getCallbackHandler());
   }

   public void addModuleOption(String name, String value){
      moduleOptionsCache.put(name, value);
   }

   public String getModelControllerHost() {
      return modelControllerHost;
   }

   public int getModelControllerPort() {
      return modelControllerPort;
   }

   public Class getServletClass() {
      return servletClass;
   }

   public List<Class> getClasses() {
      return classes;
   }

   public WebArchive getWar() {
      if(!setUp){
         setup();
      }
      return war;
   }
}
