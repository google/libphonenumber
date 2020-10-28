# Migrator Library

The library takes in E.164 phone numbers with their corresponding [BCP-47 country code](https://countrycode.org/). If inputted numbers match any of the supported internal renumbering ‘recipes’, which are created from official country documentation, said numbers will undergo a process of migration to convert them into the most up to date, dialable format. A given “recipe” is an internal record which holds data needed to migrate a stale number into one that is valid and dialable. 

## Capabilities

The migrator library can be used as either a [command line tool](#command-line-usage) or a [web application](#web-application). Both applications have the following capabilities:

```Single Number Migration``` - input a single E.164 phone number with its corresponding BCP-47 country code. If there is an available migration recipe, the number will be converted to the new format based on the given recipe. The resulting string of the migration will be outputted to the console.

```File Migration``` - input a text file location with one E.164 number per line along with the BCP-47 country code that corresponds to the numbers in the text file. All numbers in the text file that match available recipes will be migrated and a copy of the text file will be created with the updated numbers. The file path to the newly created text file will be outputted to the console.

```Custom Recipe Migration``` - specify a [custom recipe](#custom-recipe-migrations) file to use for a migration instead of the internal maintained file. Custom recipe migrations will not check if converted numbers are valid based on country formats. 

```Migration Report``` - find all phone numbers in a given input that were migrated with given migration recipes. The report will also show the phone numbers that were already seen as valid before migrations as well as identify any numbers that were migrated into invalid formats based on known valid phone number ranges for the specified country code.

## Command Line Usage

### Required Parameters

```(-n, --number | -f, --file)``` - either the phone number input or file input for a given migration. The number argument must be a single E.164 phone number. This number can be in strict E.164 format (“+841200000000”) or contain hyphens, spaces and curved brackets (“84 (120) 5555-555”). The file argument must be a path to a text file containing one E.164 phone number per line. For file migrations, the original text file will not be overwritten or stored and instead a new text file will be created containing the migrated version of each phone number or the original phone number when a migration did not take place. Original phone numbers will also be written to file when a given migration on a number is seen as invalid, producing a number not dialable for the given country, unless the --exportInvalidMigrations (see [Optional Parameters](#optional-parameters)) flag is specified. Note: all phone numbers entered for migration will be sanitized by removing any whitespace, hyphens or curved brackets. If the number after this process is still not in E.164 format, a migration will not be able to be performed on it.

```-c, --countryCode``` - the BCP-47 country code that corresponds to the given phone number input. Only recipes from this country will be queried to find matching recipes for inputted numbers. (E.g. 44 for United Kingdom, 1 for US, 84 for Vietnam)

### Optional Parameters

```-r, --customRecipe``` - the path to a csv file containing custom migration recipes which follow the standard recipes file format. When using custom recipes, validity checks on migrated numbers will not be performed.

```-e, --exportInvalidMigrations``` - the boolean command line flag specifying that text files created after the migration process for standard file migration job should contain the migrated version of a phone number when a migration has taken place on it, regardless of whether the migration resulted in an invalid phone number. By default, when a migrated number is seen as invalid, its original inputted representation is written to file. Note: this flag has no effect on a --number migration or a migration using a --customRecipe. 

```-h, --help``` - usage help


### Installation and Setup

Start off by using git to download the latest version of the libphonenumber repository:

```bash
git clone https://github.com/google/libphonenumber.git
cd libphonenumber
ls
```

The project must then be built with its dependencies using maven so that an executable jar can be created and run:

```bash
# clean install metadata dependency to run migrator tool
cd metadata
mvn clean install

cd ../migrator
# clean install migrator to run migrator-servlet locally (optional)
mvn clean install

mvn clean compile assembly:single
cd target
ls
```

### Single Number Migration

To perform a single number migration, the “countryCode” and “number” arguments must be specified. The tool will clean the number string to remove spaces, hyphens and curved brackets. Note that when a number contains spaces, the whole string must be enclosed with quotation marks.

An example of a valid migration of a Vietnam phone number is as shown below:

```bash
java -jar migrator-1.0.0-jar-with-dependencies.jar --countryCode=84 --number=+841201234567

# Output:
# Migration of country code +84 phone number(s):
# Successful migration into: +84701234567
```

If the given number is already in a valid format for the given country code, a migration will not occur and instead a message explaining this will be printed:

```bash
java -jar migrator-1.0.0-jar-with-dependencies.jar --countryCode=84 --number=+84(70)123-4567

# Output:
# Migration of country code +84 phone number(s):
# This number was seen to already be valid and dialable based on our data for the given country
```

The migrator tool will not migrate or validate a number that belongs to a different country from the specified country code. In such situations the tool will take no action:

```bash
java -jar migrator-1.0.0-jar-with-dependencies.jar --countryCode=84 --number=”+44 77 12345 678”

# Output:
# Migration of country code +84 phone number(s):
# This number could not be migrated using any of the recipes from the given recipes file
```

### File Migration

To perform a file migration, the “countryCode” and “file” arguments must be specified. The tool will generate a new text file with the outputted numbers once the migration is completed. For the following examples, “VietnamNumbers.txt” is a text file containing the phone numbers:

```bash
VietnamNumbers.txt

84-1201234567
+84(121)7654321
+841225555555
84 120 1424534
+84709000000
+123829734972
373203934781
```
##### Migration using the country code +84:
```bash
java -jar migrator-1.0.0-jar-with-dependencies.jar --countryCode=84 --file=../VietnamNumbers.txt

# Output: 
# Migration of country code +84 phone number(s):
# New numbers file created at: ./+84_Migration_VietnamNumbers.txt
```
```bash
+84_Migration_VietnamNumbers.txt

+84701234567
+84797654321
+84775555555
+84701424534
+84709000000
+123829734972
373203934781
```
##### Migration using the country code +1:
```bash
java -jar migrator-1.0.0-jar-with-dependencies.jar --countryCode=1 --file=../VietnamNumbers.txt 

# Output:
# Migration of country code +1 phone number(s):
# New numbers file created at: ./+1_Migration_VietnamNumbers.txt
```
```bash
+1_Migration_VietnamNumbers.txt

84-1201234567
+84(121)7654321
+841225555555
84 120 1424534
+84709000000
+123829734972
373203934781
```
The above migration run created a text file with the same phone numbers as the original file because there are no +1 phone numbers in the file that could have been migrated.

#### Exporting Invalid Migrations

When performing standard migrations (migrations that do not use custom recipe files), there is a possibility that a migrated phone number is seen as being invalid for the given country code based on internal checks. This will occur when either an internal recipe is erroneous or the internal phone number ranges for a given country code do not properly reflect all valid numbers. In such cases, migrations will be rolled back and the original number will be written to file when running a “file” migration. To allow for invalid migrations to still be written to file, the “exportInvalidMigrations” flag must be specified.

The recipes table below shows a recipe with an error which will cause +84120 phone numbers to be converted invalidly to +8460 instead of +8470 which is the actual valid representation for the given Vietnamese numbers:

```bash
Erroneous Internal Recipe File

Old Prefix   ;   Old Length  ;  Country Code   ;    Old Format     ;    New Format     ;  Description
84120        ;   12          ;  84             ;    xx120xxxxxxx   ;    xx60xxxxxxx    ;  Redial 84120 with 8470
84121        ;   12          ;  84             ;    xx121xxxxxx    ;    xx79xxxxxxx    ;  Redial 84121 with 8479
```

Below is the file input for the example migration runs:

```bash
VietnamNums.txt

84-1201234567
+84(121)7654321
```

##### Example 1 - export original numbers (default):

```bash
java -jar migrator-1.0.0-jar-with-dependencies.jar --countryCode=84 --file=../VietnamNums.txt 

# Output:
# Migration of country code +84 phone number(s):
# New numbers file created at: ./+84_Migration_VietnamNums.txt
```

The created text file will use the original number for the +84120 migration due to the migration being invalid.

```bash
+84_Migration_VietnamNums.txt

84-1201234567
+84797654321
```

##### Example 2 - export invalid migrations:

```bash
java -jar migrator-1.0.0-jar-with-dependencies.jar --countryCode=84 --file=../VietnamNums.txt --exportInvalidMigrations

# Output:
# Migration of country code +84 phone number(s):
# New numbers file created at: ./+84_Migration_VietnamNums.txt
```

Even though the migration was invalid, the newly created text file will use the migrated number for the +84120 migration because the file is set to be exported with invalid migrations.

```bash
+84_Migration_VietnamNums.txt

+84601234567
+84797654321
```

### Custom Recipe Migrations
Custom recipe migrations can be performed on both “number” and “file” type migrations. The additional requirement is that there is a “customRecipe” argument which points to a csv file containing a recipes table in the standard table format. The following is an example of a valid custom recipe file to be used.

```bash
CustomRecipes.csv

Old Prefix ; Old Length ; Country Code ;  Old Format    ;  New Format   ; Description
123        ; 7          ; 1            ;  xx3xxxx       ;  xx99xxxx     ; Custom recipe, replace 3 with 99
001234     ; 7          ; 00           ;  xxxxxxx       ;  xx5xxxxx     ; Custom recipe, add 5 to third index
```

The above recipes file can be used in place of internal recipes by specifying in the following way:

```bash
java -jar migrator-1.0.0-jar-with-dependencies.jar --countryCode=00 --number=0012345 --customRecipe=./CustomRecipes.csv

# Output:
# Migration of country code +00 phone number(s):
# Successful migration into: +00512345
```
### Creating Custom Recipes.csv Files

A Recipes.csv file holds a table containing migration information for all known recipes. A given “recipe” is a row within the table which holds data needed to migrate a stale number into one that is valid and dialable. 

When creating custom recipe files to use, there should never be a case where two recipes can be used to renumber one stale number so as a result, a given stale number can only ever match with one row in a Recipes table. Each column in the file must be separated using ```;``` and the table must have the following columns: 

```Prefix (key)``` - the prefix of the possible stale numbers that can be represented by the row. For example, the prefix of the number “12345” could be “12”. Please note that the prefix cannot be the actual number so for the example case, the prefix could not be “12345”.

```Length (key)``` - the length of all numbers that can be represented in the row. For example a length of 5 for a prefix of “1234” could represent numbers: “12340”, “12341”, “12342”, “12343”, ...

```Old Format``` - represents what digits in the old format need to change where every digit that needs to stay the same is represented as an ‘x’
E.g. the number 7 at the 3rd index of a 4 digit number needs to be removed/replaced. This number is always 7 → xx7x
E.g. an addition needs to happen for a 4 digit number and no digits from the original format need to be changed, so none of the original digits need to be altered → xxxx

```New Format``` - represents what the new number format should be. ‘x’ values represent digits from the original number that will stay the same.
E.g. digits ‘98’ need to be added at the start of an originally 4 digit number → 98xxxx
E.g. no digits need to be added because a removal took place. A 5 digit number has now become a 4 digit number → xxxx

```Description``` - text specifying what the change is in words. This column is used purely for debugging purposes and will help easily diagnose issues with recipes quickly when the strings in the ‘Old Format’ or ‘New Format’ columns are incorrect.

## Web Application

Please [click here](https://phone-number-migrator.uc.r.appspot.com/) to view the web application version of the migrator tool.

## About validation of phone numbers:

APIs and data in [libphonenumber/metadata/](https://github.com/google/libphonenumber/tree/master/metadata/) are used for validating phone numbers. More details in [metadata/README](https://github.com/google/libphonenumber/tree/master/metadata/README).

## Privacy

Both web application and command line versions of the migrator tool require an input of either a single string E.164 phone number or a text file containing one E.164 phone number per line. After the tool has completed the migration process, this data, along with any resulting data, is never written to any records or stored for later use.

## Disclaimer

The migration library is designed to only migrate E.164 phone numbers using internally maintained migration “recipes”. The library will not attempt to migrate national phone numbers or format non-E.164 phone numbers prior to migrations in order for said numbers to be in E.164 format. Only recipes from the specified country code will be used to perform migrations on a given number(s) input. This means that even if a stale E.164 phone number from country A has a recipe that can be used to migrate it successfully but ```--countryCode=B``` is inputted as the command line argument, the phone number will not be migrated.

For standard migrations using internal recipes, it is possible for E.164 phone numbers to be migrated to a format which the library deems as being invalid. The validity of phone numbers is checked using metadata containing valid number ranges for all countries. As a result, a phone number migration will be seen as invalid if either the metadata of valid number ranges does not reflect the given format correctly or if there is an error with the used recipe internally. In either case, please [file a new issue](https://b.corp.google.com/issues/new?component=192347&template=829703) following the guidance of how to create one [here](https://github.com/google/libphonenumber/blob/master/CONTRIBUTING.md#filing-a-code-issue) in order to resolve the issue. For ```--file``` number inputs, invalid migrations by default will not be written to the created text file containing the updated phone numbers and instead, the original number which correlates to the migration will be used. If you would like to include invalid migration results in the newly created text file, specify this by using the ```--exportInvalidMigrations``` command line flag.

When using the ```--customRecipe``` argument, all numbers that match a given recipe from the number input will be migrated. These migrations will not be checked for validity based on internal metadata of valid phone number ranges. This means that for custom recipe migrations, there is no perception of invalid migrations. 

## License
See [LICENSE file](https://github.com/google/libphonenumber/blob/master/LICENSE) for Terms and Conditions.
