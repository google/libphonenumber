<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
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
      <p></p>
      <input type="submit" value="Submit">
      <input type="reset" value="Reset">
      <p></p>
      <a href="http://code.google.com/p/libphonenumber/">Back to libphonenumber</a>
    </form>
  </body>
</html>
