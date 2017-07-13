package org.jboss.as.test.integration.jsf.duplicateid;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jsf.duplicateid.deployment.IncludeBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test case based on reproducer for https://issues.jboss.org/browse/JBEAP-10758
 *
 * Original reproducer: https://github.com/tuner/mojarra-dynamic-include-reproducer
 * Original reproducer author: Kari Lavikka <tuner@bdb.fi>
 *
 * @author Jan Kasik <jkasik@redhat.com>
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DuplicateIDIntegrationTestCase {

    private static final Logger log = LoggerFactory.getLogger(DuplicateIDIntegrationTestCase.class);

    @ContainerResource
    private ManagementClient managementClient;

    private static final String
            HTTP = "http",
            APP_NAME = "duplicate-id-reproducer",
            WEB_XML = "web.xml",
            INDEX_XHTML = "index.xhtml",
            BUTTON_XHTML = "button.xhtml",
            COMP_XHTML = "comp.xhtml",
            FACES_CONFIG_XML = "faces-config.xml",
            COMPONENTS = "components",
            RESOURCES = "resources";

    private static final int APPLICATION_PORT = 8080;

    @Deployment(testable = false, name = APP_NAME)
    public static Archive<?> deploy() {
        final String separator = "/",
                componentTargetPathPrefix = RESOURCES.concat(separator).concat(COMPONENTS).concat(separator);
        final Package resourcePackage = IncludeBean.class.getPackage();

        WebArchive archive = ShrinkWrap.create(WebArchive.class, APP_NAME.concat(".war"))
                .addClass(IncludeBean.class)
                .addAsWebResource(resourcePackage, BUTTON_XHTML, componentTargetPathPrefix + BUTTON_XHTML)
                .addAsWebResource(resourcePackage, COMP_XHTML, componentTargetPathPrefix + COMP_XHTML)
                .addAsWebResource(resourcePackage, INDEX_XHTML)
                .addAsWebInfResource(resourcePackage, WEB_XML)
                .addAsWebInfResource(resourcePackage, FACES_CONFIG_XML);
        log.debug(archive.getContent().toString());
        return archive;
    }

    /**
     * Verify, that loading two dynamically loaded elements won't cause a server error.
     * <ol>
     *     <li>Send an initial request to obtain session id and view state.</li>
     *     <li>Verify this request went OK.</li>
     *     <li>Simulate clicking on a button labeled "Show 2" to "reveal" a text element which has assigned ID.</li>
     *     <li>Verify that second request went OK too.</li>
     *     <li>Simulate clicking on a button labeled "Show 3" to "reveal" a text element which has assigned ID. In state
     *     when the bug is not fixed, new element receives same ID and response code is 500 - server error. Thus
     *     verifying, that "clicking" went OK is crucial.</li>
     *     <li>Also verify, that respond contains both displayed dynamic elements.</li>
     * </ol>
     */
    @Test
    public void testDuplicateIDIsNotGenerated() throws IOException {
        final URL baseURL = new URL(HTTP, managementClient.getMgmtAddress(), APPLICATION_PORT,
                "/" + APP_NAME + "/" + IncludeBean.class.getPackage().getName().replaceAll("\\.", "/") + "/" + INDEX_XHTML);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            log.debug("Sending initial request to '{}'.", baseURL.toString());
            final HttpResponse initialResponse = httpClient.execute(new HttpPost(baseURL.toString()));
            assertEquals(HttpURLConnection.HTTP_OK, initialResponse.getStatusLine().getStatusCode());
            final Form initialResponseForm = new Form(initialResponse);

            final HttpResponse afterFirstClickResponse = simulateClickingOnButton(httpClient, initialResponseForm, "Show 3");
            assertEquals(HttpURLConnection.HTTP_OK, afterFirstClickResponse.getStatusLine().getStatusCode());
            final Form formAfterFirstClick = new Form(afterFirstClickResponse);

            final HttpResponse afterSecondClickResponse = simulateClickingOnButton(httpClient, formAfterFirstClick, "Show 2");
            assertEquals(HttpURLConnection.HTTP_OK, afterSecondClickResponse.getStatusLine().getStatusCode());
            final String responseString = new BasicResponseHandler().handleResponse(afterSecondClickResponse);
            assertTrue("There should be text which appears after clicking on second button in response!",
                    responseString.contains(">OutputText 2</span>"));
            assertTrue("There should be text which appears after clicking on first button in response!",
                    responseString.contains(">OutputText 3</span>"));
        }
    }

    private HttpResponse simulateClickingOnButton(HttpClient client, Form form, String buttonValue) throws IOException {
        final URL url = new URL(HTTP, managementClient.getMgmtAddress(), APPLICATION_PORT, form.getAction());
        final HttpPost request = new HttpPost(url.toString());
        final List<NameValuePair> params = new LinkedList<>();
        for (Input input : form.getInputFields()) {
            if (input.type == Input.Type.HIDDEN ||
                    (input.type == Input.Type.SUBMIT && input.getValue().equals(buttonValue))) {
                params.add(new BasicNameValuePair(input.getName(), input.getValue()));
            }
        }
        request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        log.debug("Clicking on submit button '{}'.", buttonValue);
        return client.execute(request);
    }

    private final class Form {

        static final String
                NAME = "name",
                VALUE = "value",
                INPUT = "input",
                TYPE = "type",
                ACTION = "action",
                FORM = "form";

        final HttpResponse response;
        final String action;
        final List<Input> inputFields = new LinkedList<>();

        public Form(HttpResponse response) throws IOException {
            this.response = response;
            log.debug(response.getStatusLine().toString());
            final String responseString = new BasicResponseHandler().handleResponse(response);
            final Document doc = Jsoup.parse(responseString);
            final Element form = doc.select(FORM).first();
            this.action = form.attr(ACTION);
            for (Element input : form.select(INPUT)) {
                Input.Type type = null;
                switch (input.attr(TYPE)) {
                    case "submit":
                        type = Input.Type.SUBMIT;
                        break;
                    case "hidden":
                        type = Input.Type.HIDDEN;
                        break;
                }
                inputFields.add(new Input(input.attr(NAME), input.attr(VALUE), type));
            }
        }

        public String getAction() {
            return action;
        }

        public List<Input> getInputFields() {
            return inputFields;
        }
    }

    private static final class Input {

        final String name, value;
        final Type type;

        public Input(String name, String value, Type type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public enum Type {
            HIDDEN, SUBMIT
        }
    }

}
