package testmjson;

import static mjson.Json.array;
import static mjson.Json.make;
import static mjson.Json.object;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import mjson.Json;

/**
 * Created by biordanov on 10/31/2014.
 */
public class TestWithOptions {

  @Test
  public void testObjectMerge() {
    final Json o1 = object("id", 2, "name", "John", "address", object("streetName", "Main", "streetNumber", 20, "city", "Detroit"));
    final Json o2 = o1.dup().set("age", 20).at("address").delAt("city").up();
    o1.with(o2, "merge");
    Assertions.assertTrue(o1.is("age", 20));
    Assertions.assertTrue(o1.at("address").is("city", "Detroit"));
  }

  @Test
  public void testSortedArrayMerge() {
    final Json a1 = array(1, 2, 20, 30, 50);
    final Json a2 = array(0, 2, 20, 30, 35, 40, 51);
    a1.with(a2, "sort");
    Assertions.assertEquals(array(0, 1, 2, 20, 30, 35, 40, 50, 51), a1);
  }

  @Test
  public void testUnsortedArrayMerge() {
    final Json a1 = array(4, 35, 1, 65, 2, 456);
    final Json a2 = array(65, 5, 3534, 4);
    a1.with(a2, object("sort", false));
    Assertions.assertEquals(TU.set(a1.asJsonList()), TU.set(array(4, 35, 1, 65, 2, 456, 65, 5, 3534, 4).asJsonList()));
  }

  @Test
  public void testCompareEqualsInObject() {
    final Json x1 = object("id", 4, "name", "Tom");
    final Json x2 = object("id", 4, "name", "Hanna");
    Json a1 = array(object("person", x1));
    final Json a2 = array(object("person", x2));
    a1.with(a2, new Json[0]);
    Assertions.assertEquals(2, a1.asJsonList().size());
    a1 = array(object("person", x1));
    a1.with(a2, object("compareBy", "id"));
    Assertions.assertEquals(1, a1.asJsonList().size());
    Assertions.assertEquals(make("Tom"), a1.at(0).at("person").at("name"));
  }

  @Test
  public void testCompareEqualsInArray() {

  }

  @Test
  public void testCompareOrderArray() {

  }
}