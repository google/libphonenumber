<%@ page import="com.google.appengine.repackaged.com.google.gson.Gson" %>
<%@ page import="com.google.common.collect.ImmutableList" %>
<%@ page import="com.google.phonenumbers.ServletMain" %>
<%@ page import="com.google.phonenumbers.migrator.MigrationEntry" %>
<%@ page import="com.google.phonenumbers.migrator.MigrationResult" %>
<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
  final String E164_NUMBERS_LINK = "https://support.twilio.com/hc/en-us/articles/223183008-Formatting-International-Phone-Numbers";
  final String COUNTRY_CODE_LINK = "https://countrycode.org/";
  // TODO: use documentation link from base repository when forked repository has been merged in
  final String DOCUMENTATION_LINK = "https://github.com/TomiwaOke/libphonenumber/tree/master/migrator/README.md";
  final String ISSUE_TRACKER_LINK = "https://issuetracker.google.com/issues/new?component=192347";
  final String GUIDELINES_LINK = "https://github.com/google/libphonenumber/blob/master/CONTRIBUTING.md#filing-a-code-issue";

  final Gson gson = new Gson();
  ImmutableList<MigrationResult> validMigrations = (ImmutableList<MigrationResult>) request.getAttribute("validMigrations");
  ImmutableList<MigrationResult> invalidMigrations = (ImmutableList<MigrationResult>) request.getAttribute("invalidMigrations");
  ImmutableList<MigrationEntry> validUntouchedNums = (ImmutableList<MigrationEntry>) request.getAttribute("validUntouchedNumbers");
  ImmutableList<MigrationEntry> invalidUntouchedNums = (ImmutableList<MigrationEntry>) request.getAttribute("invalidUntouchedNumbers");
