<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.phonenumbers.ServletMain" %>
<html>
<head>
  <link type="text/css" rel="stylesheet" href="/stylesheets/servlet-main.css" />
  <title>Migrator</title>
</head>
<body>
  <div class="page-heading">
    <h1>Phone Number Migrator</h1>
    <p>
      This is a tool to help migrate stale phone numbers into valid formats..... More stuff about what and how to use it.
      This is a tool to help migrate stale phone numbers into valid formats..... More stuff about what and how to use it.
      This is a tool to help migrate stale phone numbers into valid formats..... More stuff about what and how to use it.
    </p>
  </div>

  <div class="migration-forms">
    <div class="migration-form">
      <h3>Single Number Migration</h3>
      <div class="error-message"><%=request.getAttribute("numberError") == null ? "" : request.getAttribute("numberError")%></div>
      <form action="${pageContext.request.contextPath}/migrate" method="post">
        <label for="number">Phone number:</label>
        <p>Enter a phone number in E.164 format. Inputted numbers can include spaces, curved brackets and hyphens</p>
        <input type="text" name="number" id="number" placeholder="+841205555555" required
               value="<%=request.getAttribute("Number") == null ? "" : request.getAttribute("Number")%>"/>

        <label for="numberCountryCode">Country Code:</label>
        <p>Enter the BCP-47 country code in which the entered E.164 phone number belongs to</p>
        <input type="number" name="countryCode" id="numberCountryCode" placeholder="84" required
               value="<%=request.getAttribute("numberCountryCode") == null ? "" : request.getAttribute("numberCountryCode")%>"/>

        <input type="submit" value="Migrate Number" class="submit"/>
      </form>
    </div>

    <div class="migration-form">
      <h3>File Migration</h3>
      <div class="error-message"><%=request.getAttribute("fileError") == null ? "" : request.getAttribute("fileError")%></div>
      <form action="${pageContext.request.contextPath}/migrate" method="post">
        <label for="file">File:</label>
        <p>Upload a file containing one E.164 phone number per line. Numbers can include spaces, curved brackets and hyphens</p>
        <input type="file" name="file" id="file" accept="text/plain" required/>

        <label for="fileCountryCode">Country Code:</label>
        <p>Enter the BCP-47 country code in which the E.164 phone numbers from the specified file belong to</p>
        <input type="number" name="countryCode" id="fileCountryCode" placeholder="84" required/>

        <input type="submit" value="Migrate File" class="submit"/>
      </form>
    </div>
  </div>
</body>
</html>
