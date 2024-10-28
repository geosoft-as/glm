package no.geosoft.glm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * An application license manager. Use it as follows:
 * <pre>
 *   LicenseManager licenseManager = new LicenseManager("<company>",
 *                                                      "<productId>",
 *                                                      "<licenseRepoUrl>",
 *                                                      "<accessToken>");
 *   License license = licenseManager.getLicense();
 * </pre>
 *
 * License files are JSON of the following format:
 * <pre>
 *   {
 *     "licensee":"XoM",
 *     "hardware":["11111","22222","33333",...], (system UUID addresses or null for any, see HardwareId.java)
 *     "product":"Petrel",
 *     "features":["SeismicRead", "SeismicWrite", "AttributeGen", ...],
 *     "issued":"18.03.2027",
 *     "expire":"18.04.2028" (or null if never)
 *   }
 * </pre>
 * The file must be named &lt;productId&gt;.license and pushed to the license GitHub repository
 * of the vendor where they will be accessed by the program. This repository is private and accessed
 * using its access token as shown.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class LicenseManager
{
  /** The logger instance. */
  private final static Logger logger_ = Logger.getLogger(LicenseManager.class.getName());

  /** Format used for dates in the license file. */
  private final static String DATE_FORMAT = "dd.MM.yyyy";

  /** Application vendor company name. Non-null. */
  private final String companyName_;

  /** Application unique ID, from build time etc. Non-null. */
  private final String productId_;

  /** URL to the vendors private license repository. Non-null. */
  private final String licenseRepositoryUrl_;

  /** License repository access token. Non-null. */
  private final String accessToken_;

  /** License of present session. Lazily created. Null initially and if reading fails. */
  private License license_;

  /** Indicate if license has been (attempted) read. */
  private boolean isLicenseRead_;

  /**
   * Create a license manager instance for the specified product.
   *
   * @param companyName           Application vendor company name. Non-null.
   * @param productId             Application unique ID, from build time etc. Non-null.
   * @param licenseRepositoryUrl  URL to the vendors private license repository. Non-null.
   * @param accessToken           License repository access token. Non-null.
   * @throws IllegalArgumentException  If any of the arguments are null.
   */
  public LicenseManager(String companyName, String productId, String licenseRepositoryUrl, String accessToken)
  {
    if (companyName == null)
      throw new IllegalArgumentException("companyName cannot be null");

    if (productId == null)
      throw new IllegalArgumentException("productId cannot be null");

    if (licenseRepositoryUrl == null)
      throw new IllegalArgumentException("licenseRepositoryUrl cannot be null");

    if (accessToken == null)
      throw new IllegalArgumentException("accessToken cannot be null");

    companyName_ = companyName;
    productId_ = productId;
    licenseRepositoryUrl_ = licenseRepositoryUrl;
    accessToken_ = accessToken;
  }

  /**
   * Return license for the present session.
   * It is the clients responsibility to check if the license is valid,
   * @see License.isValid().
   *
   * @return  The license for the present session, or null if no license
   *          was found.
   */
  public License getLicense()
  {
    // Load license for the present product ID on first access
    // and populate the license_ member
    if (!isLicenseRead_)
      loadLicense();

    // No license file found
    if (license_ == null)
      return null;

    //
    // Issue a logging message
    //
    StringBuilder s = new StringBuilder();
    s.append("License: ");
    s.append(license_.getLicensee());
    s.append(" @ ");
    s.append(license_.getHardware());
    s.append(". Expires in " + license_.getNDaysLeft() + " days.");
    logger_.log(Level.INFO, s.toString());

    return license_;
  }

  /**
   * Read license from vendor private repository.
   */
  private void loadLicense()
  {
    String url = licenseRepositoryUrl_ + "/" + productId_ + ".license";

    try {
      HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();

      // Add authorization header for the access token
      String tokenString = "token:" + accessToken_;
      String authorization = Base64.getEncoder().encodeToString(tokenString.getBytes());
      httpConnection.setRequestProperty("Authorization", "Basic " + authorization);

      int responseCode = httpConnection.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {

        // Read JSON response
        BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        StringBuilder responseContent = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
          responseContent.append(line);
          line = reader.readLine();
        }
        reader.close();
        String jsonResponse = responseContent.toString();

        // Parse the JSON response
        JsonReader jsonReader = Json.createReader(new StringReader(jsonResponse));
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();

        // Extract the "content" entry which is the license file content
        String base64Content = jsonObject.getString("content");
        base64Content = base64Content.replace("\n", "");
        String licenseJson = new String(Base64.getDecoder().decode(base64Content));

        // Parse the license JSON into the equivalent license instance
        license_ = parseLicense(licenseJson);
      }
      else {
        logger_.log(Level.WARNING, "Connection failure: " + responseCode);
      }

      httpConnection.disconnect();
    }
    catch (MalformedURLException exception) {
      isLicenseRead_ = false;
      logger_.log(Level.WARNING, "Invalid URL: " + url, exception);
    }
    catch (IOException exception) {
      isLicenseRead_ = false;
      logger_.log(Level.WARNING, "No license found", exception);
    }
  }

  /**
   * Parse license from the specified JSON text.
   *
   * @param json  JSON text holding license. Non-null.
   * @return      Associated license object.
   *              Null if not extractable from the given JSON.
   */
  private License parseLicense(String json)
  {
    assert json != null : "json cannot be null";

    JsonReader jsonReader = Json.createReader(new StringReader(json));
    JsonObject jsonObject = jsonReader.readObject();
    jsonReader.close();

    String licensee = jsonObject.getString("licensee");
    JsonArray hardware = jsonObject.get("hardware") != JsonValue.NULL ? jsonObject.getJsonArray("hardware") : null;
    String product = jsonObject.getString("product");
    JsonArray features = jsonObject.get("features") != JsonValue.NULL ? jsonObject.getJsonArray("features") : null;
    String issuedDateString = jsonObject.getString("issued");
    String expireDateString = jsonObject.get("expire") != JsonValue.NULL ? jsonObject.getString("expire") : null;

    List<String> hardwareIds = new ArrayList<>();
    if (hardware != null) {
      for (JsonValue value : hardware)
        hardwareIds.add(((JsonString) value).getString());
    }

    List<String> featureList = new ArrayList<>();
    if (features != null) {
      for (JsonValue value : features)
        featureList.add(((JsonString) value).getString());
    }

    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    Date issuedDate;
    try {
      issuedDate = dateFormat.parse(issuedDateString);
    }
    catch (ParseException exception) {
      logger_.log(Level.WARNING, "Invalid date string: " + issuedDateString);
      return null;
    }

    Date expireDate;
    try {
      expireDate = expireDateString != null ? dateFormat.parse(expireDateString) : null;
    }
    catch (ParseException exception) {
      logger_.log(Level.WARNING, "Invalid date string: " + expireDateString);
      return null;
    }

    return new License(licensee,
                       companyName_,
                       hardwareIds,
                       product,
                       productId_,
                       featureList,
                       issuedDate,
                       expireDate);
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    return license_ != null ? license_.toString() : "No license";
  }
}
