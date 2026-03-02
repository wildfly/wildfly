/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jsf;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.junit.jupiter.api.Test;

/**
 * Weld numberguess example converted to a test
 *
 * @author Stuart Douglas
 */
public abstract class AbstractJSFFailoverTestCase extends AbstractClusteringTestCase {

    static Pattern smallestPattern = Pattern.compile("<span id=\"numberGuess:smallest\">([^<]+)</span>");
    static Pattern biggestPattern = Pattern.compile("<span id=\"numberGuess:biggest\">([^<]+)</span>");
    static Pattern remainingPattern = Pattern.compile("You have (\\d+) guesses remaining.");
    static Pattern viewStatePattern = Pattern.compile("name=\"jakarta\\.faces\\.ViewState\"[^>]*value=\"([^\"]*)\"|value=\"([^\"]*)\"[^>]*name=\"jakarta\\.faces\\.ViewState\"");

    /**
     * Parses the response page and headers for a cookie, Jakarta Server Faces view state and the numberguess game status.
     */
    private static NumberGuessState parseState(HttpResponse response, String sessionId) throws IllegalStateException, IOException {
        Matcher matcher;

        NumberGuessState state = new NumberGuessState();
        String responseString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

        // FIXME Move these eventually to org.jboss.logging.Logger.Level.DEBUG once the intermittent
        // failures in [ProtoStream]JSFFailoverTestCase are fully resolved
        log.infof("Parsing response string for JSF state: %s", responseString);

        Map.Entry<String, String> sessionRouteEntry = parseSessionRoute(response);
        state.sessionId = (sessionRouteEntry != null) ? sessionRouteEntry.getKey() : sessionId;

        matcher = smallestPattern.matcher(responseString);
        if (matcher.find()) {
            state.smallest = matcher.group(1);
        }

        matcher = biggestPattern.matcher(responseString);
        if (matcher.find()) {
            state.biggest = matcher.group(1);
        }

        matcher = remainingPattern.matcher(responseString);
        if (matcher.find()) {
            state.remainingGuesses = matcher.group(1);
        }

        matcher = viewStatePattern.matcher(responseString);
        if (matcher.find()) {
            // N.B. The pattern matches both possible attribute orders (name/value or value/name),
            // so we check group(1) first (name comes before value), then fall back to group(2) (value comes before name)
            state.jsfViewState = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }

        log.infof("Parsed JSF state: %s", state);

        return state;
    }

    /**
     * Creates an HTTP POST request with a number guess.
     */
    private static HttpUriRequest buildPostRequest(String url, String sessionId, String viewState, String guess) {
        HttpPost post = new HttpPost(url);

        List<NameValuePair> list = new LinkedList<>();

        list.add(new BasicNameValuePair("jakarta.faces.ViewState", viewState));
        list.add(new BasicNameValuePair("numberGuess", "numberGuess"));
        list.add(new BasicNameValuePair("numberGuess:guessButton", "Guess"));
        list.add(new BasicNameValuePair("numberGuess:inputGuess", guess));

        post.setEntity(new StringEntity(URLEncodedUtils.format(list, StandardCharsets.UTF_8), ContentType.APPLICATION_FORM_URLENCODED));
        if (sessionId != null) {
            post.setHeader("Cookie", "JSESSIONID=" + sessionId);
        }

        log.infof("Built HTTP POST request: %s", post);

        return post;
    }

    /**
     * Creates an HTTP GET request, with a potential JSESSIONID cookie.
     */
    private static HttpUriRequest buildGetRequest(String url, String sessionId) {
        HttpGet request = new HttpGet(url);
        if (sessionId != null) {
            request.addHeader("Cookie", "JSESSIONID=" + sessionId);
        }

        log.infof("Built HTTP GET request: %s", request);

        return request;
    }

