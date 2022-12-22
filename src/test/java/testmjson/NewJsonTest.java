package testmjson;

import org.junit.jupiter.api.Test;

import mjson.Json;
import mjson.JsonSchema;

class NewJsonTest {

  @Test
  void test() {
    // Given
    final Json schema = Json.read("{\n" + "    \"$id\": \"https://example.com/root.json\",\n" + "    \"$defs\": {\n" + "        \"A\": { \"$anchor\": \"foo\" },\n" + "        \"B\": {\n" + "            \"$id\": \"other.json\",\n" + "            \"$defs\": {\n" + "                \"X\": { \"$anchor\": \"bar\" },\n" + "                \"Y\": {\n" + "                    \"$id\": \"t/inner.json\",\n" + "                    \"$anchor\": \"bar\"\n" + "                }\n" + "            }\n" + "        },\n" + "        \"C\": {\n" + "            \"$id\": \"urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f\"\n" + "        }\n" + "    }\n" + "}\n" + "");
    // When
    JsonSchema.initialize(schema);

  }

  @Test
  void test_$ref() {
    // Given
    final Json schema = Json.read("{\n" + "    \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "    \"$id\": \"https://test.json-schema.org/root\",\n" + "    \"$ref\": \"intermediate-scope\",\n" + "    \"$defs\": {\n" + "      \"foo\": {\n" + "        \"$dynamicAnchor\": \"items\",\n" + "        \"type\": \"string\"\n" + "      },\n" + "      \"intermediate-scope\": {\n" + "        \"$id\": \"intermediate-scope\",\n" + "        \"$ref\": \"list\"\n" + "      },\n" + "      \"list\": {\n" + "        \"$id\": \"list\",\n" + "        \"type\": \"array\",\n" + "        \"items\": {\n" + "          \"$dynamicRef\": \"#items\"\n" + "        },\n" + "        \"$defs\": {\n" + "          \"items\": {\n" + "            \"$comment\": \"This is only needed to satisfy the bookending requirement\",\n" + "            \"$dynamicAnchor\": \"items\"\n" + "          }\n" + "        }\n" + "      }\n" + "    }\n" + "  }");
    // When
    final JsonSchema jsonSchema = JsonSchema.initialize(schema);
    final Json result = jsonSchema.validate(Json.read("[\n" + "        123,\n" + "        \"bar\"\n" + "      ]"));
    System.out.println(result);
  }

}
