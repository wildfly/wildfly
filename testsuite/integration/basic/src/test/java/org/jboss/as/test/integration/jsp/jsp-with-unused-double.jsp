<%
    session.setAttribute("v1", new Double(1));
    session.setAttribute("v2", new Double(2));

    double v1 = (Double) session.getAttribute("v1");
    double v2 = (Double) session.getAttribute("v2");

    out.write(String.valueOf(v1));
//    out.write(String.valueOf(v2));
%>
