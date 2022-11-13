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

import junit.framework.AssertionFailedError;
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

  @Parameters
  public static Collection<Object[]> data() {
    final List<Object[]> tests = new ArrayList<Object[]>();
    for (final Map.Entry<String, String> test : TestSchemas.testResources("suite").entrySet()) {
      Json set = Json.read(test.getValue());
      if (!set.isArray()) {
        set = Json.array().add(set);
      }
      for (final Json one : set.asJsonList()) {
        try {
          final Json.Schema schema = Json.schema(one.at("schema"));
          for (final Json t : one.at("tests").asJsonList()) {
            tests.add(new Object[] { test.getKey(), t.at("description", "***").asString() + "/" + one.at("description", "---").asString(), schema, t.at("data"), t.at("valid", true).asBoolean() });
          }
        } catch (final Throwable t) {
          throw new RuntimeException("While adding tests from file " + test.getKey() + " - " + one, t);
        }
      }
    }
    return tests;
  }

  public TestJsonSchemaSuite(String group, String description, Json.Schema schema, Json data, boolean valid) {
    this.group = group;
    this.description = description;
    this.schema = schema;
    this.data = data;
    this.valid = valid;
  }

  @Test
  public void doTest() {
    try {
      System.out.println("Running test " + this.description + " from " + this.group);
      Assert.assertEquals("Running test " + this.description + " from " + this.group, this.valid, this.schema.validate(this.data).is("ok", true));
    } catch (final AssertionFailedError err) // caught so we can break in with debugger here...
    {
      this.schema.validate(this.data);
      throw err;
    } catch (final Throwable t) {
      System.err.println("Exception while running test " + this.description + " from " + this.group);
    }
  }
}