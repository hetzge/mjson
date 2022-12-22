package testmjson;

import static mjson.Json.array;
import static mjson.Json.make;
import static mjson.Json.object;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mjson.Json;

public class TestEnclosing {
  @Test
  public void testParentObject() {
    final Json obj = object();
    final Json s = make("hi");
    obj.set("greet", s);
    assertTrue(obj == s.up());

    final Json nested = object();
    obj.set("nested", nested);
    assertTrue(obj == nested.up());

    nested.set("parent", obj);
    assertTrue(nested == obj.up());

    final Json nested2 = object();
    obj.set("nested2", nested2);
    nested2.set("parent", obj);
    assertTrue(obj == nested2.up());
    assertTrue(obj.up().asJsonList().contains(nested2));
  }

  @Test
  public void testParentArray() {
    final Json arr = array();
    final Json i = make(10);
    arr.add(i);
    assertTrue(i.up() == arr);
    System.out.println(i.up());
    final Json arr2 = array();
    arr2.add(i);

    System.out.println(i.up());
    assertTrue(i.up().asJsonList().contains(arr));
    assertTrue(i.up().asJsonList().contains(arr2));
  }

  @Test
  public void testToStringOfCircularObject() {
    final Json x = Json.object("name", "x", "tuple", Json.array());
    final Json y = Json.object("backref", x);
    x.at("tuple").add(y);
    final String asstring = x.toString();
    assertTrue(asstring.contains("tuple"));
  }

  /**
   * When we duplicate a deeply nested JSON structure, the parent chains must be
   * properly replicated.
   */
  @Test
  public void testDuplicateSimpleTree() {

  }

  @Test
  public void testDuplicateGraphWithCycles() {

  }
}