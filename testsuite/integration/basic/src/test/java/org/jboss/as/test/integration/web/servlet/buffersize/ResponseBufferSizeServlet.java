package org.jboss.as.test.integration.web.servlet.buffersize;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "ResponseBufferSizeServlet", urlPatterns = {"/ResponseBufferSizeServlet"})
public class ResponseBufferSizeServlet extends HttpServlet {


    public static final String SIZE_CHANGE_PARAM_NAME = "sizeChange";

    public static final String DATA_LENGTH_IN_PERCENTS_PARAM_NAME = "dataLengthInPercents";

    public static final String RESPONSE_COMMITED_MESSAGE = "Response committed";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sizeChangeAsStr = request.getParameter(SIZE_CHANGE_PARAM_NAME);
        String dataLengthInPercentsAsStr = request.getParameter(DATA_LENGTH_IN_PERCENTS_PARAM_NAME);
        double sizeChange = 1.0;
        double dataLengthModifier = 1.0;
        if (sizeChangeAsStr != null) {
            sizeChange = Double.parseDouble(sizeChangeAsStr);
        }
        if (sizeChangeAsStr != null) {
            dataLengthModifier = Double.parseDouble(dataLengthInPercentsAsStr);
        }
        int origBufferSize = response.getBufferSize();
        int newBufferSize = (int)(origBufferSize * sizeChange);
        int dataLength = (int)(newBufferSize*dataLengthModifier);


        int lineLength = 160; // setting line length to create nicer output

        // generating output of specified size
        response.setBufferSize(newBufferSize);
        StringBuffer outputBuffer = new StringBuffer(dataLength);
        for (int i = 0; i < dataLength; i++) {
            outputBuffer.append("X");
            if ((dataLength%lineLength) == 0) {
                outputBuffer.append('\n');
                i++;
            }
        }

        response.getWriter().write(outputBuffer.toString());
        if (response.isCommitted()) {
            response.getWriter().println(RESPONSE_COMMITED_MESSAGE);
        }
    }
}
