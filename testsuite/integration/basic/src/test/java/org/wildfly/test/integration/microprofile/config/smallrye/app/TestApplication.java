/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.config.smallrye.app;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.wildfly.test.integration.microprofile.config.smallrye.SubsystemConfigSourceTask;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 * @author Jan Stourac <jstourac@redhat.com>
 */
@ApplicationPath("/microprofile")
public class TestApplication extends Application {
    public static final String APP_PATH = "/simpleTest";
    public static final String BOOLEAN_APP_PATH = "/booleanTest";
    public static final String INTEGER_APP_PATH = "/integerTest";
    public static final String LONG_APP_PATH = "/longTest";
    public static final String FLOAT_APP_PATH = "/floatTest";
    public static final String DOUBLE_APP_PATH = "/doubleTest";
    public static final String ARRAY_SET_LIST_DEFAULT_APP_PATH = "/arraySetListDefaultTest";
    public static final String ARRAY_SET_LIST_OVERRIDE_APP_PATH = "/arraySetListOverriddenTest";
    public static final String ARRAY_SET_LIST_NO_DEF_APP_PATH = "/arraySetListNoDefTest";
    public static final String PRIORITY_APP_PATH = "/priorityTest";

    @Path(APP_PATH)
    public static class ResourceSimple {

        @Inject
        Config config;

        @Inject
        @ConfigProperty(name = "my.prop", defaultValue = "BAR")
        String prop1;

        @Inject
        @ConfigProperty(name = "my.other.prop", defaultValue = "no")
        boolean prop2;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.MY_PROP_FROM_SUBSYSTEM_PROP_NAME)
        String prop3;

        @Inject
        @ConfigProperty(name = "optional.injected.prop.that.is.not.configured")
        Optional<String> optionalProp;

        @Inject
        @ConfigProperty(name = "my.prop.from.meta", defaultValue = "Meta property not defined!")
        String metaProp;

        @Inject
        @ConfigProperty(name = "node0", defaultValue = "System property not defined!")
        String systemProperty;

        @Inject
        @ConfigProperty(name = "MPCONFIG_TEST_ENV_VAR", defaultValue = "Environment variable not defined!")
        String envVariable;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            Optional<String> foo = config.getOptionalValue("my.prop.never.defined", String.class);
            StringBuilder text = new StringBuilder();
            text.append("my.prop.never.defined = " + foo + "\n");
            text.append("my.prop = " + prop1 + "\n");
            text.append("my.other.prop = " + prop2 + "\n");
            text.append("optional.injected.prop.that.is.not.configured = " + optionalProp + "\n");
            text.append(SubsystemConfigSourceTask.MY_PROP_FROM_SUBSYSTEM_PROP_NAME + " = " + prop3 + "\n");

            text.append("my.prop.from.meta = " + metaProp + "\n");
            text.append("node0 = " + systemProperty + "\n");
            text.append("MPCONFIG_TEST_ENV_VAR = " + envVariable + "\n");

