package eu.europa.ted.efx.app;

import java.util.Arrays;
import eu.europa.ted.efx.xpath.XpathContextualizer;

public class MainApp {
  public static void main(String[] args) {
    System.out.println("EFX running: args=" + Arrays.toString(args));
    if (args.length == 0) {
      throw new RuntimeException("Arguments: commandName cmdArg1 cmdArg2 ... (see README.md)");
    }
    if (args[0].equals("contextualize")) {
      if (args.length == 3) {
        final String contextualized = XpathContextualizer.contextualize(args[1], args[2]);
        System.out.println(contextualized);
      } else {
        throw new RuntimeException("contextualize command expecting 2 args: context xpath");
      }
    } else {
      throw new RuntimeException("Unknown command, see README.md for commands.");
    }
  }
}
