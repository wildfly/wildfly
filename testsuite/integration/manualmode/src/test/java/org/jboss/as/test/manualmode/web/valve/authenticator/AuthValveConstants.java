/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.manualmode.web.valve.authenticator;

/**
 *
 * @author rhatlapa
 */
public class AuthValveConstants {
    
    public static final String CONTAINER = "default-jbossas";
    
    public static final Class AUTHENTICATOR = TestAuthenticator.class;
    public static final String AUTH_VALVE_JAR = "testauthvalve.jar";
    public static final String MODULENAME = "org.jboss.testvalve.authenticator";
    
    public static final String STANDARD_VALVE_JAR = "teststandardvalve.jar";
    public static final String STANDARD_VALVE_MODULE = "org.jboss.testvalve.standard";
    
    public static final String STANDARD_VALVE = "classicValve";
    
    public static final String PARAM_NAME = "testparam";

    public static final String DEFAULT_PARAM_VALUE = "default";
    public static final String GLOBAL_PARAM_VALUE = "global";
    public static final String WEB_PARAM_VALUE = "webdescriptor"; 
    public static final String STANDARD_VALVE_DEFAULT_PARAM_VALUE = "simpleValve";
    
    public static final String WEB_APP_URL = "HelloServlet";
    
    public static final String CUSTOM_AUTHENTICATOR_1 = "MYAUTH";
    public static final String CUSTOM_AUTHENTICATOR_2 = "MYAUTH_2";
   
    public static final String WEB_APP_URL_1 = "HelloServlet";
    public static final String WEB_APP_URL_2 = "hello";
    
    public static String getBaseModulePath(String modulename) {
        return "/../modules/" + modulename.replace(".", "/") + "/main";
    }
    
}
