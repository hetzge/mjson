package testmjson;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import mjson.Json;

public class TestWriter {
  @Test
  public void testWrite() throws IOException {
    final String jsonString = TestSchemas.readTextResource("/suite/draft2020-12/additionalProperties.json");
    final Json json = Json.read(jsonString);
    final StringWriter writer = new StringWriter();
    json.write(writer);
    assertEquals(json.toString(), writer.toString());
  }

}