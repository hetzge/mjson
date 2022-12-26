package mjson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonSchemaTest {

  @Nested
  static class resolveUri {
    @Test
    void test_resolveUri() {
      // Given
      final URI inputUri = URI.create("https://json-schema.org/draft/2020-12/meta/core");
      final String sub = "meta/core#/$defs/anchorString";
      // When
      final URI outputUri = JsonSchema.resolveUri(inputUri, sub);
      // Then
      assertEquals(URI.create("https://json-schema.org/draft/2020-12/meta/core#/$defs/anchorString"), outputUri);
    }
  }
}
