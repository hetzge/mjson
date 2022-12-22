package testmjson;

import static mjson.Json.array;
import static mjson.Json.object;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import mjson.Json;

public class TestIterator {
  @Test
  public void testBoolean() {
    final Json b1 = Json.make(true);
    final Iterator<Json> iter = b1.iterator();
    assertNotNull(iter);
    assertEquals(true, iter.hasNext());
    final Json val = iter.next();
    assertEquals(true, val.asBoolean());
    assertEquals(false, iter.hasNext());
  }

  @Test
  public void testNil() {
    final Json nil = Json.nil();
    final Iterator<Json> iter = nil.iterator();
    assertNotNull(iter);
    assertEquals(true, iter.hasNext());
    final Json val = iter.next();
    Assertions.assertTrue(val.isNull());
    assertEquals(false, iter.hasNext());
  }

  @Test
  public void testNumber() {
    final Json n1 = Json.make(567);
    final Iterator<Json> iter = n1.iterator();
    assertNotNull(iter);
    assertEquals(true, iter.hasNext());
    final Json val = iter.next();
    assertEquals(567, val.asInteger());
    assertEquals(false, iter.hasNext());
  }

  @Test
  public void testString() {
    final Json s1 = Json.make("Hello");
    final Iterator<Json> iter = s1.iterator();
    assertNotNull(iter);
    assertEquals(true, iter.hasNext());
    final Json val = iter.next();
    assertEquals("Hello", val.asString());
    assertEquals(false, iter.hasNext());
  }

  @Test
  public void testArray() {
    final Json numbers = array(4, 3, 7);
    final Iterator<Json> iter = numbers.iterator();
    assertNotNull(iter);
    assertEquals(true, iter.hasNext());
    Json val = iter.next();
    assertEquals(4, val.getValue());
    assertEquals(true, iter.hasNext());
    val = iter.next();
    assertEquals(3, val.getValue());
    assertEquals(true, iter.hasNext());
    val = iter.next();
    assertEquals(7, val.getValue());
    assertEquals(false, iter.hasNext());
  }

  @Test
  public void testObject() {
    final Json o1 = object("p", 1, "p2", "p2value");
    final Iterator<Entry<String, Json>> iter = o1.asJsonMap().entrySet().iterator();
    assertNotNull(iter);
    assertEquals(true, iter.hasNext());
    Map.Entry<String, Json> val = iter.next();
    assertEquals("p", val.getKey());
    assertEquals(1, val.getValue().getValue());
    assertEquals(true, iter.hasNext());
    val = iter.next();
    assertEquals("p2", val.getKey());
    assertEquals("p2value", val.getValue().getValue());
    assertEquals(false, iter.hasNext());
  }
}
