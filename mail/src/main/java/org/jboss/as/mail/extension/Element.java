package org.jboss.as.mail.extension;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 * @created 26.7.11 15:21
 */
public enum Element {
    UNKNOWN(null),

    LOGIN(MailSubsystemModel.LOGIN),
    MAIL_SESSION(MailSubsystemModel.MAIL_SESSION),
    SMTP_SERVER(MailSubsystemModel.SMTP_SERVER),
    POP3_SERVER(MailSubsystemModel.POP3_SERVER),
    IMAP_SERVER(MailSubsystemModel.IMAP_SERVER),;

    private final String name;

    Element(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Element> elements;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null) { map.put(name, element); }
        }
        elements = map;
    }

    public static Element forName(String localName) {
        final Element element = elements.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
