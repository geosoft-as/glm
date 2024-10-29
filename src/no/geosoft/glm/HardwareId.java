package no.geosoft.glm;

/**
 * Convenience class for returning the hardware ID of the client machine.
 *
 * @author <a href="mailto:jacob.dreyer@petroware.no">Jacob Dreyer</a>
 */
public final class HardwareId
{
  /**
   * Private constructor to prevent client instantiation.
   */
  private HardwareId()
  {
    assert false : "This constructor should never be called";
  }

  /**
   * Return system hardware ID
   *
   * @return  Hardware ID of the system. Null if not present.
   */
  public static String get()
  {
    //
    // MS/Windows
    //
    String response = Command.execute("wmic csproduct get uuid");

    String[] tokens = response != null ? response.split("\\s+") : null;
    if (tokens != null && tokens.length == 2)
      return tokens[1].trim();

    //
    // Linux
    //
    response = Command.execute("cat /etc/machine-id");
    if (response != null && response.length() > 0)
      return response;

    //
    // MacOS
    //
    String[] cmd = { "/bin/sh", "-c", "system_profiler SPHardwareDataType | awk '/UUID/ { print $3; }'" };
    response = Command.execute(cmd);
    if(response != null){
    	return response;
    }

    return null;
  }

  /**
   * Testing this class.
   *
   * @param arguments  Application arguments. Not used.
   */
  static void main(String[] arguments)
  {
    System.out.println(HardwareId.get());
  }
}
