<%@ page language="java"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>

<html>
 <head>
  <title>
    <bean:message key="login.jsp.title" />   
  </title>
 </head>
 <body>
  <html:form action="/login.do">
   <bean:message key="login.jsp.username" />
   <html:text property="userName" />
   <html:errors property="userName" /> <br />
   <bean:message key="login.jsp.password" />
   <html:password property="password" />
   <html:errors property="password" /> <br />
   <html:submit /> <html:cancel />
  </html:form>
 </body>
</html>