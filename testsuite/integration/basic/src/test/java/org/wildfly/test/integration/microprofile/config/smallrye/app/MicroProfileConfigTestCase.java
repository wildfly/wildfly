/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import static org.wildfly.test.integration.microprofile.config.smallrye.AssertUtils.assertTextContainsProperty;
import static org.wildfly.test.integration.microprofile.config.smallrye.HttpUtils.getContent;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.config.smallrye.SubsystemConfigSourceTask;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 * @author Jan Stourac <jstourac@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SubsystemConfigSourceTask.class)
public class MicroProfileConfigTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileConfigTestCase.war")
                .addClasses(TestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(MicroProfileConfigTestCase.class.getPackage(),
                        "microprofile-config.properties", "microprofile-config.properties");
        return war;
    }

    @ArquillianResource
    private URL url;

    private final String appContext = "microprofile";

    /**
     * Check that we get default values for properties except for one, which should have value loaded from the
     * subsystem. There is also checked that property form META-INF file and also some System Property is loaded.
     *
     * @throws Exception
     */
    @Test
    public void testGetWithConfigProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + appContext + TestApplication.APP_PATH));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);

            assertTextContainsProperty(text, "my.prop.never.defined", Optional.empty().toString());
            assertTextContainsProperty(text, "my.prop", "BAR");
            assertTextContainsProperty(text, "my.other.prop", false);
            assertTextContainsProperty(text, "optional.injected.prop.that.is.not.configured",
                    Optional.empty().toString());

            assertTextContainsProperty(text, SubsystemConfigSourceTask.MY_PROP_FROM_SUBSYSTEM_PROP_NAME,
                    SubsystemConfigSourceTask.MY_PROP_FROM_SUBSYSTEM_PROP_VALUE);
            assertTextContainsProperty(text, "node0", System.getProperty("node0"));
            assertTextContainsProperty(text, "MPCONFIG_TEST_ENV_VAR", System.getenv("MPCONFIG_TEST_ENV_VAR"));
        }
    }

    /**
     * Check boolean/Boolean type is correctly handled in regards of the default values, no default values and if it is
     * overridden.
     *
     * @throws Exception
     */
    @Test
    public void testGetBooleanProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + appContext + TestApplication.BOOLEAN_APP_PATH));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);

            assertTextContainsProperty(text, "boolTrue", true);
            assertTextContainsProperty(text, "bool1", true);
            assertTextContainsProperty(text, "boolYes", true);
            assertTextContainsProperty(text, "boolY", true);
            assertTextContainsProperty(text, "boolOn", true);

            assertTextContainsProperty(text, "boolDefault", true);
            assertTextContainsProperty(text, SubsystemConfigSourceTask.BOOL_OVERRIDDEN_PROP_NAME, true);

            assertTextContainsProperty(text, "booleanDefault", true);
            assertTextContainsProperty(text, SubsystemConfigSourceTask.BOOLEAN_OVERRIDDEN_PROP_NAME, true);
        }
    }

    /**
     * Check int/Integer type is correctly handled in regards of the default values, no default values and if it is
     * overridden.
     *
     * @throws Exception
     */
    @Test
    public void testGetIntegerProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + appContext + TestApplication.INTEGER_APP_PATH));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);

            assertTextContainsProperty(text, "intDefault", -42);
            assertTextContainsProperty(text, SubsystemConfigSourceTask.INT_OVERRIDDEN_PROP_NAME,
                    SubsystemConfigSourceTask.INTEGER_OVERRIDDEN_PROP_VALUE);

            assertTextContainsProperty(text, "integerDefault", -42);
            assertTextContainsProperty(text, SubsystemConfigSourceTask.INTEGER_OVERRIDDEN_PROP_NAME,
                    SubsystemConfigSourceTask.INTEGER_OVERRIDDEN_PROP_VALUE);

            assertTextContainsProperty(text, "intBadValue", 0);
            assertTextContainsProperty(text, "integerBadValue", "null");
        }
    }

    /**
     * Check long/Long type is correctly handled in regards of the default values, no default values and if it is
     * overridden.
     *
     * @throws Exception
     */
    @Test
    public void testGetLongProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + appContext + TestApplication.LONG_APP_PATH));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);

            assertTextContainsProperty(text, "longDefault", -42);
            assertTextContainsProperty(text, SubsystemConfigSourceTask.LONG_OVERRIDDEN_PROP_NAME, SubsystemConfigSourceTask.LONG_OVERRIDDEN_PROP_VALUE);

            assertTextContainsProperty(text, "longClassDefault", -42);
            assertTextContainsProperty(text, SubsystemConfigSourceTask.LONG_CLASS_OVERRIDDEN_PROP_NAME, SubsystemConfigSourceTask.LONG_OVERRIDDEN_PROP_VALUE);

            assertTextContainsProperty(text, "longBadValue", 0);
            assertTextContainsProperty(text, "longClassBadValue", "null");
        }
    }

    /**
     * Check float/Float type is correctly handled in regards of the default values, no default values and if it is
     * overridden.
     *
     * @throws Exception
     */
    @Test
    public void testGetFloatProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + appContext + TestApplication.FLOAT_APP_PATH));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);

            assertTextContainsProperty(text, "floatDefault", -3.14);
            assertTextContainsProperty(text, SubsystemConfigSourceTask.FLOAT_OVERRIDDEN_PROP_NAME, SubsystemConfigSourceTask.FLOAT_OVERRIDDEN_PROP_VALUE);

            assertTextContainsProperty(text, "floatClassDefault", Float.valueOf("-3.14e10"));
            assertTextContainsProperty(text, SubsystemConfigSourceTask.FLOAT_CLASS_OVERRIDDEN_PROP_NAME, SubsystemConfigSourceTask.FLOAT_OVERRIDDEN_PROP_VALUE);

            assertTextContainsProperty(text, "floatBadValue", 0.0f);
            assertTextContainsProperty(text, "floatClassBadValue", "null");
        }
    }

    /**
     * Check double/Double type is correctly handled in regards of the default values, no default values and if it is
     * overridden.
     *
     * @throws Exception
     */
    @Test
    public void testGetDoubleProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + appContext + TestApplication.DOUBLE_APP_PATH));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);

            assertTextContainsProperty(text, "doubleDefault", -3.14);
            assertTextContainsProperty(text, SubsystemConfigSourceTask.DOUBLE_OVERRIDDEN_PROP_NAME, SubsystemConfigSourceTask.DOUBLE_OVERRIDDEN_PROP_VALUE);

            assertTextContainsProperty(text, "doubleClassDefault", Double.valueOf("-3.14e10"));
            assertTextContainsProperty(text, SubsystemConfigSourceTask.DOUBLE_CLASS_OVERRIDDEN_PROP_NAME, SubsystemConfigSourceTask.DOUBLE_OVERRIDDEN_PROP_VALUE);

            assertTextContainsProperty(text, "doubleBadValue", 0.0d);
            assertTextContainsProperty(text, "doubleClassBadValue", "null");
        }
    }

    /**
     * Check String array, List and Set properties are correctly handled in regards of the default values.
     *
     * @throws Exception
     */
    @Test
    public void testGetWithArraySetListDefaultProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + appContext + TestApplication.ARRAY_SET_LIST_DEFAULT_APP_PATH));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);

            LinkedList<String> petsList = new LinkedList<>();
            petsList.add("cat");
            petsList.add("lama,yokohama");

            Set<String> petsSet = new HashSet<>();
            petsSet.add("dog");
            petsSet.add("mouse,house");

            assertTextContainsProperty(text, "myPets as String array", Arrays.toString(new String[]{"horse","monkey,donkey"}));
            assertTextContainsProperty(text, "myPets as String list", petsList);
            assertTextContainsProperty(text, "myPets as String set", petsSet); // TODO - not sure whether this is safe as Set doesn't assure order?
        }
    }

    /**
     * Check String array, List and Set properties are correctly handled if their default values are overridden.
     *
     * @throws Exception
     */
    @Test
    public void testGetWithArraySetListOverriddenProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + appContext + TestApplication.ARRAY_SET_LIST_OVERRIDE_APP_PATH));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);

            LinkedList<String> petsList = new LinkedList<>();
            petsList.add("donkey");
            petsList.add("shrek,fiona");

            Set<String> petsSet = new HashSet<>();
            petsSet.add("donkey");
            petsSet.add("shrek,fiona");

            assertTextContainsProperty(text, "myPetsOverridden as String array", Arrays.toString(new String[] {"donkey", "shrek,fiona"}));
            assertTextContainsProperty(text, "myPetsOverridden as String list", petsList);
            assertTextContainsProperty(text, "myPetsOverridden as String set", petsSet); // TODO - not sure whether this is safe as Set doesn't assure order?
