package testmjson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import mjson.Json;
import mjson.JsonSchema;

/**
 * Run a test from the https://github.com/json-schema/JSON-Schema-Test-Suite
 * test spec.
 *
 * @author Borislav Iordanov
 *
 */
public class TestJsonSchemaSuite {

  public static Collection<Object[]> data() {
    final List<Object[]> tests = new ArrayList<Object[]>();
    for (final Map.Entry<String, String> test : TestSchemas.testResources("src/test/resources/suite/draft2020-12").entrySet()) {
      Json set = Json.read(test.getValue());
      if (!set.isArray()) {
        set = Json.array().add(set);
      }
      for (final Json one : set.asJsonList()) {
        try {
          for (final Json t : one.at("tests").asJsonList()) {
            tests.add(new Object[] { test.getKey(), t.at("description", "***").asString() + "/" + one.at("description", "---").asString(), one, t });
          }
        } catch (final Throwable t) {
          throw new RuntimeException("While adding tests from file " + test.getKey() + " - " + one, t);
        }
      }
    }
    return tests;
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("data")
  public void doTest(String group, String description, Json one, Json test) {
    final Json.Schema schema = JsonSchema.initialize(one.at("schema"));
    final Json data = test.at("data");
    final boolean valid = test.at("valid", true).asBoolean();
    final Json result = schema.validate(data);
    assertEquals(valid, result.is("ok", true), String.format("data:\n%s\nschema:\n%s\nexpected:\n%s\nresult:\n%s\n", data, schema.toJson().toString(), valid, result.toString()));
  }
}