            return Response.ok(text).build();
        }
    }

    @Path(BOOLEAN_APP_PATH)
    public static class ResourceBoolean {

        @Inject
        Config config;

        @Inject
        @ConfigProperty(name = "boolTrue", defaultValue = "true")
        private boolean boolTrue;

        @Inject
        @ConfigProperty(name = "bool1", defaultValue = "1")
        private boolean bool1;

        @Inject
        @ConfigProperty(name = "boolYes", defaultValue = "YES")
        private boolean boolYes;

        @Inject
        @ConfigProperty(name = "boolY", defaultValue = "Y")
        private boolean boolY;

        @Inject
        @ConfigProperty(name = "boolOn", defaultValue = "on")
        private boolean boolOn;

        @Inject
        @ConfigProperty(name = "boolDefault", defaultValue = "yes")
        private boolean boolDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.BOOL_OVERRIDDEN_PROP_NAME, defaultValue = "badValue")
        private boolean boolOverridden;

        @Inject
        @ConfigProperty(name = "booleanDefault", defaultValue = "yes")
        private boolean booleanDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.BOOLEAN_OVERRIDDEN_PROP_NAME, defaultValue = "badValue")
        private boolean booleanOverridden;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();

            text.append("boolTrue = " + boolTrue + "\n");
            text.append("bool1 = " + bool1 + "\n");
            text.append("boolYes = " + boolYes + "\n");
            text.append("boolY = " + boolY + "\n");
            text.append("boolOn = " + boolOn + "\n");

            text.append("boolDefault = " + boolDefault + "\n");
            text.append(SubsystemConfigSourceTask.BOOL_OVERRIDDEN_PROP_NAME + " = " + boolOverridden + "\n");

            text.append("booleanDefault = " + booleanDefault + "\n");
            text.append(SubsystemConfigSourceTask.BOOLEAN_OVERRIDDEN_PROP_NAME + " = " + booleanOverridden + "\n");

            return Response.ok(text).build();
        }
    }

    @Path(INTEGER_APP_PATH)
    public static class ResourceInteger {

        @Inject
        Config config;

        @Inject
        @ConfigProperty(name = "intDefault", defaultValue = "-42")
        private int intDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.INT_OVERRIDDEN_PROP_NAME, defaultValue = "123")
        private int intOverridden;

        @Inject
        @ConfigProperty(name = "integerDefault", defaultValue = "-42")
        private Integer integerDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.INTEGER_OVERRIDDEN_PROP_NAME, defaultValue = "123")
        private Integer integerOverridden;


        @Inject
        @ConfigProperty(name = "intBadValue", defaultValue = "1.23")
        private int intBadValue;

        @Inject
        @ConfigProperty(name = "integerBadValue", defaultValue = "illegalText1.23")
        private Integer integerBadValue;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();

            text.append("intDefault = " + intDefault + "\n");
            text.append(SubsystemConfigSourceTask.INT_OVERRIDDEN_PROP_NAME + " = " + intOverridden + "\n");

            text.append("integerDefault = " + integerDefault + "\n");
            text.append(SubsystemConfigSourceTask.INTEGER_OVERRIDDEN_PROP_NAME + " = " + integerOverridden + "\n");

            text.append("intBadValue = " + intBadValue + "\n");
            text.append("integerBadValue = " + integerBadValue + "\n");

            return Response.ok(text).build();
        }
    }

    @Path(LONG_APP_PATH)
    public static class ResourceLong {

        @Inject
        Config config;

        @Inject
        @ConfigProperty(name = "longDefault", defaultValue = "-42")
        private long longDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.LONG_OVERRIDDEN_PROP_NAME, defaultValue = "123")
        private long longOverridden;

        @Inject
        @ConfigProperty(name = "longClassDefault", defaultValue = "-42")
        private Long longClassDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.LONG_CLASS_OVERRIDDEN_PROP_NAME, defaultValue = "123")
        private Long longClassOverridden;


        @Inject
        @ConfigProperty(name = "longBadValue", defaultValue = "1.23")
        private long longBadValue;

        @Inject
        @ConfigProperty(name = "longClassBadValue", defaultValue = "illegalText1.23")
        private Long longClassBadValue;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();

            text.append("longDefault = " + longDefault + "\n");
            text.append(SubsystemConfigSourceTask.LONG_OVERRIDDEN_PROP_NAME + " = " + longOverridden + "\n");

            text.append("longClassDefault = " + longClassDefault + "\n");
            text.append(SubsystemConfigSourceTask.LONG_CLASS_OVERRIDDEN_PROP_NAME + " = " + longClassOverridden + "\n");

            text.append("longBadValue = " + longBadValue + "\n");
            text.append("longClassBadValue = " + longClassBadValue + "\n");

            return Response.ok(text).build();
        }
    }

    @Path(FLOAT_APP_PATH)
    public static class ResourceFloat {

        @Inject
        Config config;

        @Inject
        @ConfigProperty(name = "floatDefault", defaultValue = "-3.14")
        private float floatDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.FLOAT_OVERRIDDEN_PROP_NAME, defaultValue = "1.618")
        private float floatOverridden;

        @Inject
        @ConfigProperty(name = "floatClassDefault", defaultValue = "-3.14e10")
        private Float floatClassDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.FLOAT_CLASS_OVERRIDDEN_PROP_NAME, defaultValue = "1.618")
        private Float floatClassOverridden;


        @Inject
        @ConfigProperty(name = "floatBadValue", defaultValue = "text1.23")
        private float floatBadValue;

        @Inject
        @ConfigProperty(name = "floatClassBadValue", defaultValue = "illegalText1.23")
        private Float floatClassBadValue;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();

            text.append("floatDefault = " + floatDefault + "\n");
            text.append(SubsystemConfigSourceTask.FLOAT_OVERRIDDEN_PROP_NAME + " = " + floatOverridden + "\n");

            text.append("floatClassDefault = " + floatClassDefault + "\n");
            text.append(SubsystemConfigSourceTask.FLOAT_CLASS_OVERRIDDEN_PROP_NAME + " = " + floatClassOverridden + "\n");

            text.append("floatBadValue = " + floatBadValue + "\n");
            text.append("floatClassBadValue = " + floatClassBadValue + "\n");

            return Response.ok(text).build();
        }
    }

    @Path(DOUBLE_APP_PATH)
    public static class ResourceDouble {

        @Inject
        Config config;

        @Inject
        @ConfigProperty(name = "doubleDefault", defaultValue = "-3.14")
        private double doubleDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.DOUBLE_OVERRIDDEN_PROP_NAME, defaultValue = "1.618")
        private double doubleOverridden;

        @Inject
        @ConfigProperty(name = "doubleClassDefault", defaultValue = "-3.14e10")
        private Double doubleClassDefault;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.DOUBLE_CLASS_OVERRIDDEN_PROP_NAME, defaultValue = "1.618")
        private Double doubleClassOverridden;


        @Inject
        @ConfigProperty(name = "doubleBadValue", defaultValue = "text1.23")
        private double doubleBadValue;

        @Inject
        @ConfigProperty(name = "doubleClassBadValue", defaultValue = "illegalText1.23")
        private Double doubleClassBadValue;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();

            text.append("doubleDefault = " + doubleDefault + "\n");
            text.append(SubsystemConfigSourceTask.DOUBLE_OVERRIDDEN_PROP_NAME + " = " + doubleOverridden + "\n");

            text.append("doubleClassDefault = " + doubleClassDefault + "\n");
            text.append(SubsystemConfigSourceTask.DOUBLE_CLASS_OVERRIDDEN_PROP_NAME + " = " + doubleClassOverridden + "\n");

            text.append("doubleBadValue = " + doubleBadValue + "\n");
            text.append("doubleClassBadValue = " + doubleClassBadValue + "\n");

            return Response.ok(text).build();
        }
    }

    @Path(PRIORITY_APP_PATH)
    public static class ResourcePriority {

        @Inject
        Config config;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.PROPERTIES_PROP_NAME0, defaultValue = "Custom file property not defined!")
        String customFileProp0;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.PROPERTIES_PROP_NAME1, defaultValue = "Custom file property not defined!")
        String customFileProp1;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.PROPERTIES_PROP_NAME2, defaultValue = "Custom file property not defined!")
        String customFileProp2;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.PROPERTIES_PROP_NAME3, defaultValue = "Custom file property not defined!")
        String customFileProp3;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.PROPERTIES_PROP_NAME4, defaultValue = "Custom file property not defined!")
        String customFileProp4;

        @Inject
        @ConfigProperty(name = SubsystemConfigSourceTask.PROPERTIES_PROP_NAME5, defaultValue = "Custom file property not defined!")
        String customFileProp5;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();

            text.append(SubsystemConfigSourceTask.PROPERTIES_PROP_NAME0 + " = " + customFileProp0 + "\n");
            text.append(SubsystemConfigSourceTask.PROPERTIES_PROP_NAME1 + " = " + customFileProp1 + "\n");
            text.append(SubsystemConfigSourceTask.PROPERTIES_PROP_NAME2 + " = " + customFileProp2 + "\n");
            text.append(SubsystemConfigSourceTask.PROPERTIES_PROP_NAME3 + " = " + customFileProp3 + "\n");
            text.append(SubsystemConfigSourceTask.PROPERTIES_PROP_NAME4 + " = " + customFileProp4 + "\n");
            text.append(SubsystemConfigSourceTask.PROPERTIES_PROP_NAME5 + " = " + customFileProp5 + "\n");

            return Response.ok(text).build();
        }
    }

    @Path(ARRAY_SET_LIST_DEFAULT_APP_PATH)
    public static class ResourceArraySetListDefaultProps {

        @Inject
        Config config;

        @Inject
        @ConfigProperty(name = "myPets", defaultValue = "horse,monkey\\,donkey")
        private String[] myArrayPets;

        @Inject
        @ConfigProperty(name = "myPets", defaultValue = "cat,lama\\,yokohama")
        private List<String> myListPets;

        @Inject
        @ConfigProperty(name = "myPets", defaultValue = "dog,mouse\\,house")
        private Set<String> mySetPets;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();

            text.append("myPets as String array = " + Arrays.toString(myArrayPets) + "\n");
            text.append("myPets as String list = " + myListPets + "\n");
            text.append("myPets as String set = " + mySetPets + "\n");

            return Response.ok(text).build();
        }
    }

    @Path(ARRAY_SET_LIST_OVERRIDE_APP_PATH)
    public static class ResourceArraySetListOverriddenProps {

        @Inject
        Config config;

        @Inject
        @ConfigProperty(name = "myPetsOverridden", defaultValue = "horse,monkey\\,donkey")
        private String[] myArrayPetsOverridden;

        @Inject
        @ConfigProperty(name = "myPetsOverridden", defaultValue = "cat,lama\\,yokohama")
        private List<String> myListPetsOverridden;

        @Inject
        @ConfigProperty(name = "myPetsOverridden", defaultValue = "dog,mouse\\,house")
        private Set<String> mySetPetsOverridden;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();

            text.append("myPetsOverridden as String array = " + Arrays.toString(myArrayPetsOverridden) + "\n");
            text.append("myPetsOverridden as String list = " + myListPetsOverridden + "\n");
            text.append("myPetsOverridden as String set = " + mySetPetsOverridden + "\n");

            return Response.ok(text).build();
        }
    }

}
