/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.manualmode.web.valve.authenticator;

import org.jboss.as.test.manualmode.web.valve.ValveConstants;

/**
 *
 * @author rhatlapa
 */
public class AuthValveConstants extends ValveConstants {
        
    public static final Class AUTHENTICATOR = TestAuthenticator.class;
    public static final String AUTH_VALVE_JAR = "testauthvalve.jar";
    public static final String MODULENAME = "org.jboss.testvalve.authenticator";
    
    public static final String AUTH_VALVE_DEFAULT_PARAM_VALUE = "auth-default";
   
    public static final String CUSTOM_AUTHENTICATOR_1 = "MYAUTH";
    public static final String CUSTOM_AUTHENTICATOR_2 = "MYAUTH_2";
   
    public static final String WEB_APP_URL_1 = "HelloServlet";
    public static final String WEB_APP_URL_2 = "hello";
    
    public static String getBaseModulePath(String modulename) {
        return "/../modules/" + modulename.replace(".", "/") + "/main";
    }
    
}