%>
<html>
<head>
  <link type="text/css" rel="stylesheet" href="/stylesheets/servlet-main.css" />
  <title>Migrator</title>
  <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
  <script type="text/javascript">
    const VALID_MIGRATIONS = 'Valid Migrations';
    const INVALID_MIGRATIONS = 'Invalid Migrations';
    const UNTOUCHED_VALID = 'Already Valid Numbers';
    const UNTOUCHED_INVALID = 'Invalid Non-migratable Numbers';

    const CHART_DESCRIPTIONS = new Map();
    CHART_DESCRIPTIONS[VALID_MIGRATIONS] = 'The following are numbers that were successfully migrated by the tool:';
    CHART_DESCRIPTIONS[INVALID_MIGRATIONS] = 'The following are numbers that were migrated by the tool but were not able' +
            ' to be verified as valid numbers based on metadata for the given country code:';
    CHART_DESCRIPTIONS[UNTOUCHED_VALID] = 'The following are numbers that were already in valid formats:';
    CHART_DESCRIPTIONS[UNTOUCHED_INVALID] = 'The following numbers were not seen as valid and could not be migrated based' +
            ' on the given country code:';

    function getNumbersForSegment(selection) {
      if (selection === VALID_MIGRATIONS) {
        return <%=gson.toJson(ServletMain.getMigrationResultOutputList(validMigrations))%>;
      } else if (selection === INVALID_MIGRATIONS) {
        return <%=gson.toJson(ServletMain.getMigrationResultOutputList(invalidMigrations))%>;
      } else if (selection === UNTOUCHED_VALID) {
        return <%=gson.toJson(ServletMain.getMigrationEntryOutputList(validUntouchedNums))%>;
      }
      return <%=gson.toJson(ServletMain.getMigrationEntryOutputList(invalidUntouchedNums))%>;
    }

    google.charts.load('current', {packages:['corechart']});
    google.charts.setOnLoadCallback(drawChart);
    function drawChart() {
      const chartData = google.visualization.arrayToDataTable([
        ['Task', 'Frequency'],
        [VALID_MIGRATIONS, <%= validMigrations != null ? validMigrations.size() : 0%>],
        [INVALID_MIGRATIONS, <%= invalidMigrations != null ? invalidMigrations.size() : 0%>],
        [UNTOUCHED_VALID, <%= validUntouchedNums != null ? validUntouchedNums.size() : 0%>],
        [UNTOUCHED_INVALID, <%= invalidUntouchedNums != null ? invalidUntouchedNums.size() : 0%>]
      ]);

      const chartProperties = {
        pieHole: 0.4,
        chartArea: { width: '90%', height: '100%' },
        colors: [
          <%=validMigrations != null && !validMigrations.isEmpty()%> ? '#277301' : '',
          <%=invalidMigrations != null && !invalidMigrations.isEmpty()%> ? '#ffbf36' : '',
          <%=validUntouchedNums != null && !validUntouchedNums.isEmpty()%> ? '#90ee90' : '',
          <%=invalidUntouchedNums != null && !invalidUntouchedNums.isEmpty()%> ? '#ff472b' : '']
      };

      const modalBackdrop = document.getElementById("modalBackdrop");

      document.getElementById("modalButton").onclick = function() {
        document.getElementById("numbersList").innerHTML = '';
        modalBackdrop.style.display = 'none';
      };

      window.onclick = function(event) {
        if (event.target === modalBackdrop) {
          document.getElementById("numbersList").innerHTML = '';
          modalBackdrop.style.display = 'none';
        }
      };

      function onSegmentClick() {
        const selection = chart.getSelection()[0];
        if (selection) {
          const selectionName = chartData.getValue(selection.row, 0);
          const numbersList = document.getElementById("numbersList");
          const segmentNumbers = getNumbersForSegment(selectionName);

          segmentNumbers.forEach(number => {
            const value = document.createElement('li');
            value.appendChild(document.createTextNode(number));
            numbersList.appendChild(value);
          });
          document.getElementById("modalTitle").innerHTML = selectionName;
          document.getElementById("modalDescription").innerHTML = CHART_DESCRIPTIONS[selectionName];
          modalBackdrop.style.display = 'block';
        }
      }

      const chart = new google.visualization.PieChart(document.getElementById('migration-chart'));
      google.visualization.events.addListener(chart, 'select', onSegmentClick);
      chart.draw(chartData, chartProperties);
    }
  </script>
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
      tool, please view the <a href="<%=DOCUMENTATION_LINK%>" target="_blank">documentation</a>.
    </p>
  </div>

  <div class="migration-result">
    <c:if test="${empty numberError and not empty number}">
      <c:choose>
        <c:when test="${not empty validMigration}">
          <h3 class='valid'>Valid +<c:out value="${numberCountryCode}"/> Phone Number Produced!</h3>
          <p>The stale number '<c:out value="${number}"/>' was successfully migrated into the phone number: +<c:out value="${validMigration}"/></p>
        </c:when>
        <c:when test="${not empty invalidMigration}">
          <h3 class='invalid-migration'>Invalid +<c:out value="${numberCountryCode}"/> Migration</h3>
          <p>The stale number '<c:out value="${number}"/>' was migrated into the phone number: +<c:out value="${invalidMigration}"/>. However this was not seen as valid using our internal metadata for country code +<c:out value="${numberCountryCode}"/>.</p>
        </c:when>
        <c:when test="${not empty alreadyValidNumber}">
          <h3 class='valid'>Already Valid +<c:out value="${numberCountryCode}"/> Phone Number!</h3>
          <p>The entered phone number was already seen as being in a valid, dialable format based on our metadata for country code +<c:out value="${numberCountryCode}"/>. Here is the number in its clean E.164 format: +<c:out value="${alreadyValidNumber}"/></p>
        </c:when>
        <c:otherwise>
          <h3 class='invalid-number'>Non-migratable +<c:out value="${numberCountryCode}"/> Phone Number</h3>
          <p>The phone number '<c:out value="${number}"/>' was not seen as a valid number and no migration recipe could be found for country code +<c:out value="${numberCountryCode}"/> to migrate it. This may be because you have entered a country code which does not correctly correspond to the given phone number or the specified number has never been valid.</p>
        </c:otherwise>
      </c:choose>
      <p style='color: red; font-size: 14px'>Think there's an issue? File one <a href='<%= ISSUE_TRACKER_LINK %>' target='_blank'>here</a> following the given <a href='<%= GUIDELINES_LINK %>' target='_blank'>guidelines</a>.</p>
    </c:if>
    <c:if test="${empty fileError and not empty fileName}">
      <h3>'<c:out value="${fileName}"/>' Migration Report for Country Code: +<c:out value="${fileCountryCode}"/></h3>
      <p>Below is a chart showing the ratio of numbers from the entered file that were able to be migrated using '+<c:out value="${fileCountryCode}"/>' migration recipes. To understand more, select a given segment from the chart below.</p>
      <div class='chart-wrap'><div id='migration-chart' class='chart'></div></div>
      <form action='${pageContext.request.contextPath}/migrate' method='get' style='margin-bottom: 1rem'>
        <input type='hidden' name='countryCode' value='<c:out value="${fileCountryCode}"/>'/>
        <input type='hidden' name='fileName' value='<c:out value="${fileName}"/>'/>
        <input type='hidden' name='fileContent' value='<c:out value="${fileContent}"/>'/>
        <input type='submit' value='Export Results' class='button'/>
      </form>
    </c:if>
  </div>

  <div class="migration-forms">
    <div class="migration-form">
      <h3>Single Number Migration</h3>
      <div class="error-message"><c:out value="${numberError}"/></div>
      <form action="${pageContext.request.contextPath}/migrate" method="post" enctype="multipart/form-data">
        <input type="hidden" name="csrf_token" value="<c:out value='${csrf_token}'/>"/>
        <label for="number">Phone number:</label>
        <p>Enter a phone number in E.164 format. Inputted numbers can include spaces, curved brackets and hyphens</p>
        <input type="text" name="number" id="number" placeholder="+841205555555" required
               value="<c:out value='${number}'/>"/>

        <label for="numberCountryCode">Country Code:</label>
        <p>Enter the BCP-47 country code in which the specified E.164 phone number belongs to</p>
        <input type="number" name="numberCountryCode" id="numberCountryCode" placeholder="84" required
               value="<c:out value='${numberCountryCode}'/>"/>

        <label for="numberCustomRecipe">Custom Recipe:</label>
        <p>
          (Optional) Upload a csv file containing a custom recipes table to be used for migrations. To understand how to
          create a custom recipe file, please view the <a href="<%=DOCUMENTATION_LINK%>" target="_blank">documentation</a>.
        </p>
        <input type="file" name="customRecipe" id="numberCustomRecipe" accept=".csv"/>

        <input type="submit" value="Migrate Number" class="button"/>
      </form>
    </div>

    <div class="migration-form">
      <h3>File Migration</h3>
      <div class="error-message"><c:out value="${fileError}"/></div>
      <form action="${pageContext.request.contextPath}/migrate" method="post" enctype="multipart/form-data">
        <input type="hidden" name="csrf_token" value="<c:out value='${csrf_token}'/>"/>
        <label for="file">File:</label>
        <p>Upload a file containing one E.164 phone number per line. Numbers can include spaces, curved brackets and hyphens</p>
        <input type="file" name="file" id="file" accept="text/plain" required/>

        <label for="fileCountryCode">Country Code:</label>
        <p>Enter the BCP-47 country code in which the E.164 phone numbers from the specified file belong to</p>
        <input type="number" name="fileCountryCode" id="fileCountryCode" placeholder="84" required/>

        <label for="fileCustomRecipe">Custom Recipe:</label>
        <p>
          (Optional) Upload a csv file containing a custom recipes table to be used for migrations. To understand how to
          create a custom recipe file, please view the <a href="<%=DOCUMENTATION_LINK%>" target="_blank">documentation</a>.
        </p>
        <input type="file" name="customRecipe" id="fileCustomRecipe" accept=".csv"/>

        <input type="submit" value="Migrate File" class="button"/>
      </form>
    </div>
  </div>

  <div id="modalBackdrop" class="modal-backdrop">
    <div class="modal-content">
      <h3 id="modalTitle"></h3>
      <p id="modalDescription" style="color: grey; font-size: 12px"></p>
      <div class="body">
        <ul id="numbersList" style="padding-left: 1.5rem"></ul>
        <p style="color: red; font-size: 14px">
          Think there's an issue? File one <a href="<%=ISSUE_TRACKER_LINK%>" target="_blank">here</a>
          following the given <a href="<%=GUIDELINES_LINK%>" target="_blank">guidelines</a>.
        </p>
      </div>
      <button id="modalButton" class="button">Close</button>
    </div>
  </div>
</body>
</html>
