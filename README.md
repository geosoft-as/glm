# GeoSoft License Manager (GLM)

GLM is a lightweight distributed license management system.
Put the licenses on GitHub and access them there frome your software.
You can then remote control things like:

* extend license period
* change feature set
* remove license if client doesn pay
* change from evaluation to production license


## The license

A license gives a client access to use your software for a specified time period.
It is stored in JSON form in a file named `<productId>.license`:

```JSON
{
  "licensee":"XoM",
  "hardware":["11111","22222","33333",...],
  "product":"Petrel",
  "features":["SeismicRead", "SeismicWrite", "AttributeGen", ...],
  "issued":"18.03.2027",
  "expire":"18.04.2028"
}
```

* **product**  - Descriptive name of the client.
* **licensee** - The name of the client.
* **hardware** - List of hardware IDs (UUID) the software can run on. Null for any.
* **features** - List of features available to the client.
* **issued**   - The start date for the license period.
* **expire**   - The end date for the license period. Null for never.


## The license repository

Put the licenses in a _private_ repository on GitHub and retrieve a _personal access token_
(PAT) to grant programatic read access to it through the GitHub API.


## Accessing the license from the program


