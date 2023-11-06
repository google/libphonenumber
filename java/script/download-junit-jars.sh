#!/bin/bash
# Set the version of JUnit and Mockito
junit_version="4.13.1"
mockito_version="1.10.19"
cd ../
# Function to download a JAR if not available
download_jar() {
    local jar_name="$1"
    local jar_filename="$2"
    local download_url="$3"

    if [ ! -f "lib/$jar_filename" ]; then
        echo "Downloading $jar_name..."
        wget "$download_url" -P lib
    else
        echo "$jar_name already exists in lib directory."
    fi
}

# Check and download JUnit&Mockito JAR
download_jar "JUnit" "junit-$junit_version.jar" "https://repo1.maven.org/maven2/junit/junit/$junit_version/junit-$junit_version.jar"
download_jar "Mockito" "mockito-all-$mockito_version.jar" "https://repo1.maven.org/maven2/org/mockito/mockito-all/$mockito_version/mockito-all-$mockito_version.jar"

# Run the mvn install command for the Snapshot jars
cd ../
mvn clean install
