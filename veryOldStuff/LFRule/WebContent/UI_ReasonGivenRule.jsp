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
What's the rule? <INPUT TYPE=TEXT NAME=rule SIZE=4><BR>
What's the new patient name? <INPUT TYPE=TEXT NAME=new_patient SIZE=20>
<P><INPUT TYPE=SUBMIT>
</FORM>

<% 
 String patient = request.getParameter( "patient" );
 session.setAttribute( "thePatient", patient );
   
 String rule = request.getParameter( "rule" );
 session.setAttribute( "theRule", rule);
   
 String new_patient = request.getParameter( "new_patient" );
 session.setAttribute( "theNew_patient", new_patient);
%>

<%
	ReasonGivenRule.RunCommandLineTool(patient, rule, new_patient);
%>

Your patient is: <%= session.getAttribute( "thePatient" ) %>.

Your rule is: <%= session.getAttribute( "theRule" ) %>. 

Your new patient is: <%= session.getAttribute( "theNew_patient" ) %>, isn't it?

</body>
</html>