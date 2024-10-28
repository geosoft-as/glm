# GeoSoft License Manager (GLM)

The _GeoSoft License Manager_ (GLM) is an extremely lightweight distributed
license management system.
Put your client licenses on GitHub and access them directly from your software.

You can then remotely:

* Extend license period
* Change feature set
* Change from evaluation to production license
* Remove license on missing payments
* etc.

Note that GLM does not support floating licenses.



## The license

A _license_ gives a client access to use (parts of) your software for a specified
time period. The license is stored in JSON form in the `<productId>.license` file:

```JSON
{
  "licensee":"XoM",
  "product":"Petroll",
  "hardware":["4C4C4544-0034-4A10-8048-B2C04F565432", "4C4C4544-0036-4A12-8048-7689CBA0075"],
  "features":["Core","PdfRead","PdfWrite","2DView"],
  "issued":"18.03.2027",
  "expire":"18.03.2028"
}
```

* **productId** - A string that uniquely identifies the instance of the licensed software.
* **licensee**  - Name of the client.
* **product**   - Name of the software product.
* **hardware**  - List of hardware IDs (UUID) the software can run on. Null for any.
* **features**  - List of features available to the client.
* **issued**    - The start date for the license period.
* **expire**    - The end date for the license period. Null for never.



## The license repository

Put the licenses in a _private_ repository on GitHub and retrieve a fine-grained
_personal access token_ (PAT) to grant programatic read access to it through the
GitHub API.



## Accessing the license from the program

Include `glm.jar` in your software and do:

```Java
import no.geosoft.glm.License;
import no.geosoft.glm.LicenseManager;

// Create license manager
LicenseManager licenseManager = new LicenseManager("<Your company name>",
                                                   "<Product ID>",
                                                   "https://api.github.com/repos/<organization>/<repo>/contents/",
                                                   "<repository token>");

// Get license for the current session
License license = licenseManager.getLicense();
```

If `null` is returned, the program was not able to find the license.

If a license is found there are two ways it can be invalid, either because
it is expired, or if it doesn't apply to the present hardware. Check by:

```Java
license.isExpired();
license.IsValidForThisHardware();
license.isValid(); // The two combined
```

The program must take the appropriate action based on the outcome of these methods.



## Product ID

The running software is associated with its corresponding license through
the _Product ID_ of the application. It is up to the vendor to define how this ID
is created, but a common approach is to use a hash of the _build time_ of the
application given that it is built exclusively for each client.



## Features

An application can be divided into _features_ of which a client have license
to all or a subset. These are listed in the license file, and can be checked like:

```Java
license.hasFeature("<featureName>");
```

The software vendor defines the features and how they are applied in the program.



## Hardware ID

A license can restrict software to specific hardware,
preventing it from running on any devices other than those designated.
This hardware is identified by a
[UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier)
(Universally Unique Identifier), which can be obtained through:

```csh
wmic csproduct get uuid  (Windows)
cat /etc/machine-id  (Linux)
/bin/sh -c system_profiler SPHardwareDataType | awk '/UUID/ { print $3; }' (Mac-OS)
```

An easy way to collect hardware IDs from a client is to have them run the program
on the desired machine(s). When the initial licensing check fails,
display the necessary information they should send to update the license, see:

```Java
import no.geosoft.glm.HardwareId;

String uuid = HardwareId.get();
```

If `hardwareId` in the license file is `null`, the software can run on any device.
