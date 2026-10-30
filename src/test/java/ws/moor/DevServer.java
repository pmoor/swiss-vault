package ws.moor;

import java.io.IOException;
import java.net.URL;

/** The dev server that maven starts for the integration tests. */
public final class DevServer {

  public static final String BASE_URL = "http://localhost:" + System.getProperty("it.port", "8080");

  private DevServer() {}

  /** The dev server is started asynchronously by maven, give it some time to come up. */
  public static void awaitStartup() throws InterruptedException {
    for (int attempt = 0; attempt < 120; attempt++) {
      try {
        new URL(BASE_URL + "/").openConnection().connect();
        return;
      } catch (IOException e) {
        Thread.sleep(500);
      }
    }
    throw new IllegalStateException("dev server did not start at " + BASE_URL);
  }
}
