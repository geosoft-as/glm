package no.geosoft.glm;

/**
 * Testing this library.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
final class TestApplication
{
  /**
   * Create a test application.
   */
  public TestApplication()
  {
    LicenseManager licenseManager = new LicenseManager("GeoSoft",
                                                       "101",
                                                       "https://api.github.com/repos/geosoft-as/database/contents",
                                                       "<token here>");
    License license = licenseManager.getLicense();
    System.out.println(license);

    System.out.println("---");

    System.out.println("Days left: " + license.getNDaysLeft());
    System.out.println("Expired: " + license.isExpired());
    System.out.println("Is valid for this hardware: " + license.isValidForThisHardware());
  }

  /**
   * Test program
   *
   * @param arguments  Application arguments. Not used.
   */
  public static void main(String[] arguments)
  {
    TestApplication a = new TestApplication();
  }
}

