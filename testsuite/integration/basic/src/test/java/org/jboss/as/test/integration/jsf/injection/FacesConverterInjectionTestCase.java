/*
 * Copyright (c) 2020. Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jsf.injection;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.beans11.BeansDescriptor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for a FacesConverter injected using annotation=true.
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
public class FacesConverterInjectionTestCase {

    private static final String TEST_NAME = "FacesConverterInjectionTestCase";

    @ArquillianResource
    @OperateOnDeployment(TEST_NAME)
    private URL url;

    private static Asset createBeansXml(String beanDiscoveryMode) {
        return new StringAsset(Descriptors.create(BeansDescriptor.class)
                .version("1.1")
                .beanDiscoveryMode(beanDiscoveryMode)
                .exportAsString());
    }

    @Deployment(name = TEST_NAME)
    public static Archive<?> deploy() {
        final Package resourcePackage = FacesConverterInjectionTestCase.class.getPackage();
        WebArchive archive = ShrinkWrap.create(WebArchive.class, TEST_NAME + ".war")
                .addClasses(JSF23ConfigurationBean.class, URLConverter.class, URLConverter.class, UrlConverterBean.class)
                .addAsWebResource(resourcePackage, "url.xhtml", "url.xhtml")
                .addAsWebInfResource(resourcePackage, "web.xml", "web.xml")
                .addAsWebInfResource(createBeansXml("all"), "beans.xml");
        return archive;
    }

    private void test(String urlToCheck, String containsText) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpUriRequest getRequest = new HttpGet(url.toExternalForm() + "faces/url.xhtml");
            try (CloseableHttpResponse getResponse = client.execute(getRequest)) {
                MatcherAssert.assertThat("GET success", getResponse.getStatusLine().getStatusCode(), CoreMatchers.equalTo(HttpURLConnection.HTTP_OK));
                String text = EntityUtils.toString(getResponse.getEntity());
                Document doc = Jsoup.parse(text);
                Element form = doc.select("form").first();
                MatcherAssert.assertThat("form is included in the response", form, CoreMatchers.notNullValue());
                List<NameValuePair> params = new ArrayList<>();
                for (Element input : form.select("input")) {
                    String value = input.attr("value");
                    if (value != null && !value.isEmpty()) {
                        params.add(new BasicNameValuePair(input.attr("name"), value));
                    } else {
                        params.add(new BasicNameValuePair(input.attr("name"), urlToCheck));
                    }
                }
                Assert.assertFalse("Form paramaters are filled", params.isEmpty());
                URI uri = new URIBuilder().setScheme(url.getProtocol())
                        .setHost(url.getHost()).setPort(url.getPort())
                        .setPath(form.attr("action")).build();
                HttpPost postRequest = new HttpPost(uri);
                postRequest.setEntity(new UrlEncodedFormEntity(params));
                try (CloseableHttpResponse postResponse = client.execute(postRequest)) {
                    MatcherAssert.assertThat("POST success", postResponse.getStatusLine().getStatusCode(), CoreMatchers.equalTo(HttpURLConnection.HTTP_OK));
                    text = EntityUtils.toString(postResponse.getEntity());
                    MatcherAssert.assertThat("Expected text is in POST response", text, CoreMatchers.containsString(containsText));
                }
            }
        }
    }

    @Test
    public void testConverterSuccess() throws Exception {
        test("http://wildfly.org/index.html", "Valid URL.");
    }

    @Test
    public void testConverterError() throws Exception {
        test("http://wildfly.org:wrong/index.html", "Invalid URL:");
    }
}
