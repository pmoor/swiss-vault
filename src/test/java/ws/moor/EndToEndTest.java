package ws.moor;

import com.google.common.truth.Truth;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ws.moor.categories.Integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Category(Integration.class)
public class EndToEndTest {

  @BeforeClass
  public static void waitForDevServer() throws InterruptedException {
    DevServer.awaitStartup();
  }

  @Test
  public void initialAuthRedirect() throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(DevServer.BASE_URL + "/").openConnection();
    connection.setInstanceFollowRedirects(false);

    Truth.assertThat(connection.getResponseCode()).isEqualTo(HttpURLConnection.HTTP_MOVED_TEMP);
    Truth.assertThat(connection.getHeaderField("Location")).startsWith("https://accounts.google.com/o/oauth2/auth");

    connection.disconnect();
  }

  @Test
  public void pythonClientIntegration() throws Exception {
    ProcessBuilder pb = new ProcessBuilder("python3", "client/test_client.py", DevServer.BASE_URL);
    pb.redirectErrorStream(true);
    Process process = pb.start();

    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }
    }
    int exitCode = process.waitFor();
    Truth.assertWithMessage("Python integration test failed:\n" + output)
        .that(exitCode).isEqualTo(0);
  }
}
