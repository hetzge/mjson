package testmjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import mjson.Json;
import mjson.Json.MalformedJsonException;

class TestParser {
  @Test
  void malformedTest1() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("{\"value2\":\"years\"\"value3\":\"wtf\"}");
    });
  }

  @Test
  void malformedTest2() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("{\"x\":\"5\",\"y\":\"wtf\"");
    });
  }

  @Test
  void malformedTest3() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("{\"value2\"\"years\",\"value3\":\"wtf\"}");
    });
  }

  @Test
  void malformedTest4() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("{\"a\":true, }");
    });
  }

  @Test
  void malformedTest5() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("[43 45]");
    });
  }

  @Test
  void malformedTest6() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("[true, false, ]");
    });
  }

  @Test
  void malformedTest7() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("[true, 10, \"asdf\"");
    });
  }

  @Test
  void malformedTest8() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("{[}");
    });
  }

  @Test
  void malformedTest9() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("{\"a\":[}");
    });
  }

  @Test
  void malformedTest10() {
    assertThrows(MalformedJsonException.class, () -> {
      Json.read("[{]");
    });
  }

  @Test
  void parsePrimitives() {
    assertEquals(Json.nil(), Json.read("null"));
    assertEquals(Json.make(23), Json.read("23"));
    assertEquals(Json.make(0.345), Json.read("0.345"));
    assertEquals(Json.make(""), Json.read("\"\""));
    assertEquals(Json.make("hell\""), Json.read("\"hell\\\"\""));
    assertEquals(Json.make(true), Json.read("true"));
    assertEquals(Json.make(false), Json.read("false"));
  }

  @Test
  void parseArrays() {
    assertEquals(Json.array(), Json.read("[]"));
    assertEquals(Json.array(1, 2, 3), Json.read("[1,2,3]"));
    assertEquals(Json.array(10.3, "blabla", true), Json.read("[10.3,  \"blabla\", true]"));
  }

  @Test
  void parseObjects() {
    assertEquals(Json.object(), Json.read("{}"));
    assertEquals(Json.object("one", 1, "maybe", false, "nothing", null), Json.read("\t{\"one\":1, \t    \"maybe\":false , \n\n\"nothing\"    :     null} "));
  }

  @Test
  void parseSomeDeepStructures() {
    final Json j1 = Json.read(TU.resource("/parseme1.json"));
    j1.is("ok", true);
    j1.at("doc").at("content").at(0).is("type", "discourseContainer");
    assertEquals(Json.array(Json.object(), Json.object("x", null), null), Json.read("[{},{\"x\":null},null]"));
  }

}
