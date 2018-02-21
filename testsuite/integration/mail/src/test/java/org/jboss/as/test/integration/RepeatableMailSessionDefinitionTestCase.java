package org.jboss.as.test.integration;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class RepeatableMailSessionDefinitionTestCase {

    private static SimpleSmtpServer smtpServer1;
    private static SimpleSmtpServer smtpServer2;

    @BeforeClass
    public static void beforeClass() throws IOException {
        smtpServer1 = SimpleSmtpServer.start(Constants.DEFAULT_PORT_1);
        smtpServer2 = SimpleSmtpServer.start(Constants.DEFAULT_PORT_2);
    }

    @AfterClass
    public static void afterClass() {
        if (smtpServer1 != null) {
            smtpServer1.close();
        }
        if (smtpServer2 != null) {
            smtpServer2.close();
        }
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(MailServlet.class)
                .addClass(Constants.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new File("src/test/resources/web.xml"), "web.xml");
    }

    @Test
    @RunAsClient
    public void testMultipleMailSessionDefinitions(@ArquillianResource URL baseUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) baseUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.getResponseCode();
        connection.disconnect();

        checkInbox(smtpServer1.getReceivedEmails(), "session1");
        checkInbox(smtpServer2.getReceivedEmails(), "session2");
    }

    private void checkInbox(List<SmtpMessage> emails, String fromName) {
        assertThat(emails.size(), is(1));
        SmtpMessage email = emails.get(0);
        assertThat(email.getHeaderValue("Subject"), is("Hello"));
        assertThat(email.getBody(), is(fromName));
        assertThat(email.getHeaderValue("From"), is(fromName + "@something.test"));
        assertThat(email.getHeaderValue("To"), is("test@test.test"));
    }
}
