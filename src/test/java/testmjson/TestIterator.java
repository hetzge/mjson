package testmjson;

import static mjson.Json.array;
import static mjson.Json.object;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import mjson.Json;

public class TestIterator {
  @Test
  public void testBoolean() {
    final Json b1 = Json.make(true);
    final Iterator<Json> iter = b1.iterator();
    Assert.assertNotNull(iter);
    Assert.assertEquals(true, iter.hasNext());
    final Json val = iter.next();
    Assert.assertEquals(true, val.asBoolean());
    Assert.assertEquals(false, iter.hasNext());
  }

  @Test
  public void testNil() {
    final Json nil = Json.nil();
    final Iterator<Json> iter = nil.iterator();
    Assert.assertNotNull(iter);
    Assert.assertEquals(true, iter.hasNext());
    final Json val = iter.next();
    Assert.assertTrue(val.isNull());
    Assert.assertEquals(false, iter.hasNext());
  }

  @Test
  public void testNumber() {
    final Json n1 = Json.make(567);
    final Iterator<Json> iter = n1.iterator();
    Assert.assertNotNull(iter);
    Assert.assertEquals(true, iter.hasNext());
    final Json val = iter.next();
    Assert.assertEquals(567, val.asInteger());
    Assert.assertEquals(false, iter.hasNext());
  }

  @Test
  public void testString() {
    final Json s1 = Json.make("Hello");
    final Iterator<Json> iter = s1.iterator();
    Assert.assertNotNull(iter);
    Assert.assertEquals(true, iter.hasNext());
    final Json val = iter.next();
    Assert.assertEquals("Hello", val.asString());
    Assert.assertEquals(false, iter.hasNext());
  }

  @Test
  public void testArray() {
    final Json numbers = array(4, 3, 7);
    final Iterator<Json> iter = numbers.iterator();
    Assert.assertNotNull(iter);
    Assert.assertEquals(true, iter.hasNext());
    Json val = iter.next();
    Assert.assertEquals(4, val.getValue());
    Assert.assertEquals(true, iter.hasNext());
    val = iter.next();
    Assert.assertEquals(3, val.getValue());
    Assert.assertEquals(true, iter.hasNext());
    val = iter.next();
    Assert.assertEquals(7, val.getValue());
    Assert.assertEquals(false, iter.hasNext());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testObject() {
    final Json o1 = object("p", 1, "p2", "p2value");
    final Iterator<Entry<String, Json>> iter = o1.asJsonMap().entrySet().iterator();
    Assert.assertNotNull(iter);
    Assert.assertEquals(true, iter.hasNext());
    Map.Entry<String, Json> val = iter.next();
    Assert.assertEquals("p", val.getKey());
    Assert.assertEquals(1, val.getValue().getValue());
    Assert.assertEquals(true, iter.hasNext());
    val = iter.next();
    Assert.assertEquals("p2", val.getKey());
    Assert.assertEquals("p2value", val.getValue().getValue());
    Assert.assertEquals(false, iter.hasNext());
  }
}
