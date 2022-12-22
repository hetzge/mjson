package testmjson;

import static mjson.Json.array;
import static mjson.Json.nil;
import static mjson.Json.object;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mjson.Json;

public class TestBasics {
  @Test
  public void testBoolean() {
    final Json b1 = Json.make(true);
    final Json b1dup = b1.dup();
    assertFalse(b1 == b1dup);
    assertEquals(b1, b1dup);
    final Json b2 = Json.factory().bool(Boolean.TRUE);
    assertEquals(b1, b2);
    assertEquals(Boolean.FALSE, Json.make(false).getValue());
  }

  @Test
  public void testNil() {
    final Json nil = Json.nil();
    assertEquals(nil, Json.make(null));
    assertNull(nil.getValue());
    final Json nil2 = nil.dup();
    assertFalse(nil == nil2);
    assertEquals(nil, nil2);
  }

  @Test
  public void testNumber() {
    final Json n1 = Json.make(567);
    final Json n2 = Json.make(567.0);
    final Json n1dup = n1.dup();
    assertFalse(n1 == n1dup);
    assertEquals(n1, n1dup);
    assertEquals(567, n1.asInteger());
    assertEquals(567, n2.asInteger());
    assertEquals(567l, n2.asLong());
    assertEquals(567.0, n2.asFloat(), 0.0);
    assertEquals(567.0, n1.asDouble(), 0.0);
    assertEquals(n1, n2);
    assertEquals(Double.MAX_VALUE, Json.factory().number(Double.MAX_VALUE).getValue());
    assertEquals(Integer.MIN_VALUE, Json.factory().number(Integer.MIN_VALUE).getValue());
  }

  @Test
  public void testString() {
    final Json s1 = Json.make("");
    assertEquals("", s1.getValue());
    final Json s1dup = s1.dup();
    assertFalse(s1dup == s1);
    assertEquals(s1, s1dup);
    final Json s2 = Json.make("string1");
    final Json s2again = Json.factory().string("string1");
    assertEquals(s2, s2again);
    final Json s3 = Json.make("Case Sensitive");
    final Json s3_lower = Json.make("Case Sensitive".toLowerCase());
    assertNotEquals(s3, s3_lower);
  }

  @Test
  public void testArray() {
    final Json empty = array();
    assertTrue(empty.asJsonList().isEmpty());
    final Json numbers = array(4, 3, 4);
    assertEquals(numbers.at(0), numbers.at(2));
    assertSame(numbers, numbers.add(1));
    assertEquals(4, numbers.asJsonList().size());
    assertEquals(1, numbers.at(3).asInteger());
    final Json mixed = array(nil(), "s", 2);
    assertNull(mixed.at(0).getValue());
    final Json mixeddup = mixed.dup();
    assertEquals(mixed, mixeddup);
    assertEquals("s", mixeddup.at(1).getValue());
  }

  @Test
  public void testObject() {
    final Json empty = object();
    assertTrue(empty.asJsonMap().isEmpty());
    final Json o1 = object("p", 1).set("p2", "p2value");
    assertEquals(1, o1.at("p").getValue());
    assertTrue(o1.has("p2"));
    assertFalse(o1.has("p2asdfasd"));
    final Json dup = o1.dup();
    assertEquals(o1, dup);
    dup.set("A", array("23423", 24, 2423, o1));
    assertNotEquals(o1, dup);
  }
}
