/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.manualmode.web.valve;

import org.jboss.as.test.manualmode.web.valve.*;

/**
 *
 * @author rhatlapa
 */
public class ValveConstants {
    
    public static final String CONTAINER = "default-jbossas";
    
    public static final Class VALVE_CLASS = TestValve.class;
    
    public static final String STANDARD_VALVE_JAR = "teststandardvalve.jar";
    public static final String STANDARD_VALVE_MODULE = "org.jboss.testvalve.standard";
    
    public static final String STANDARD_VALVE_NAME = "classicValve";
    
    public static final String PARAM_NAME = "testparam";

    public static final String DEFAULT_PARAM_VALUE = "default";
    public static final String GLOBAL_PARAM_VALUE = "global";
    public static final String WEB_PARAM_VALUE = "webdescriptor"; 
    public static final String STANDARD_VALVE_DEFAULT_PARAM_VALUE = "simpleValve";
    
    public static final String WEB_APP_URL = "HelloServlet";
    
    public static String getBaseModulePath(String modulename) {
        return "/../modules/" + modulename.replace(".", "/") + "/main";
    }
    
}
