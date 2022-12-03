package testmjson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import mjson.Json;

/**
 * Run a test from the https://github.com/json-schema/JSON-Schema-Test-Suite
 * test spec.
 *
 * @author Borislav Iordanov
 *
 */
@RunWith(Parameterized.class)
public class TestJsonSchemaSuite {
  private final String group;
  private final String description;
  private final Json.Schema schema;
  private final Json data;
  private final boolean valid;

  @Parameters(name = "{1}")
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

  public TestJsonSchemaSuite(String group, String description, Json one, Json test) {
    this.group = group;
    this.description = description;
    this.schema = Json.schema(one.at("schema"));
    this.data = test.at("data");
    this.valid = test.at("valid", true).asBoolean();
  }

  @Test
  public void doTest() {
    final Json result = this.schema.validate(this.data);
    Assert.assertEquals(String.format("data:\n%s\nschema:\n%s\nexpected:\n%s\nresult:\n%s\n", this.data, this.schema.toJson().toString(), this.valid, result.toString()), this.valid, result.is("ok", true));
  }
}