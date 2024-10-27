package no.geosoft.glm;

public final class TestApplication
{
  public TestApplication()
  {
    LicenseManager licenseManager = new LicenseManager("GeoSoft",
                                                       "101",
                                                       "https://api.github.com/repos/geosoft-as/licenses/contents/",
                                                       "copy from ./token.txt");
    License license = licenseManager.getLicense();
    System.out.println(license);

    System.out.println("---");

    System.out.println("Days left: " + license.getNDaysLeft());
    System.out.println("Expired: " + license.isExpired());
    System.out.println("Is valid for this hardware: " + license.isValidForThisHardware());
  }

  public static void main(String[] arguments)
  {
    TestApplication a = new TestApplication();
  }
}

