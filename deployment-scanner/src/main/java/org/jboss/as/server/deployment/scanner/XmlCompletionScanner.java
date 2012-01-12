package org.jboss.as.server.deployment.scanner;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Determines if an XML document is well formed, to prevent half copied XML files from being deployed
 *
 * @author Stuart Douglas
 */
public class XmlCompletionScanner {


    public static boolean isCompleteDocument(final File file) throws IOException {
        ErrorHandler handler = new ErrorHandler();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            final SAXParser parser = factory.newSAXParser();
            parser.parse(file, handler);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            DeploymentScannerLogger.ROOT_LOGGER.debugf(e, "Exception parsing scanned XML document %s", file);
            return false;
        }
        return !handler.error;
    }

    private static class ErrorHandler extends DefaultHandler {

        private boolean error = false;

        @Override
        public void error(final SAXParseException e) throws SAXException {
            error = true;
        }

    }
}