    /**
     * Test simple graceful shutdown failover:
     * <p/>
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Shutdown first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Bring up the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     */
    @Test
    public void gracefulSimpleFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws Exception {

        String url1 = baseURL1.toString() + "home.jsf";
        String url2 = baseURL2.toString() + "home.jsf";

        log.trace("URLs are: " + url1 + ", " + url2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response;
            NumberGuessState state;

            // First non-Jakarta Server Faces request to the home page
            response = client.execute(buildGetRequest(url1, null));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, null);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // We get a cookie!
            String sessionId = state.sessionId;

            assertNotNull(sessionId);
            assertEquals("0", state.smallest);
            assertEquals("100", state.biggest);
            assertEquals("10", state.remainingGuesses);

            // We do a Jakarta Server Faces POST request, guessing "1"
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "1"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            assertEquals("2", state.smallest);
            assertEquals("100", state.biggest);
            assertEquals("9", state.remainingGuesses);

            // Gracefully shutdown the 1st container.
            stop(NODE_1);

            // Now we do a Jakarta Server Faces POST request with a cookie on to the second node, guessing 100, expecting to find a replicated state.
            response = client.execute(buildPostRequest(url2, state.sessionId, state.jsfViewState, "100"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // If the state would not be replicated, we would have 9 remaining guesses.
            assertEquals("8", state.remainingGuesses, "Session failed to replicate after container 1 was shutdown.");

            // The server should accept our cookie and not try to set a different one
            assertEquals(sessionId, state.sessionId);
            assertEquals("2", state.smallest);
            assertEquals("99", state.biggest);

            // Now we do a Jakarta Server Faces POST request on the second node again, guessing "99"
            response = client.execute(buildPostRequest(url2, sessionId, state.jsfViewState, "99"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            assertEquals("7", state.remainingGuesses);
            assertEquals("2", state.smallest);
            assertEquals("98", state.biggest);

            start(NODE_1);

            // And now we go back to the first node, guessing 2
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "2"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            assertEquals("6", state.remainingGuesses, "Session failed to replicate after container 1 was brought up.");
            assertEquals(sessionId, state.sessionId);
            assertEquals("3", state.smallest);
            assertEquals("98", state.biggest);

            // One final guess on the first node, guess 50
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "50"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            assertEquals(sessionId, state.sessionId);
            assertEquals("5", state.remainingGuesses);
            assertEquals("3", state.smallest);
            assertEquals("49", state.biggest);
        }
    }

    /**
     * Test simple undeploy failover:
     * <p/>
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Undeploy application from the first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Redeploy application to the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     */
    @Test
    public void gracefulUndeployFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws Exception {

        String url1 = baseURL1.toString() + "home.jsf";
        String url2 = baseURL2.toString() + "home.jsf";

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response;
            NumberGuessState state;

            // First non-Jakarta Server Faces request to the home page
            response = client.execute(buildGetRequest(url1, null));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, null);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // We get a cookie!
            String sessionId = state.sessionId;

            assertNotNull(sessionId);
            assertEquals("0", state.smallest);
            assertEquals("100", state.biggest);
            assertEquals("10", state.remainingGuesses);

            // We do a Jakarta Server Faces POST request, guessing "1"
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "1"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            assertEquals("2", state.smallest);
            assertEquals("100", state.biggest);
            assertEquals("9", state.remainingGuesses);

            // Gracefully undeploy from the 1st container.
            undeploy(DEPLOYMENT_1);

            // Now we do a Jakarta Server Faces POST request with a cookie on to the second node, guessing 100, expecting to find a replicated state.
            response = client.execute(buildPostRequest(url2, state.sessionId, state.jsfViewState, "100"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // If the state would not be replicated, we would have 9 remaining guesses.
            assertEquals("8", state.remainingGuesses, "Session failed to replicate after container 1 was shutdown.");

            // The server should accept our cookie and not try to set a different one
            assertEquals(sessionId, state.sessionId);
            assertEquals("2", state.smallest);
            assertEquals("99", state.biggest);

            // Now we do a Jakarta Server Faces POST request on the second node again, guessing "99"
            response = client.execute(buildPostRequest(url2, sessionId, state.jsfViewState, "99"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            assertEquals("7", state.remainingGuesses);
            assertEquals("2", state.smallest);
            assertEquals("98", state.biggest);

            // Redeploy
            deploy(DEPLOYMENT_1);

            // And now we go back to the first node, guessing 2
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "2"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            assertEquals("6", state.remainingGuesses, "Session failed to replicate after container 1 was brought up.");
            assertEquals(sessionId, state.sessionId);
            assertEquals("3", state.smallest);
            assertEquals("98", state.biggest);

            // One final guess on the first node, guess 50
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "50"));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            assertEquals(sessionId, state.sessionId);
            assertEquals("5", state.remainingGuesses);
            assertEquals("3", state.smallest);
            assertEquals("49", state.biggest);
        }
    }

    /**
     * A simple class representing the client state.
     */
    static class NumberGuessState {
        String smallest;
        String biggest;
        String sessionId;
        String remainingGuesses;
        String jsfViewState;

        @Override
        public String toString() {
            return "NumberGuessState{" +
                    "smallest='" + smallest + '\'' +
                    ", biggest='" + biggest + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    ", remainingGuesses='" + remainingGuesses + '\'' +
                    ", jsfViewState='" + jsfViewState + '\'' +
                    '}';
        }
    }
}
