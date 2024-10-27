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
import javax.json.JsonValue;

/**
 * License files have the following format (aligned for readability only):
 * <pre>
 *   Licensee   = AkerBP
 *   HardwareId = 11111,22222,33333,... (system UUID addresses or blank for all, see HardwareId.java)
 *   Product    = Petrel,
 *   Features   = Account, Survilance, Read, Write,...
 *   Issued     = 18.03.2027
 *   Expire     = 18.04.2028 (or blank if never)
 * </pre>
 * The file must be named &lt;productId&gt;.license and uploaded to &lt;ProductUrl&gt;/Licenses/
 * where it will be accessed by the program.
 *
 * @author <a href="mailto:jacob.dreyer@petroware.no">Jacob Dreyer</a>
 */
public final class LicenseManager
{
  /** The logger instance. */
  private final static Logger logger_ = Logger.getLogger(LicenseManager.class.getName());

  /** Format used for dates in the license file. */
  private final static String DATE_FORMAT = "dd.MM.yyyy";

  /** The application currently running. Non-null. */
  private final IApplication application_;

  /** License of present session. Lazily created. Null initially and if reading fails. */
  private License license_;

  /** Indicate if license has been (attempted) read. */
  private boolean isLicenseRead_;

  /**
   * Create a license manager instance.
   *
   * @param application  Owner application. Non-null.
   * @throws IllegalArgumentException  If application is null.
   */
  LicenseManager(IApplication application)
  {
    if (application == null)
      throw new IllegalArgumentException("Application cannot be null");

    application_ = application;
  }

  /**
   * Read license from GitHub.
   */
  private void readGitHubLicense()
  {
    String productId = application_.getProductId();
    String repositoryOwner = application_.getLicenseRepositoryOwner();
    String repositoryName = application_.getLicenseRepositoryName();
    String repositoryToken = application_.getLicenseRepositoryToken();

    String url = "https://api.github.com/repos/" + repositoryOwner + "/" + repositoryName + "/contents/" + productId + ".license";

    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

      // Add authorization header
      String tokenString = "token:" + repositoryToken;
      String authorization = Base64.getEncoder().encodeToString(tokenString.getBytes());

      connection.setRequestProperty("Authorization", "Basic " + authorization);

      int responseCode = connection.getResponseCode();

      // Check if the request was successful
      if (responseCode == 200) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        // Read JSON response
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

        // Extract the content
        String base64Content = jsonObject.getString("content");
        base64Content = base64Content.replace("\n", "");
        String licenseJson = new String(Base64.getDecoder().decode(base64Content));

        license_ = parseLicense(licenseJson);
      }
      else {
        logger_.log(Level.WARNING, "Connection to license repository failed: " + responseCode);
      }

      connection.disconnect();
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
        hardwareIds.add(value.toString());
    }

    List<String> featureList = new ArrayList<>();
    if (features != null) {
      for (JsonValue value : features)
        featureList.add(value.toString());
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
                       application_.getCompany(),
                       hardwareIds,
                       product,
                       application_.getProductId(),
                       featureList,
                       issuedDate,
                       expireDate);
  }

  /**
   * Return the license for the given product on the present
   * hardware.
   *
   * @return  The license for the present application on this
   *          particular hardware. Null if none.
   */
  public License getLicense()
  {
    // Read license for the present product ID on first access
    if (!isLicenseRead_)
      readGitHubLicense();

    // No license file found
    if (license_ == null)
      return null;

    //
    // Issue a logging message
    //
    StringBuilder s = new StringBuilder();
    s.append("License OK: ");
    s.append(license_.getLicensee());
    s.append(" @ ");

    // If the license is for a particular hardware, check hardware
    if (!license_.isForAllHardware()) {
      String hardwareId = HardwareId.get();
      if (hardwareId != null)
        logger_.log(Level.INFO, "System UUID: " + hardwareId);

      if (!license_.isValidForHardwareId(hardwareId))
        return null;

      s.append(hardwareId);
    }
    else
      s.append ("any hardware");

    s.append(". Expires in " + license_.getNDaysLeft() + " days.");

    logger_.log(Level.INFO, s.toString());

    return license_;
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    return license_ != null ? license_.toString() : "No license";
  }


  public static class LogScene implements IApplication
  {
    public String getCompany()
    {
      return "GeoSoft";
    }

    public String getProductId()
    {
      return "101";
    }

    public String getInstallationDirectory()
    {
      return null;
    }

    public String getLicenseRepositoryOwner()
    {
      return "geosoft-as";
    }

    public String getLicenseRepositoryName()
    {
      return "licenses";
    }

    public String getLicenseRepositoryToken()
    {
      return "<token>";
    }
  }

  /**
   * Testing this class
   *
   * @param arguments  Application arguments. Not used.
   */
  public static void main(String[] arguments)
  {
    IApplication application = new LogScene();
    LicenseManager licenseManager = new LicenseManager(application);
    License license = licenseManager.getLicense();

    System.out.println(license);
  }
}
