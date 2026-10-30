package ws.moor;

import com.google.common.truth.Truth;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ws.moor.categories.Integration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Category(Integration.class)
public class EndToEndTest {

  private static final String BASE_URL = "http://localhost:" + System.getProperty("it.port", "8080");

  @BeforeClass
  public static void waitForDevServer() throws InterruptedException {
    // the dev server is started asynchronously by maven, give it some time to come up
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

  @Test
  public void initialAuthRedirect() throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/").openConnection();
    connection.setInstanceFollowRedirects(false);

    Truth.assertThat(connection.getResponseCode()).isEqualTo(HttpURLConnection.HTTP_MOVED_TEMP);
    Truth.assertThat(connection.getHeaderField("Location")).startsWith("https://accounts.google.com/o/oauth2/auth");

    connection.disconnect();
  }
}
