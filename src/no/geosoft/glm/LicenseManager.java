package no.geosoft.glm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import no.petroware.cc.util.Crypto;

/**
 * License files have the following format (aligned for readability only):
 * <pre>
 *   Licensee   = Best Buy
 *   HardwareId = 11111,22222,33333,... (system UUID addresses or blank for all, see HardwareId.java)
 *   Product    = CutomerView
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
   * Read license from the license server and populate the
   * license_ and the isLicenseRead_ members.
   */
  private void readLicense()
  {
    String licenseUrl = application_.getLicenseUrl();
    String productId = application_.getProductId();

    try {
      URL licenseFileUrl = new URL(licenseUrl + "/" + productId + ".license");
      logger_.log(Level.INFO, "Getting license from " + licenseUrl + " ...");
      license_ = readLicense(licenseFileUrl);
      isLicenseRead_ = true;
    }
    catch (MalformedURLException exception) {
      isLicenseRead_ = false;
      logger_.log(Level.WARNING, "No license file found @ " + licenseUrl, exception);
    }
    catch (IOException exception) {
      isLicenseRead_ = false;
      logger_.log(Level.WARNING, "No license file found @ " + licenseUrl, exception);
    }
  }

  /**
   * May be used as a fallback if net-based access is not applicable.
   */
  private void readLicenseFromInstallationFolder()
  {
    File licenseFile = new File(application_.getInstallationDirectory(), application_.getProductId() + ".license");

    try {
      URL licenseFileUrl = licenseFile.toURI().toURL();
      logger_.log(Level.INFO, "Getting license from " + licenseFile + " ...");
      license_ = readLicense(licenseFileUrl);
      isLicenseRead_ = true;
    }
    catch (MalformedURLException exception) {
      isLicenseRead_ = false;
      logger_.log(Level.WARNING, "No license file found @ " + licenseFile, exception);
    }
    catch (IOException exception) {
      isLicenseRead_ = false;
      logger_.log(Level.WARNING, "No license file found @ " + licenseFile, exception);
    }
  }

  /**
   * Read the license file at the specified URL.
   *
   * @param url  URL to license file. Non-null.
   * @return     The associated license, or null if the location doesn't
   *             contain a proper license file.
   * @throws IOException  If the reading fails for some reason.
   */
  private License readLicense(URL url)
    throws IOException
  {
    assert url != null : "url cannot be null";

    // The product ID is in the URL itself
    String path = url.toString();
    int pos1 = path.lastIndexOf('/');
    int pos2 = path.lastIndexOf(".license");
    if (pos1 == -1 || pos2 == -1)
      return null;

    String productId = path.substring(pos1 + 1, pos2);
    assert productId.equals(application_.getProductId()) : "Programming error";

    // Establish connection
    URLConnection urlConnection = url.openConnection();

    // Read all bytes
    InputStream inputStream = urlConnection.getInputStream();
    byte[] bytes = new byte[1000];
    int nBytes = inputStream.read(bytes);
    inputStream.close();

    if (nBytes <= 0)
      throw new IOException("Unable to read " + url + ". No content.");

    // Convert to string
    String text = new String(bytes, 0, nBytes);

    // Decrypt if encrypted
    //if (!text.contains("Licensee"))
    //  text = Crypto.aesDecrypt(text);

    //
    // Parse the string into a license if possible
    //
    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    BufferedReader reader = new BufferedReader(new StringReader(text));

    String licensee = null;
    String hardwareIdString = null;
    String product = null;
    String featureString = null;
    String issuedDateString = null;
    String expireDateString = null;

    while (true) {
      String line = reader.readLine();

      //
      // Reading is done. Create a license instance if possible
      //
      if (line == null) {

        // Return null if the license specification is incomplete
        if (licensee == null || issuedDateString == null)
          return null;

        // Features
        List<String> features = new ArrayList<>();
        if (featureString != null) {
          for (String feature : featureString.split(","))
            features.add(feature.trim());
        }

        // Hardware
        List<String> hardwareIds = new ArrayList<>();
        if (hardwareIdString != null) {
          for (String hardwareId : hardwareIdString.split(","))
            hardwareIds.add(hardwareId.trim());
        }

        // Issued date
        Date issuedDate;
        try {
          issuedDate = dateFormat.parse(issuedDateString);
        }
        catch (ParseException exception) {
          logger_.log(Level.WARNING, "Invalid date string: " + issuedDateString);
          return null;
        }

        // Expire date
        Date expireDate;
        try {
          expireDate = expireDateString == null || expireDateString.isEmpty() ? null : dateFormat.parse(expireDateString);
        }
        catch (ParseException exception) {
          logger_.log(Level.WARNING, "Invalid date string: " + expireDateString);
          return null;
        }

        License license = new License(licensee,
                                      application_.getCompany(),
                                      hardwareIds,
                                      product,
                                      productId,
                                      features,
                                      issuedDate,
                                      expireDate);

        return license;
      }

      //
      // Comments or blank lines
      //
      if (line.trim().isEmpty() || line.startsWith("#"))
        continue;

      //
      // Content lines
      //
      if (!line.contains("=")) {
        logger_.log(Level.WARNING, "Unrecognized license content: " + line + ". Ignored.");
        continue;
      }

      String[] tokens = line.split("=");

      String key = tokens[0].trim();
      String value = tokens.length > 1 ? tokens[1].trim() : null;

      switch (key) {
        case "Licensee"   : licensee = value; break;
        case "Product"    : product = value; break;
        case "HardwareId" : hardwareIdString = value; break;
        case "Features"   : featureString = value; break;
        case "Issued"     : issuedDateString = value; break;
        case "Expire"     : expireDateString= value; break;
        default :
          logger_.log(Level.WARNING, "Unrecognized license content: " + line + ". Ignored.");
      }
    }
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
      readLicense();

    // Fallback: Read license from installation directory
    if (license_ == null)
      readLicenseFromInstallationFolder();

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

    public String getLicenseUrl()
    {
      return "https://raw.githubusercontent.com/geosoft-as/glm/main/licenses";
    }

    public String getInstallationDirectory()
    {
      return null;
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
