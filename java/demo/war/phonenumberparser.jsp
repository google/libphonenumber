<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css" />
  </head>
  <body>
    <h2>Phone Number Parser Demo</h2>
    <form action="/phonenumberparser" method="post" accept-charset="UTF-8"
          enctype="multipart/form-data">
      <h2>Step 1</h2>
      <p>
      Specify a Phone Number: <input type="text" name="phoneNumber" size="25">
      <p>
      <b>Or</b> Upload a file containing phone numbers separated by comma.
      <p>
      <input type="file" name="numberFile" size="30">
      <p>
      <h2>Step 2</h2>
      <p>
      Specify a Default Country:
      <input type="text" name="defaultCountry" size="2">
          (<a href="http://www.iso.org/iso/english_country_names_and_code_elements">
          ISO 3166-1 two-letter country code</a>)
      <h2>Step 3</h2>
      <p>
      Specify a locale for phone number geocoding (Optional, defaults to en):
      <p>
      <input type="text" name="languageCode" size="2">-<input type="text" name="regionCode"
                                                              size="2">
          (<a href="http://download.oracle.com/javase/6/docs/api/java/util/Locale.html">A valid ISO
              Language Code and optionally a region to more precisely define the language.</a>)
      <p></p>
      <input type="submit" value="Submit">
      <input type="reset" value="Reset">
      <p></p>
      <a href="https://github.com/googlei18n/libphonenumber/">Back to libphonenumber</a>
    </form>
  </body>
</html>
