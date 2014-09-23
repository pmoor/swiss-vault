package ws.moor;

import com.google.common.truth.Truth;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ws.moor.categories.Integration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Category(Integration.class)
public class EndToEndTest {

  @Test
  public void initialAuthRedirect() throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:8080/").openConnection();

    Truth.assertThat(connection.getResponseCode()).isEqualTo(HttpURLConnection.HTTP_MOVED_TEMP);
    Truth.assertThat(connection.getHeaderField("Location")).startsWith("https://accounts.google.com/o/oauth2/auth");

    connection.disconnect();
  }
}
