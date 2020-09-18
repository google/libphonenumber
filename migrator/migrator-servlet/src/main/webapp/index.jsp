<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  final String E164_NUMBERS_LINK = "https://support.twilio.com/hc/en-us/articles/223183008-Formatting-International-Phone-Numbers";
  final String COUNTRY_CODE_LINK = "https://countrycode.org/";
  final String DOCUMENTATION_LINK = "./"; // TODO: use README documentation link when uploaded
%>
<html>
<head>
  <link type="text/css" rel="stylesheet" href="/stylesheets/servlet-main.css" />
  <title>Migrator</title>
</head>
<body>
  <div class="page-heading">
    <h1>Phone Number Migrator</h1>
    <p>
      The migrator is a tool which takes in a given <a href="<%=E164_NUMBERS_LINK%>" target="_blank">E.164 phone number(s)</a>
      input as well as the corresponding BCP-47 <a href="<%=COUNTRY_CODE_LINK%>" target="_blank">country code</a>. The tool
      will then check the validity of the phone number based on the country code and if possible, will convert the number
      into a valid, dialable format.
    </p>
    <p>The following are the two available migration types that can be performed:</p>
    <ul>
      <li>
        <strong>Single Number Migration:</strong> input a single E.164 phone number with its corresponding BCP-47 country
        code. If there is an available migration that can be performed on the number, it will be converted to the new
        format based on the specified migration rules.<br><br>
      </li>
      <li>
        <strong>File Migration:</strong> input a text file containing one E.164 number per line along with the BCP-47
        country code that corresponds to the numbers in the text file. All numbers in the text file that match available
        migrations will be migrated and there will be the option of downloading a new text file containing the updated numbers.
        By default, invalid migrations and numbers that did not go through a process of migration will be written to file
        in their original text file format.
        <br><br>
      </li>
    </ul>
    <p>
      For more information on the capabilities of the migrator as well as instructions on how to install the command line
      tool, please view the <a href="<%=DOCUMENTATION_LINK%>" target="_blank">documentation</a>. <%-- TODO: use README documentation link when uploaded --%>
    </p>
  </div>

  <div class="migration-result">
    <%
      if (request.getAttribute("numberError") == null && request.getAttribute("number") != null) {
        if (request.getAttribute("validMigration") != null) {
          out.print("<h3 class='valid'>Valid +" + request.getAttribute("numberCountryCode") + " Phone Number Produced!</h3>");
          out.print("<p>The stale number '" + request.getAttribute("number") + "' was successfully migrated into the" +
                  " phone number: +" + request.getAttribute("validMigration") + "</p>");
        } else if (request.getAttribute("invalidMigration") != null) {
          out.print("<h3 class='invalid-migration'>Invalid +" + request.getAttribute("numberCountryCode") + " Migration</h3>");
          out.print("<p>The stale number '" + request.getAttribute("number") + "' was migrated into the phone number:" +
                  " +" + request.getAttribute("invalidMigration") + ". However this was not seen as valid using our internal" +
                  " metadata for country code +" + request.getAttribute("numberCountryCode") + ".</p>");
          // TODO: add link for users to file bugs
        } else if (request.getAttribute("alreadyValidNumber") != null) {
          out.print("<h3 class='valid'>Already Valid +" + request.getAttribute("numberCountryCode") + " Phone Number!</h3>");
          out.print("<p>The entered phone number was already seen as being in a valid, dialable format based on our" +
                  " metadata for country code +" + request.getAttribute("numberCountryCode") + ". Here is the number in" +
                  " its clean E.164 format: +" + request.getAttribute("alreadyValidNumber") + "</p>");
        } else {
          out.print("<h3 class='invalid-number'>Non-migratable +" + request.getAttribute("numberCountryCode") + " Phone Number</h3>");
          out.print("<p>The phone number '" + request.getAttribute("number") + "' was not seen as a valid number and" +
                  " no migration recipe could be found for country code +" + request.getAttribute("numberCountryCode") +
                  " to migrate it. This may be because you have entered a country code which does not correctly correspond" +
                  " to the given phone number or the specified number has never been valid.</p>");
          // TODO: add link for users to file bugs
        }
      }
    %>
  </div>

  <div class="migration-forms">
    <div class="migration-form">
      <h3>Single Number Migration</h3>
      <div class="error-message"><%=request.getAttribute("numberError") == null ? "" : request.getAttribute("numberError")%></div>
      <form action="${pageContext.request.contextPath}/migrate" method="post">
        <label for="number">Phone number:</label>
        <p>Enter a phone number in E.164 format. Inputted numbers can include spaces, curved brackets and hyphens</p>
        <input type="text" name="number" id="number" placeholder="+841205555555" required
               value="<%=request.getAttribute("number") == null ? "" : request.getAttribute("number")%>"/>

        <label for="numberCountryCode">Country Code:</label>
        <p>Enter the BCP-47 country code in which the specified E.164 phone number belongs to</p>
        <input type="number" name="numberCountryCode" id="numberCountryCode" placeholder="84" required
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
        <input type="number" name="fileCountryCode" id="fileCountryCode" placeholder="84" required/>

        <input type="submit" value="Migrate File" class="submit"/>
      </form>
    </div>
  </div>
</body>
</html>
