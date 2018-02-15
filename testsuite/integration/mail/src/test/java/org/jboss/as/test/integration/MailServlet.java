package org.jboss.as.test.integration;

import javax.annotation.Resource;
import javax.mail.MailSessionDefinition;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@MailSessionDefinition(
        name = "java:/mail/test-mail-session-1",
        host = Constants.DEFAULT_HOST,
        transportProtocol = "smtp",
        properties = {
                "mail.smtp.host=" + Constants.DEFAULT_HOST,
                "mail.smtp.port=" + Constants.DEFAULT_PORT_1,
                "mail.smtp.sendpartial=true",
                "mail.debug=true"
        }
)
@MailSessionDefinition(
        name = "java:/mail/test-mail-session-2",
        host = Constants.DEFAULT_HOST,
        transportProtocol = "smtp",
        properties = {
                "mail.smtp.host=" + Constants.DEFAULT_HOST,
                "mail.smtp.port=" + Constants.DEFAULT_PORT_2,
                "mail.smtp.sendpartial=true",
                "mail.debug=true"
        })
public final class MailServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Resource(mappedName = "java:/mail/test-mail-session-1")
    private Session session1;

    @Resource(mappedName = "java:/mail/test-mail-session-2")
    private Session session2;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            sendMessage(session1, "session1@something.test", "test@test.test", "Hello", "session1");
            sendMessage(session2, "session2@something.test", "test@test.test", "Hello", "session2");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Session session, String from, String to, String subject, String body) throws MessagingException {
        MimeMessage msg = createMessage(session, from, to, subject, body);
        Transport.send(msg);
    }

    private MimeMessage createMessage(
            Session session, String from, String to, String subject, String body) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setText(body);
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        return msg;
    }

}
