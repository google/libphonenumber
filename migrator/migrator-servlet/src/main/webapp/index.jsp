<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.phonenumbers.HelloWorld" %>
<html>
<head>
  <link type="text/css" rel="stylesheet" href="/stylesheets/hello-world.css" />
  <title>Hello World</title>
</head>
<body>
<h1>Hello World</h1>
<p>This is <%= HelloWorld.getInfo() %>.</p>
<form action="${pageContext.request.contextPath}/hello" method="post">

  Phone number:<input type="text" name="number"/><br/><br/>
  Country Code:<input type="number" name="countryCode"/><br/><br/>
  <input type="submit" value="convert"/>

</form>
<table>
  <tr>
    <td colspan="2" style="font-weight:bold;">Available Servlets:</td>
  </tr>
  <tr>
    <td><a href='${pageContext.request.contextPath}/hello'>Hello App Engine</a></td>
  </tr>
</table>

</body>
</html>