//            Assert.assertTrue(text.contains("myPetsOverridden as String set = [donkey,shrek]") || text.contains("myPetsOverridden as String set = [shrek,donkey]"));
        }
    }

    /**
     * Checks that properties with same names are loaded based on their priorities defined by their sources.
     *
     * @throws Exception
     */
    @Test
    public void testPriorityOrderingProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + appContext + TestApplication.PRIORITY_APP_PATH));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);

            // Values from META-INF
            assertTextContainsProperty(text, SubsystemConfigSourceTask.PROPERTIES_PROP_NAME0, "Value prop0 from META-INF/microprofile-config.properties");
            // TODO - enable this when https://issues.jboss.org/browse/WFWIP-60 is resolved
            //assertTextContainsProperty(text, SubsystemConfigSourceTask.PROPERTIES_PROP_NAME1, SubsystemConfigSourceTask.PROP1_VALUE);
            // Value from defined system property in subsystem overrided meta-inf
            assertTextContainsProperty(text, SubsystemConfigSourceTask.PROPERTIES_PROP_NAME2, SubsystemConfigSourceTask.PROP2_VALUE);

            // fileProperty has ordinal value 100, same as default for META-INF properties, thus system property should override this
            assertTextContainsProperty(text, SubsystemConfigSourceTask.PROPERTIES_PROP_NAME3, SubsystemConfigSourceTask.PROP3_VALUE);

            // dir property should override all in this case
            assertTextContainsProperty(text, SubsystemConfigSourceTask.PROPERTIES_PROP_NAME4, "priority.prop.4 value loaded via directory config-source fileProperty4");

            // not defined anywhere...
            assertTextContainsProperty(text, SubsystemConfigSourceTask.PROPERTIES_PROP_NAME5, "Custom file property not defined!");
        }
    }
}
