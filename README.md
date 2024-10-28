# GeoSoft License Manager (GLM)

GLM is a lightweight distributed license management system.
Put the licenses on GitHub and access them directly from your software.

You can then remote control things like:

* Extend license period
* Change feature set
* Remove license if client doesn't pay
* Change from evaluation to production license



## The license

A license gives a client access to use your software for a specified time period.
It is stored in JSON form in the `<productId>.license` file:

```JSON
{
  "licensee":"XoM",
  "product":"Petroll",
  "hardware":["4C4C4544-0034-4A10-8048-B2C04F565432", "4C4C4544-0036-4A12-8048-7689CBA0075"],
  "features":["SeismicRead","SeismicWrite","AttributeGen"],
  "issued":"18.03.2027",
  "expire":"18.04.2028"
}
```

* **licensee** - Name of the client.
* **product**  - Name of the software product.
* **hardware** - List of hardware IDs (UUID) the software can run on. Null for any.
* **features** - List of features available to the client.
* **issued**   - The start date for the license period.
* **expire**   - The end date for the license period. Null for never.



## The license repository

Put the licenses in a _private_ repository on GitHub and retrieve a _personal access token_
(PAT) to grant programatic read access to it through the GitHub API.



## Accessing the license from the program

In your software include the glm.jar library and do

```Java
// Create license manager
LicenseManager licenseManager = new LicenseManager("<Your company name>",
                                                   "<Software Uniiqe ID>",
                                                   "https://api.github.com/repos/<organization>/<repo>/contents/",
                                                   "<token>");

// Get license for the current session
License license = licenseManager.getLicense();
```

If null is returned, the program was not able to find the license.

If a license is found there are two ways it can be invalid, either because
it is expired, or if it doesn't apply to the present hardware. Check by:

```Java
license.isValid();
license.isExpired();
license.IsValidForThisHardware();
```



## Features

An application can be divided into _features_ of which your client have license
to all or a subset. These are listed in the license file, and can be checked like:

```Java
license.hasFeature("<featureName>");
```

The software vendor defines the features and how they are applied in the program.



## Hardware ID

A license can bind the software to certain hardwares so that it cannot run on any
other machines than the specified ones. Hardware is identified by UUID, and can be
obtained by:

```csh
wmic csproduct get uuid // Windows
cat /etc/machine-id // Linux
/bin/sh -c system_profiler SPHardwareDataType | awk '/UUID/ { print $3; }' // Mac-OS
```

A simple way to gather hardware IDs from a client is to ask them to start the program
on the machine(s) they want to use, and when the licensing initially fails present the
necessary information they need to send you in order to update the license, see:

```Java
String uuid = HardwareId.get();
```


