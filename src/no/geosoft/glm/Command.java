package no.geosoft.glm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class for conveniently executing a shell command on the
 * present system.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class Command
{
  /**
   * Private constructor to prevent client instantiation.
   */
  private Command()
  {
    assert false : "This constructor should never be called";
  }

  /**
   * Execute the specified command.
   *
   * @param command  Command to execute. Non-null.
   * @return         The command response. Null if something failed.
   * @throws IllegalArgumentException  If command is null.
   */
  public static String execute(String command)
  {
    if (command == null)
      throw new IllegalArgumentException("command cannot be null");

    StringBuilder response = new StringBuilder();

    try {
      Process process = Runtime.getRuntime().exec(command);

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line = reader.readLine();
      while (line != null) {
        response.append(line);
        line = reader.readLine();
      }
      reader.close();
    }
    catch (IOException exception) {
      // Fine. Ignore.
      return null;
    }

    return response.toString().trim();
  }

  /**
   * Execute the specified command. Supports piping.
   *
   * @param command  Command to execute. Non-null.
   * @return         The command response. Null if something failed.
   * @throws IllegalArgumentException  If command is null.
   */
  public static String execute(String[] command)
  {
    if (command == null)
      throw new IllegalArgumentException("command cannot be null");

    String response = null;

    try {
      Process process = Runtime.getRuntime().exec(command);

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      response = reader.readLine();

      reader.close();
    }
    catch (IOException exception) {
      // Fine. Ignore.
      return null;
    }

    return response;
  }
}
