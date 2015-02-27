<%@ page import="java.util.Map, java.lang.*, cwm.reasoning.*" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Hello man!</title>
</head>
<body>

<FORM METHOD=POST>
What's the patient name? <INPUT TYPE=TEXT NAME=patient SIZE=20><BR>
<P><INPUT TYPE=SUBMIT>
</FORM>

<% 
 String patient = request.getParameter( "patient" );
 session.setAttribute( "thePatient", patient );
%>

<%
	ReasonApplicableRules.runIt(patient);
%>

Your patient is: <%= session.getAttribute( "thePatient" ) %>.

</body>
</html>