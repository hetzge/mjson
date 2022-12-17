/*
 * Copyright (C) 2011 Miami-Dade County.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Note: this file incorporates source code from 3d party entities. Such code
 * is copyrighted by those entities as indicated below.
 */
package mjson;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
//import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * <p>
 * Represents a JSON (JavaScript Object Notation) entity. For more information
 * about JSON, please see
 * <a href="http://www.json.org" target="_">http://www.json.org</a>.
 * </p>
 *
 * <p>
 * A JSON entity can be one of several things: an object (set of name/Json
 * entity pairs), an array (a list of other JSON entities), a string, a number,
 * a boolean or null. All of those are represented as <code>Json</code>
 * instances. Each of the different types of entities supports a different set
 * of operations. However, this class unifies all operations into a single
 * interface so in Java one is always dealing with a single object type: this
 * class. The approach effectively amounts to dynamic typing where using an
 * unsupported operation won't be detected at compile time, but will throw a
 * runtime {@link UnsupportedOperationException}. It simplifies working with
 * JSON structures considerably and it leads to shorter at cleaner Java code. It
 * makes much easier to work with JSON structure without the need to convert to
 * "proper" Java representation in the form of POJOs and the like. When
 * traversing a JSON, there's no need to type-cast at each step because there's
 * only one type: <code>Json</code>.
 * </p>
 *
 * <p>
 * One can examine the concrete type of a <code>Json</code> with one of the
 * <code>isXXX</code> methods: {@link #isObject()},
 * {@link #isArray()},{@link #isNumber()},{@link #isBoolean()},{@link #isString()},
 * {@link #isNull()}.
 * </p>
 *
 * <p>
 * The underlying representation of a given <code>Json</code> instance can be
 * obtained by calling the generic {@link #getValue()} method or one of the
 * <code>asXXX</code> methods such as {@link #asBoolean()} or
 * {@link #asString()} etc. JSON objects are represented as Java {@link Map}s
 * while JSON arrays are represented as Java {@link List}s. Because those are
 * mutable aggregate structures, there are two versions of the corresponding
 * <code>asXXX</code> methods: {@link #asMap()} which performs a deep copy of
 * the underlying map, unwrapping every nested Json entity to its Java
 * representation and {@link #asJsonMap()} which simply return the map
 * reference. Similarly there are {@link #asList()} and {@link #asJsonList()}.
 * </p>
 *
 * <h3>Constructing and Modifying JSON Structures</h3>
 *
 * <p>
 * There are several static factory methods in this class that allow you to
 * create new <code>Json</code> instances:
 * </p>
 *
 * <table>
 * <tr>
 * <td>{@link #read(String)}</td>
 * <td>Parse a JSON string and return the resulting <code>Json</code> instance.
 * The syntax recognized is as defined in
 * <a href="http://www.json.org">http://www.json.org</a>.</td>
 * </tr>
 * <tr>
 * <td>{@link #make(Object)}</td>
 * <td>Creates a Json instance based on the concrete type of the parameter. The
 * types recognized are null, numbers, primitives, String, Map, Collection, Java
 * arrays and <code>Json</code> itself.</td>
 * </tr>
 * <tr>
 * <td>{@link #nil()}</td>
 * <td>Return a <code>Json</code> instance representing JSON
 * <code>null</code>.</td>
 * </tr>
 * <tr>
 * <td>{@link #object()}</td>
 * <td>Create and return an empty JSON object.</td>
 * </tr>
 * <tr>
 * <td>{@link #object(Object...)}</td>
 * <td>Create and return a JSON object populated with the key/value pairs passed
 * as an argument sequence. Each even parameter becomes a key (via
 * <code>toString</code>) and each odd parameter is converted to a
 * <code>Json</code> value.</td>
 * </tr>
 * <tr>
 * <td>{@link #array()}</td>
 * <td>Create and return an empty JSON array.</td>
 * </tr>
 * <tr>
 * <td>{@link #array(Object...)}</td>
 * <td>Create and return a JSON array from the list of arguments.</td>
 * </tr>
 * </table>
 *
 * <p>
 * To customize how Json elements are represented and to provide your own
 * version of the {@link #make(Object)} method, you create an implementation of
 * the {@link Factory} interface and configure it either globally with the
 * {@link #setGlobalFactory(Factory)} method or on a per-thread basis with the
 * {@link #attachFactory(Factory)}/{@link #detachFactory()} methods.
 * </p>
 *
 * <p>
 * If a <code>Json</code> instance is an object, you can set its properties by
 * calling the {@link #set(String, Object)} method which will add a new property
 * or replace an existing one. Adding elements to an array <code>Json</code> is
 * done with the {@link #add(Object)} method. Removing elements by their index
 * (or key) is done with the {@link #delAt(int)} (or {@link #delAt(String)})
 * method. You can also remove an element from an array without knowing its
 * index with the {@link #remove(Object)} method. All these methods return the
 * <code>Json</code> instance being manipulated so that method calls can be
 * chained. If you want to remove an element from an object or array and return
 * the removed element as a result of the operation, call {@link #atDel(int)} or
 * {@link #atDel(String)} instead.
 * </p>
 *
 * <p>
 * If you want to add properties to an object in bulk or append a sequence of
 * elements to array, use the {@link #with(Json, Json...opts)} method. When used
 * on an object, this method expects another object as its argument and it will
 * copy all properties of that argument into itself. Similarly, when called on
 * array, the method expects another array and it will append all elements of
 * its argument to itself.
 * </p>
 *
 * <p>
 * To make a clone of a Json object, use the {@link #dup()} method. This method
 * will create a new object even for the immutable primitive Json types. Objects
 * and arrays are cloned (i.e. duplicated) recursively.
 * </p>
 *
 * <h3>Navigating JSON Structures</h3>
 *
 * <p>
 * The {@link #at(int)} method returns the array element at the specified index
 * and the {@link #at(String)} method does the same for a property of an object
 * instance. You can use the {@link #at(String, Object)} version to create an
 * object property with a default value if it doesn't exist already.
 * </p>
 *
 * <p>
 * To test just whether a Json object has a given property, use the
 * {@link #has(String)} method. To test whether a given object property or an
 * array elements is equal to a particular value, use the
 * {@link #is(String, Object)} and {@link #is(int, Object)} methods
 * respectively. Those methods return true if the given named property (or
 * indexed element) is equal to the passed in Object as the second parameter.
 * They return false if an object doesn't have the specified property or an
 * index array is out of bounds. For example is(name, value) is equivalent to
 * 'has(name) &amp;&amp; at(name).equals(make(value))'.
 * </p>
 *
 * <p>
 * To help in navigating JSON structures, instances of this class contain a
 * reference to the enclosing JSON entity (object or array) if any. The
 * enclosing entity can be accessed with {@link #up()} method.
 * </p>
 *
 * <p>
 * The combination of method chaining when modifying <code>Json</code> instances
 * and the ability to navigate "inside" a structure and then go back to the
 * enclosing element lets one accomplish a lot in a single Java statement,
 * without the need of intermediary variables. Here for example how the
 * following JSON structure can be created in one statement using chained calls:
 * </p>
 *
 * <pre>
 * <code>
 * {"menu": {
 * "id": "file",
 * "value": "File",
 * "popup": {
 *   "menuitem": [
 *     {"value": "New", "onclick": "CreateNewDoc()"},
 *     {"value": "Open", "onclick": "OpenDoc()"},
 *     {"value": "Close", "onclick": "CloseDoc()"}
 *   ]
 * }
 * "position": 0
 * }}
 * </code>
 * </pre>
 *
 * <pre>
 * <code>
 * import mjson.Json;
 * import static mjson.Json.*;
 * ...
 * Json j = object()
 *  .at("menu", object())
 *    .set("id", "file")
 *    .set("value", "File")
 *    .at("popup", object())
 *      .at("menuitem", array())
 *        .add(object("value", "New", "onclick", "CreateNewDoc()"))
 *        .add(object("value", "Open", "onclick", "OpenDoc()"))
 *        .add(object("value", "Close", "onclick", "CloseDoc()"))
 *        .up()
 *      .up()
 *    .set("position", 0)
 *  .up();
 * ...
 * </code>
 * </pre>
 *
 * <p>
 * If there's no danger of naming conflicts, a static import of the factory
 * methods (<code>
 * import static json.Json.*;</code>) would reduce typing even further and make
 * the code more readable.
 * </p>
 *
 * <h3>Converting to String</h3>
 *
 * <p>
 * To get a compact string representation, simply use the {@link #toString()}
 * method. If you want to wrap it in a JavaScript callback (for JSON with
 * padding), use the {@link #pad(String)} method.
 * </p>
 *
 * <h3>Validating with JSON Schema</h3>
 *
 * <p>
 * Since version 1.3, mJson supports JSON Schema, draft 4. A schema is
 * represented by the internal class {@link mjson.Json.Schema}. To perform a
 * validation, you have a instantiate a <code>Json.Schema</code> using the
 * factory method {@link mjson.Json.Schema} and then call its
 * <code>validate</code> method on a JSON instance:
 * </p>
 *
 * <pre>
 * <code>
 * import mjson.Json;
 * import static mjson.Json.*;
 * ...
 * Json inputJson = Json.read(inputString);
 * Json schema = Json.schema(new URI("http://mycompany.com/schemas/model"));
 * Json errors = schema.validate(inputJson);
 * for (Json error : errors.asJsonList())
 * 	   System.out.println("Validation error " + err);
 * </code>
 * </pre>
 *
 * @author Borislav Iordanov
 * @version 2.0.0
 */
public abstract class Json implements java.io.Serializable, Iterable<Json> {

  private static final long serialVersionUID = 1L;
  private static final int MAX_CHARACTERS = 200;

  // https://github.com/ajv-validator/ajv-formats/blob/master/src/formats.ts
  // https://github.com/eclipse-vertx/vertx-json-schema/blob/135da5046be5538365bcf4090e2b8ba07a129838/src/main/java/io/vertx/json/schema/common/RegularExpressions.java
  private static final Map<String, Pattern> PATTERN_BY_FORMAT;
  static {
    PATTERN_BY_FORMAT = new HashMap<>();
    PATTERN_BY_FORMAT.put("date-time", Pattern.compile("^(?:[1-9]\\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:Z|[+-][01]\\d:[0-5]\\d)$"));
    PATTERN_BY_FORMAT.put("date", Pattern.compile("^(?:[1-9]\\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)$"));
    PATTERN_BY_FORMAT.put("time", Pattern.compile("^(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d+)*(?:Z|[+-][01]\\d:[0-5]\\d)$"));
    PATTERN_BY_FORMAT.put("duration", Pattern.compile("^P(?!$)(\\d+(?:\\.\\d+)?Y)?(\\d+(?:\\.\\d+)?M)?(\\d+(?:\\.\\d+)?W)?(\\d+(?:\\.\\d+)?D)?(T(?=\\d)(\\d+(?:\\.\\d+)?H)?(\\d+(?:\\.\\d+)?M)?(\\d+(?:\\.\\d+)?S)?)?$"));
    PATTERN_BY_FORMAT.put("email", Pattern.compile("^(?:[\\w!#\\$%&'\\*\\+\\-/=\\?\\^`\\{\\|\\}~]+\\.)" + "*[\\w!#\\$%&'\\*\\+\\-/=\\?\\^`\\{\\|\\}~]+@(?:(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-](?!\\.)){0,61}[a-zA-Z0-9]?\\.)" + "+[a-zA-Z0-9](?:[a-zA-Z0-9\\-](?!$)){0,61}[a-zA-Z0-9]?)|(?:\\[(?:(?:[01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\.){3}" + "(?:[01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\]))$"));
    PATTERN_BY_FORMAT.put("uuid", Pattern.compile("^(?:urn:uuid:)?[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE));

    PATTERN_BY_FORMAT.put("hostname", Pattern.compile("^(?=.{1,253}\\.?$)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[-0-9a-z]{0,61}[0-9a-z])?)*\\.?$"));
    PATTERN_BY_FORMAT.put("ipv4", Pattern.compile("^(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$"));
    PATTERN_BY_FORMAT.put("ipv6", Pattern.compile(
        "^((([0-9a-f]{1,4}:){7}([0-9a-f]{1,4}|:))|(([0-9a-f]{1,4}:){6}(:[0-9a-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9a-f]{1,4}:){5}(((:[0-9a-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9a-f]{1,4}:){4}(((:[0-9a-f]{1,4}){1,3})|((:[0-9a-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9a-f]{1,4}:){3}(((:[0-9a-f]{1,4}){1,4})|((:[0-9a-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9a-f]{1,4}:){2}(((:[0-9a-f]{1,4}){1,5})|((:[0-9a-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9a-f]{1,4}:){1}(((:[0-9a-f]{1,4}){1,6})|((:[0-9a-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9a-f]{1,4}){1,7})|((:[0-9a-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))$"));
    PATTERN_BY_FORMAT.put("uri-template", Pattern.compile("^(?:(?:[^\\x00-\\x20\"'<>%\\\\^`{|}]|%[0-9a-f]{2})|\\{[+#./;?&=,!@|]?(?:[a-z0-9_]|%[0-9a-f]{2})+(?::[1-9][0-9]{0,3}|\\*)?(?:,(?:[a-z0-9_]|%[0-9a-f]{2})+(?::[1-9][0-9]{0,3}|\\*)?)*\\})*$"));

    PATTERN_BY_FORMAT.put("relative-json-pointer", Pattern.compile("^(([1-9]+0*)+|0{1})(\\/[\\/\\w]*)*#{0,1}$"));
    PATTERN_BY_FORMAT.put("json-pointer", Pattern.compile("^(?:\\/(?:[^~/]|~0|~1)*)*$"));
  }

  /**
   * <p>
   * This interface defines how <code>Json</code> instances are constructed. There
   * is a default implementation for each kind of <code>Json</code> value, but you
   * can provide your own implementation. For example, you might want a different
   * representation of an object than a regular <code>HashMap</code>. Or you might
   * want string comparison to be case insensitive.
   * </p>
   *
   * <p>
   * In addition, the {@link #make(Object)} method allows you plug-in your own
   * mapping of arbitrary Java objects to <code>Json</code> instances. You might
   * want to implement a Java Beans to JSON mapping or any other JSON
   * serialization that makes sense in your project.
   * </p>
   *
   * <p>
   * To avoid implementing all methods in that interface, you can extend the
   * {@link DefaultFactory} default implementation and simply overwrite the ones
   * you're interested in.
   * </p>
   *
   * <p>
   * The factory implementation used by the <code>Json</code> classes is specified
   * simply by calling the {@link #setGlobalFactory(Factory)} method. The factory
   * is a static, global variable by default. If you need different factories in
   * different areas of a single application, you may attach them to different
   * threads of execution using the {@link #attachFactory(Factory)}. Recall a
   * separate copy of static variables is made per ClassLoader, so for example in
   * a web application context, that global factory can be different for each web
   * application (as Java web servers usually use a separate class loader per
   * application). Thread-local factories are really a provision for special
   * cases.
   * </p>
   *
   * @author Borislav Iordanov
   *
   */
  public static interface Factory {
    /**
     * Construct and return an object representing JSON <code>null</code>.
     * Implementations are free to cache a return the same instance. The resulting
     * value must return <code>true</code> from <code>isNull()</code> and
     * <code>null</code> from <code>getValue()</code>.
     *
     * @return The representation of a JSON <code>null</code> value.
     */
    Json nil();

    /**
     * Construct and return a JSON boolean. The resulting value must return
     * <code>true</code> from <code>isBoolean()</code> and the passed in parameter
     * from <code>getValue()</code>.
     *
     * @param value The boolean value.
     * @return A JSON with <code>isBoolean() == true</code>. Implementations are
     *         free to cache and return the same instance for true and false.
     */
    Json bool(boolean value);

    /**
     * Construct and return a JSON string. The resulting value must return
     * <code>true</code> from <code>isString()</code> and the passed in parameter
     * from <code>getValue()</code>.
     *
     * @param value The string to wrap as a JSON value.
     * @return A JSON element with the given string as a value.
     */
    Json string(String value);

    /**
     * Construct and return a JSON number. The resulting value must return
     * <code>true</code> from <code>isNumber()</code> and the passed in parameter
     * from <code>getValue()</code>.
     *
     * @param value The numeric value.
     * @return Json instance representing that value.
     */
    Json number(Number value);

    /**
     * Construct and return a JSON object. The resulting value must return
     * <code>true</code> from <code>isObject()</code> and an implementation of
     * <code>java.util.Map</code> from <code>getValue()</code>.
     *
     * @return An empty JSON object.
     */
    Json object();

    /**
     * Construct and return a JSON object. The resulting value must return
     * <code>true</code> from <code>isArray()</code> and an implementation of
     * <code>java.util.List</code> from <code>getValue()</code>.
     *
     * @return An empty JSON array.
     */
    Json array();

    /**
     * Construct and return a JSON object. The resulting value can be of any JSON
     * type. The method is responsible for examining the type of its argument and
     * performing an appropriate mapping to a <code>Json</code> instance.
     *
     * @param anything An arbitray Java object from which to construct a
     *                 <code>Json</code> element.
     * @return The newly constructed <code>Json</code> instance.
     */
    Json make(Object anything);
  }

  public static interface Function<T, R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(T t);
  }

  /**
   * <p>
   * Represents JSON schema - a specific data format that a JSON entity must
   * follow. The idea of a JSON schema is very similar to XML. Its main purpose is
   * validating input.
   * </p>
   *
   * <p>
   * More information about the various JSON schema specifications can be found at
   * http://json-schema.org. JSON Schema is an IETF draft (v4 currently) and our
   * implementation follows this set of specifications. A JSON schema is specified
   * as a JSON object that contains keywords defined by the specification. Here
   * are a few introductory materials:
   * <ul>
   * <li>http://jsonary.com/documentation/json-schema/ - a very well-written
   * tutorial covering the whole standard</li>
   * <li>http://spacetelescope.github.io/understanding-json-schema/ - online book,
   * tutorial (Python/Ruby based)</li>
   * </ul>
   * </p>
   *
   * @author Borislav Iordanov
   *
   */
  public static interface Schema {
    /**
     * <p>
     * Validate a JSON document according to this schema. The validations attempts
     * to proceed even in the face of errors. The return value is always a
     * <code>Json.object</code> containing the boolean property <code>ok</code>.
     * When <code>ok</code> is <code>true</code>, the return object contains nothing
     * else. When it is <code>false</code>, the return object contains a property
     * <code>errors</code> which is an array of error messages for all detected
     * schema violations.
     * </p>
     *
     * @param document The input document.
     * @return <code>{"ok":true}</code> or
     *         <code>{"ok":false, errors:["msg1", "msg2", ...]}</code>
     */
    Json validate(Json document);

    /**
     * <p>
     * Return the JSON representation of the schema.
     * </p>
     */
    Json toJson();

    /**
     * <p>
     * Possible options are: <code>ignoreDefaults:true|false</code>.
     * </p>
     *
     * @return A newly created <code>Json</code> conforming to this schema.
     */
    // Json generate(Json options);
  }

  public abstract void write(Writer writer) throws IOException;

  @Override
  public Iterator<Json> iterator() {
    return new Iterator<Json>() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Json next() {
        return null;
      }

      @Override
      public void remove() {
      }
    };
  }

  static String fetchContent(URL url) {
    java.io.Reader reader = null;
    try {
      reader = new java.io.InputStreamReader((java.io.InputStream) url.getContent());
      final StringBuilder content = new StringBuilder();
      final char[] buf = new char[1024];
      for (int n = reader.read(buf); n > -1; n = reader.read(buf)) {
        content.append(buf, 0, n);
      }
      return content.toString();
    } catch (final Exception ex) {
      throw new RuntimeException("Failed to fetch: " + url, ex);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (final Throwable t) {
        }
      }
    }
  }

  static Json resolvePointer(String pointerRepresentation, Json top) {
    final String[] parts = pointerRepresentation.split("/");
    Json result = top;
    for (String p : parts) {
      // TODO: unescaping and decoding
      if (p.length() == 0) {
        continue;
      }
      p = p.replace("~1", "/").replace("~0", "~");
      if (result.isArray()) {
        result = result.at(Integer.parseInt(p));
      } else if (result.isObject()) {
        result = result.at(p);
      } else {
        throw new RuntimeException("Can't resolve pointer " + pointerRepresentation + " on document " + top.toString(200));
      }
    }
    return result;
  }

  static URI makeAbsolute(URI base, String ref) throws Exception {
    URI refuri;
    if (base != null && base.getAuthority() != null && !new URI(ref).isAbsolute()) {
      final StringBuilder sb = new StringBuilder();
      if (base.getScheme() != null) {
        sb.append(base.getScheme()).append("://");
      }
      sb.append(base.getAuthority());
      if (!ref.startsWith("/")) {
        if (ref.startsWith("#")) {
          sb.append(base.getPath());
        } else {
          final int slashIdx = base.getPath().lastIndexOf('/');
          sb.append(slashIdx == -1 ? base.getPath() : base.getPath().substring(0, slashIdx)).append("/");
        }
      }
      refuri = new URI(sb.append(ref).toString());
    } else if (base != null) {
      refuri = base.resolve(ref);
    } else {
      refuri = new URI(ref);
    }
    return refuri;
  }

  static Json resolveRef(URI base, Json refdoc, URI refuri, CompileContext context) throws Exception {
    System.out.println("Resolve ref: " + refuri + " on " + refdoc);

    refuri = refuri.normalize();

    if (!refuri.isAbsolute()) {
      refuri = base.resolve(refuri);
    }

    if (refuri.isAbsolute()) {
      if (!context.isResolved(refuri)) {
        refdoc = context.resolveUri(refuri);
        refdoc = expandReferences(null, refdoc, refdoc, refuri, context);
        context.putResolved(refuri, refdoc);
      }
      refdoc = context.getResolved(refuri);
      if (refdoc == null) {
        throw new IllegalStateException("unresolved: " + refuri);
      }
    }

    Json result;
    if (refuri.getFragment() == null) {
      result = refdoc;
    } else {
      final Json anchor = context.getAnchor(refuri);
      if (anchor != null) {
        result = anchor;
      } else {
        result = resolvePointer(refuri.getFragment(), refdoc);
      }
    }
    System.out.println("result: " + result);
    return result;
  }

  /**
   * <p>
   * Replace all JSON references, as per the
   * http://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03 specification, by
   * their referents.
   * </p>
   *
   * @param field    the field the given <code>json</code> is retrieved from, can
   *                 be <code>null</code>
   * @param json     the json the references should be expanded for
   * @param resolved resolved parts by url
   * @param expanded already expanded parts
   * @return the expanded json
   */
  static Json expandReferences(String field, Json json, Json topdoc, URI base, CompileContext context) throws Exception {
    System.out.println("Expand references: " + json);
    if (context.isExpanded(json)) {
      return json;
    }
    if (json.isObject()) {
      // change scope of nest references
      if (json.has("$id") && json.at("$id").isString()) {
        base = base.resolve(json.at("$id").asString());
        context.putResolved(base, json);
      }

      // https://json-schema.org/blog/posts/dynamicref-and-generics
      if (json.has("$dynamicAnchor") && !"properties".equals(field)) {
        context.putDynamicAnchor("#" + json.at("$dynamicAnchor").asString(), json);
        context.putDynamicAnchor(base.resolve("#" + json.at("$dynamicAnchor").asString()).toString(), json);
      }

      if (json.has("$anchor") && !"properties".equals(field)) {
        final String anchor = json.at("$anchor").asString();
        if (base.getScheme() != null && base.getScheme().equals("urn")) {
          context.putAnchor(URI.create(base.toString() + "#" + anchor), json);
        } else {
          System.out.println("base: " + base + ", anchor: " + anchor);
          context.putAnchor(makeAbsolute(base, "#" + anchor), json);
        }
      }

      // Expand $defs before other
      if (json.has("$defs") && !"properties".equals(field) && json.at("$defs").isObject()) {
        json.set("$defs", expandReferences("$defs", json.at("$defs"), topdoc, base, context));
      }

      for (final Map.Entry<String, Json> entry : json.asJsonMap().entrySet()) {
        json.set(entry.getKey(), expandReferences(entry.getKey(), entry.getValue(), topdoc, base, context));
      }

      if (json.has("$ref") && !"properties".equals(field)) {
        final URI refuri = makeAbsolute(base, json.at("$ref").asString());
        final Json resolved = resolveBooleanSchema(resolveRef(base, topdoc, refuri, context));
        json = deepMerge(json, resolved);
      }

      if (json.has("$dynamicRef") && !"properties".equals(field)) {
        final String dynamicRef = json.at("$dynamicRef").asString();
        Json anchor = context.getDynamicAnchor(dynamicRef);
        if (anchor == null) {
          final URI refuri = makeAbsolute(base, json.at("$dynamicRef").asString());
          anchor = resolveBooleanSchema(resolveRef(base, topdoc, refuri, context));
        }
        if (anchor == null) {
          throw new IllegalStateException(String.format("Anchor '%s' not found", dynamicRef));
        }
        json = deepMerge(json, anchor);
      }
    } else if (json.isArray()) {
      for (int i = 0; i < json.asJsonList().size(); i++) {
        json.set(i, expandReferences(null, json.at(i), topdoc, base, context));
      }
    }
    context.markExpanded(json);
    return json;
  }

  private static Json deepMerge(Json a, Json b) {
    if (a != null && b != null && a.isObject() && b.isObject()) {
      final Json result = a.dup();
      for (final Map.Entry<String, Json> entry : b.asJsonMap().entrySet()) {
        result.set(entry.getKey(), deepMerge(a.at(entry.getKey()), entry.getValue()));
      }
      return result;
    } else {
      if (b != null) {
        return b;
      } else {
        return a;
      }
    }
  }

  private static Json resolveBooleanSchema(Json resolvedRef) {
    if (resolvedRef != null && resolvedRef.isBoolean()) {
      if (resolvedRef.asBoolean()) {
        // always match
        resolvedRef = Json.object().set("type", "any");
      } else {
        // never match
        resolvedRef = Json.object().set("not", Json.object().set("type", "any"));
      }
    }
    return resolvedRef;
  }

  static class CompileContext {
    private final Function<URI, Json> uriResolver;
    private final Map<String, Json> resolved;
    private final Map<String, Json> dynamicAnchors;
    private final Map<URI, Json> anchors;
    private final Map<Json, Json> expanded;

    CompileContext(Function<URI, Json> uriResolver) {
      this(uriResolver, new HashMap<String, Json>(), new HashMap<String, Json>(), new HashMap<URI, Json>(), new IdentityHashMap<Json, Json>());
    }

    CompileContext(Function<URI, Json> uriResolver, Map<String, Json> resolved, Map<String, Json> dynamicAnchors, Map<URI, Json> anchors, Map<Json, Json> expanded) {
      this.uriResolver = uriResolver;
      this.resolved = resolved;
      this.dynamicAnchors = dynamicAnchors;
      this.anchors = anchors;
      this.expanded = expanded;
    }

    Json resolveUri(URI uri) {
      if (!isResolved(uri)) {
        putResolved(uri, this.uriResolver.apply(uri));
      }
      return this.resolved.get(stripFragment(uri));
    }

    boolean isResolved(URI uri) {
      return this.resolved.containsKey(stripFragment(uri));
    }

    void putResolved(URI uri, Json json) {
      System.out.println("Resolved " + stripFragment(uri) + " with " + json);
      this.resolved.put(stripFragment(uri), json);
    }

    Json getResolved(URI uri) {
      return this.resolved.get(stripFragment(uri));
    }

    void putDynamicAnchor(String anchor, Json json) {
      System.out.println("Add dynamic anchor " + anchor + " with " + json);
      if (!this.dynamicAnchors.containsKey(anchor)) {
        this.dynamicAnchors.put(anchor, json);
      } else {
        System.out.println("Skip");
      }
    }

    Json getDynamicAnchor(String anchor) {
      return this.dynamicAnchors.get(anchor);
    }

    void putAnchor(URI anchor, Json json) {
      System.out.println("Add anchor " + anchor + " with " + json);
      this.anchors.put(anchor, json);
    }

    Json getAnchor(URI anchor) {
      return this.anchors.get(anchor);
    }

    boolean isExpanded(Json json) {
      return this.expanded.containsKey(json);
    }

    void markExpanded(Json json) {
      this.expanded.put(json, json);
    }

    String stripFragment(URI uri) {
      return uri.toString().replace("#" + uri.getFragment(), "");
    }
  }

  static class DefaultSchema implements Schema {
    static interface Instruction extends Function<Json, Json> {
    }

    static Json maybeError(Json errors, Json error) {
      return error == null ? errors : (errors == null ? Json.array() : errors).with(error, new Json[0]);
    }

    // Anything is valid schema
    static Instruction any = new Instruction() {
      @Override
      public Json apply(Json param) {
        return null;
      }
    };

    // Nothing is valid schema
    static Instruction never = new Instruction() {
      @Override
      public Json apply(Json param) {
        return Json.make("Never valid");
      }
    };

    // Type validation
    class IsObject implements Instruction {
      @Override
      public Json apply(Json param) {
        return param.isObject() ? null : Json.make(param.toString(DefaultSchema.this.maxchars));
      }
    }

    class IsArray implements Instruction {
      @Override
      public Json apply(Json param) {
        return param.isArray() ? null : Json.make(param.toString(DefaultSchema.this.maxchars));
      }
    }

    class IsString implements Instruction {
      @Override
      public Json apply(Json param) {
        return param.isString() ? null : Json.make(param.toString(DefaultSchema.this.maxchars));
      }
    }

    class IsBoolean implements Instruction {
      @Override
      public Json apply(Json param) {
        return param.isBoolean() ? null : Json.make(param.toString(DefaultSchema.this.maxchars));
      }
    }

    class IsNull implements Instruction {
      @Override
      public Json apply(Json param) {
        return param.isNull() ? null : Json.make(param.toString(DefaultSchema.this.maxchars));
      }
    }

    class IsNumber implements Instruction {
      @Override
      public Json apply(Json param) {
        return param.isNumber() ? null : Json.make(param.toString(DefaultSchema.this.maxchars));
      }
    }

    class IsInteger implements Instruction {
      @Override
      public Json apply(Json param) {
        return param.isNumber() && ((Number) param.getValue()) instanceof Integer ? null : Json.make(param.toString(DefaultSchema.this.maxchars));
      }
    }

    class CheckString implements Instruction {
      int min = 0, max = Integer.MAX_VALUE;
      Pattern pattern;

      @Override
      public Json apply(Json param) {
        Json errors = null;
        if (!param.isString()) {
          return errors;
        }
        final String s = param.asString();
        final int size = s.codePointCount(0, s.length());
        if (size < this.min || size > this.max) {
          errors = maybeError(errors, Json.make("String " + param.toString(DefaultSchema.this.maxchars) + " has length outside of the permitted range [" + this.min + "," + this.max + "]."));
        }
        if (this.pattern != null && !this.pattern.matcher(s).find()) {
          errors = maybeError(errors, Json.make("String " + param.toString(DefaultSchema.this.maxchars) + " does not match regex '" + this.pattern.toString() + "'"));
        }
        return errors;
      }
    }

    class CheckNumber implements Instruction {
      double min = Double.NaN, max = Double.NaN, multipleOf = Double.NaN;
      double exclusiveMin = Double.NaN, exclusiveMax = Double.NaN;

      @Override
      public Json apply(Json param) {
        Json errors = null;
        if (!param.isNumber()) {
          return errors;
        }
        final double value = param.asDouble();
        if (!Double.isNaN(this.min) && (value < this.min)) {
          errors = maybeError(errors, Json.make("Number " + param + " is below allowed minimum " + this.min));
        }
        if (!Double.isNaN(this.exclusiveMin) && value <= this.min) {
          errors = maybeError(errors, Json.make("Number " + param + " is equal or below allowed exclusive minimum " + this.exclusiveMin));
        }
        if (!Double.isNaN(this.max) && (value > this.max)) {
          errors = maybeError(errors, Json.make("Number " + param + " is above allowed maximum " + this.max));
        }
        if (!Double.isNaN(this.exclusiveMax) && value >= this.max) {
          errors = maybeError(errors, Json.make("Number " + param + " is equal or above allowed exclusive maximum " + this.exclusiveMax));
        }
        if (!Double.isNaN(this.multipleOf) && (value / this.multipleOf) % 1 != 0) {
          errors = maybeError(errors, Json.make("Number " + param + " is not a multiple of  " + this.multipleOf));
        }
        return errors;
      }
    }

    // TODO
    // https://json-schema.org/understanding-json-schema/reference/object.html#unevaluated-properties
    static class Evaluated {
      final Map<String, Boolean> slots;

      /**
       * The error {@link Json} or <code>null</code>
       */
      Json result;

      public Evaluated(Json json) {
        this(json.isObject() ? json.asJsonMap().size() : json.asJsonList().size());
      }

      public Evaluated(int size) {
        this.slots = new HashMap<>(size);
        this.result = null;
      }

      Evaluated join(Evaluated other) {
        if (!other.isSuccess()) {
          return this;
        }
        final Evaluated result = new Evaluated(this.slots.size());
        for (final String key : this.slots.keySet()) {
          result.slots.put(key, other.isSuccess() && other.isEvaluated(key) && other.isSuccess(key) ? Boolean.TRUE : this.slots.get(key));
        }
        for (final String key : other.slots.keySet()) {
          result.slots.put(key, other.isSuccess() && other.isEvaluated(key) && other.isSuccess(key) ? Boolean.TRUE : this.slots.get(key));
        }
        return result;
      }

      void setEvaluated(int i, boolean success) {
        this.setEvaluated(String.valueOf(i), success);
      }

      void setEvaluated(String key, boolean success) {
        this.slots.put(key, success);
      }

      boolean isEvaluated(int i) {
        return isEvaluated(String.valueOf(i));
      }

      boolean isEvaluated(String key) {
        return this.slots.get(key) != null;
      }

      boolean isSuccess(int i) {
        return isSuccess(String.valueOf(i));
      }

      boolean isSuccess(String key) {
        return isEvaluated(key) && this.slots.get(key) == true;
      }

      boolean isSuccess() {
        return this.result == null;
      }

      @Override
      public String toString() {
        return "Evaluated: " + this.slots.entrySet().stream().map(new java.util.function.Function<Entry<String, Boolean>, String>() {
          @Override
          public String apply(Entry<String, Boolean> entry) {
            return entry.getKey() + ": " + String.valueOf(entry.getValue());
          }
        }).collect(Collectors.joining(","));
      }
    }

    static class ValidationContext {
      static final ThreadLocal<ValidationContext> LOCAL = new ThreadLocal<>();
      final Map<Json, Evaluated> evaluatedByJson;

      ValidationContext() {
        this.evaluatedByJson = new IdentityHashMap<>();
      }

      void addEvaluated(Json json, Evaluated evaluated) {
        Evaluated existingEvaluated = this.evaluatedByJson.get(json);
        if (existingEvaluated == null) {
          existingEvaluated = new Evaluated(json);
        }
        this.evaluatedByJson.put(json, existingEvaluated.join(evaluated));
      }

      Evaluated getOrCreateEvaluated(Json json) {
        return this.evaluatedByJson.computeIfAbsent(json, new java.util.function.Function<Json, Evaluated>() {
          @Override
          public Evaluated apply(Json key) {
            return new Evaluated(key);
          }
        });
      }

      Evaluated getEvaluated(Json json) {
        return this.evaluatedByJson.get(json);
      }

      @Override
      public String toString() {
        return this.evaluatedByJson.toString();
      }
    }

    class StartContext implements Instruction {

      @Override
      public Json apply(Json json) {
        if (ValidationContext.LOCAL.get() == null) {
          ValidationContext.LOCAL.set(new ValidationContext());
        }
        if (json.isArray()) {
          ValidationContext.LOCAL.get().addEvaluated(json, new Evaluated(json));
        }
        return null;
      }

    }

    class EndContext implements Instruction {

      Instruction unevaluatedSchema;

      @Override
      public Json apply(Json json) {
        Json errors = null;
        if (!json.isArray() && !json.isObject()) {
          return errors;
        }
        if (this.unevaluatedSchema == null) {
          return errors;
        }
        final Evaluated evaluated = ValidationContext.LOCAL.get().getOrCreateEvaluated(json);
        final Evaluated nestedEvaluated = new Evaluated(json);
        if (json.isArray()) {
          for (int i = 0; i < json.asJsonList().size(); i++) {
            if (!evaluated.isEvaluated(i) || !evaluated.isSuccess(i)) {
              errors = maybeError(errors, this.unevaluatedSchema.apply(json.at(i)));
              nestedEvaluated.setEvaluated(i, errors == null);
            }
          }
        } else {
          for (final Entry<String, Json> entry : json.asJsonMap().entrySet()) {
            final String key = entry.getKey();
            if (!evaluated.isEvaluated(key) || !evaluated.isSuccess(key)) {
              errors = maybeError(errors, this.unevaluatedSchema.apply(json.at(key)));
              System.out.println("Unevaluated: " + errors);
              nestedEvaluated.setEvaluated(key, errors == null);
            }
          }
        }
        nestedEvaluated.result = errors;
        ValidationContext.LOCAL.get().addEvaluated(json, nestedEvaluated);

//        for (int i = 0; i < json.asJsonList().size(); i++) {
//          if (i >= ValidationContext.LOCAL.get().getEvaluatedCount(json)) {
//            errors = maybeError(errors, this.unevaluatedSchema.apply(json.at(i)));
//          }
//        }
//        ValidationContext.LOCAL.get().setEvaluatedCount(json, json.asJsonList().size());

        // TODO
        System.out.println(evaluated.toString());

        return errors;
      }
    }

    class WithContext implements Instruction {
      final Instruction body;

      WithContext(Instruction body) {
        this.body = body;
      }

      @Override
      public Json apply(Json json) {
        final ValidationContext parentContext = ValidationContext.LOCAL.get();
        final ValidationContext activeContext = new ValidationContext();
        ValidationContext.LOCAL.set(activeContext);
        final Json errors = this.body.apply(json);
        final Evaluated evaluated = activeContext.getEvaluated(json);
        if (evaluated != null) {
          parentContext.addEvaluated(json, evaluated);
        }
        ValidationContext.LOCAL.set(parentContext);
        return errors;
      }

    }

    class CheckIfThenElse implements Instruction {
      Instruction ifInstruction;
      Instruction thenInstruction;
      Instruction elseInstruction;

      @Override
      public Json apply(Json json) {
        if (this.ifInstruction != null) {
          System.out.println("if");
          if (this.ifInstruction.apply(json) == null) {
            System.out.println("then");
            return this.thenInstruction != null ? this.thenInstruction.apply(json) : null;
          } else {
            System.out.println("else");
            return this.elseInstruction != null ? this.elseInstruction.apply(json) : null;
          }
        } else {
          return null;
        }
      }
    }

    class CheckArray implements Instruction {
      int min = 0;
      int max = Integer.MAX_VALUE;
      Boolean uniqueitems = null;
      Instruction prefixSchema;
      Instruction additionalSchema = any;
      Instruction schema;
      ArrayList<Instruction> prefixSchemas;
      int minContains = 1;
      int maxContains = Integer.MAX_VALUE;
      Instruction contains;
      boolean ignoreEvaluation = false;

      @Override
      public Json apply(Json param) {
        Json errors = null;
        if (!param.isArray()) {
          return errors;
        }
        final Evaluated evaluated = new Evaluated(param);
        try {
          if (this.prefixSchema != null && this.schema == null && this.prefixSchemas == null && this.additionalSchema == null) { // no schema specified
            return errors;
          }
          final int size = param.asJsonList().size();
          int containsCount = 0;
          for (int i = 0; i < size; i++) {
            final Json item = param.at(i);
            if (this.prefixSchema != null) {
              final Json error = this.prefixSchema.apply(item);
              errors = maybeError(errors, error);
              evaluated.setEvaluated(i, errors == null);
            } else if (this.prefixSchemas != null && this.prefixSchemas.size() > i) {
              final Json error = this.prefixSchemas.get(i).apply(item);
              errors = maybeError(errors, error);
              evaluated.setEvaluated(i, errors == null);
            } else if (this.schema != null) {
              final Json error = this.schema.apply(item);
              errors = maybeError(errors, error);
              evaluated.setEvaluated(i, errors == null);
            } else if (this.additionalSchema != null) {
              errors = maybeError(errors, this.additionalSchema.apply(item));
            } else {
              errors = maybeError(errors, Json.make("Additional items are not permitted: " + item + " in " + param.toString(DefaultSchema.this.maxchars)));
            }
            if (this.uniqueitems != null && this.uniqueitems && param.asJsonList().lastIndexOf(item) > i) {
              errors = maybeError(errors, Json.make("Element " + item + " is duplicate in array."));
            }
            if (this.contains != null) {
              if (this.contains.apply(item) == null) {
                containsCount++;
                evaluated.setEvaluated(i, errors == null);
              }
              if (containsCount > this.maxContains) {
                errors = maybeError(errors, Json.make("Array contains to much matches."));
              }
            }
            if (errors != null && !errors.asJsonList().isEmpty()) {
              break;
            }
          }
          if (this.contains != null && containsCount < this.minContains) {
            errors = maybeError(errors, Json.make(String.format("Array requires minimum %s matches", this.minContains)));
          }
          if (size < this.min || size > this.max) {
            errors = maybeError(errors, Json.make("Array  " + param.toString(DefaultSchema.this.maxchars) + " has number of elements outside of the permitted range [" + this.min + "," + this.max + "]."));
          }
          return errors;
        } finally {
          evaluated.result = errors;
          if (!this.ignoreEvaluation) {
            ValidationContext.LOCAL.get().addEvaluated(param, evaluated);
          }
        }
      }
    }

    class CheckPropertyPresent implements Instruction {
      String propname;

      public CheckPropertyPresent(String propname) {
        this.propname = propname;
      }

      @Override
      public Json apply(Json param) {
        if (!param.isObject()) {
          return null;
        }
        if (param.has(this.propname)) {
          return null;
        } else {
          return Json.array().add(Json.make("Required property " + this.propname + " missing from object " + param.toString(DefaultSchema.this.maxchars)));
        }
      }
    }

    class CheckObject implements Instruction {
      int min = 0, max = Integer.MAX_VALUE;
      Instruction additionalSchema = any;
      ArrayList<CheckProperty> props = new ArrayList<CheckProperty>();
      ArrayList<CheckPatternProperty> patternProps = new ArrayList<CheckPatternProperty>();
      Instruction propertyNames;

      // Object validation
      class CheckProperty implements Instruction {
        String name;
        Instruction schema;

        public CheckProperty(String name, Instruction schema) {
          this.name = name;
          this.schema = schema;
        }

        @Override
        public Json apply(Json param) {
          final Json value = param.at(this.name);
          if (value == null) {
            return null;
          } else {
            final Json errors = this.schema.apply(value);
            ValidationContext.LOCAL.get().getOrCreateEvaluated(param).setEvaluated(this.name, errors == null);
            return errors;
          }
        }
      }

      class CheckPatternProperty // implements Instruction
      {
        Pattern pattern;
        Instruction schema;

        public CheckPatternProperty(String pattern, Instruction schema) {
          this.pattern = Pattern.compile(pattern.replace("\\p{Letter}", "\\p{L}").replace("\\p{digit}", "\\p{N}"));
          this.schema = schema;
        }

        public Json apply(Json param, Set<String> found) {
          Json errors = null;
          for (final Map.Entry<String, Json> e : param.asJsonMap().entrySet()) {
            if (this.pattern.matcher(e.getKey()).find()) {
              found.add(e.getKey());
              errors = maybeError(errors, new CheckProperty(e.getKey(), this.schema).apply(param));
            }
          }
          return errors;
        }
      }

      @Override
      public Json apply(Json param) {
        Json errors = null;
        if (!param.isObject()) {
          return errors;
        }
        final HashSet<String> checked = new HashSet<String>();
        for (final CheckProperty I : this.props) {
          if (param.has(I.name)) {
            checked.add(I.name);
          }
          errors = maybeError(errors, I.apply(param));
        }
        for (final CheckPatternProperty I : this.patternProps) {
          errors = maybeError(errors, I.apply(param, checked));
        }
        if (this.additionalSchema != null) {
          for (final Map.Entry<String, Json> e : param.asJsonMap().entrySet()) {
            if (!checked.contains(e.getKey())) {
              final Json newErrors = this.additionalSchema.apply(e.getValue());
              errors = maybeError(errors, newErrors);
            }
          }
        }
        if (this.propertyNames != null) {
          for (final Map.Entry<String, Json> e : param.asJsonMap().entrySet()) {
            final String propertyName = e.getKey();
            final Json propertyNameErrors = this.propertyNames.apply(Json.make(propertyName));
            if (propertyNameErrors != null) {
              errors = maybeError(errors, Json.make(String.format("Property name '%s' is not valid", propertyName)));
            }
          }
        }
        if (param.asJsonMap().size() < this.min) {
          errors = maybeError(errors, Json.make("Object " + param.toString(DefaultSchema.this.maxchars) + " has fewer than the permitted " + this.min + "  number of properties."));
        }
        if (param.asJsonMap().size() > this.max) {
          errors = maybeError(errors, Json.make("Object " + param.toString(DefaultSchema.this.maxchars) + " has more than the permitted " + this.min + "  number of properties."));
        }
        return errors;
      }
    }

    class Sequence implements Instruction {
      ArrayList<Instruction> seq = new ArrayList<Instruction>();

      @Override
      public Json apply(Json param) {
        Json errors = null;
        for (final Instruction I : this.seq) {
          errors = maybeError(errors, I.apply(param));
        }
        return errors;
      }

      public Sequence add(Instruction I) {
        this.seq.add(I);
        return this;
      }

      public List<CheckArray> getCheckArrays() {
        final List<CheckArray> result = new ArrayList<>();
        for (final Instruction I : this.seq) {
          if (I instanceof CheckArray) {
            result.add((CheckArray) I);
          }
        }
        return result;
      }
    }

    class CheckType implements Instruction {
      Json types;

      public CheckType(Json types) {
        this.types = types;
      }

      @Override
      public Json apply(Json param) {
        final String ptype = param.isString() ? "string" : param.isObject() ? "object" : param.isArray() ? "array" : param.isNumber() ? "number" : param.isNull() ? "null" : "boolean";
        for (final Json type : this.types.asJsonList()) {
          if (type.asString().equals(ptype)) {
            return null;
          } else if (type.asString().equals("integer") && param.isNumber() && param.asDouble() % 1 == 0) {
            return null;
          }
        }
        return Json.array().add(Json.make("Type mistmatch for " + param.toString(DefaultSchema.this.maxchars) + ", allowed types: " + this.types));
      }
    }

    class CheckEnum implements Instruction {
      Json theenum;

      public CheckEnum(Json theenum) {
        this.theenum = theenum;
      }

      @Override
      public Json apply(Json param) {
        for (final Json option : this.theenum.asJsonList()) {
          if (param.equals(option)) {
            return null;
          }
        }
        return Json.array().add("Element " + param.toString(DefaultSchema.this.maxchars) + " doesn't match any of enumerated possibilities " + this.theenum);
      }
    }

    class CheckEquals implements Instruction {
      Json value;

      public CheckEquals(Json value) {
        this.value = value;
      }

      @Override
      public Json apply(Json param) {
        return param.equals(this.value) ? null : Json.array().add("Element " + param.toString(DefaultSchema.this.maxchars) + " is not equal " + param.toString(DefaultSchema.this.maxchars));
      }
    }

    class CheckAny implements Instruction {
      ArrayList<Instruction> alternates = new ArrayList<Instruction>();
      Json schema;

      @Override
      public Json apply(Json param) {
        boolean any = false;
        for (final Instruction instruction : this.alternates) {
          if (instruction.apply(param) == null) {
            any = true;
          }
        }
        if (any) {
          return null;
        }
        return Json.array().add("Element " + param.toString(DefaultSchema.this.maxchars) + " must conform to at least one of available sub-schemas " + this.schema.toString(DefaultSchema.this.maxchars));
      }
    }

    class CheckOne implements Instruction {
      ArrayList<Instruction> alternates = new ArrayList<Instruction>();
      Json schema;

      @Override
      public Json apply(Json param) {
        int matches = 0;
        final Json errors = Json.array();
        for (final Instruction I : this.alternates) {
          final Json result = I.apply(param);
          if (result == null) {
            matches++;
          } else {
            errors.add(result);
          }
        }
        if (matches != 1) {
          return Json.array().add("Element " + param.toString(DefaultSchema.this.maxchars) + " must conform to exactly one of available sub-schemas, but not more " + this.schema.toString(DefaultSchema.this.maxchars)).add(errors);
        } else {
          return null;
        }
      }
    }

    class CheckNot implements Instruction {
      Instruction I;
      Json schema;

      public CheckNot(Instruction I, Json schema) {
        this.I = I;
        this.schema = schema;
      }

      @Override
      public Json apply(Json param) {
        if (this.I.apply(param) != null) {
          return null;
        } else {
          return Json.array().add("Element " + param.toString(DefaultSchema.this.maxchars) + " must NOT conform to the schema " + this.schema.toString(DefaultSchema.this.maxchars));
        }
      }
    }

    class CheckSchemaDependency implements Instruction {
      Instruction schema;
      String property;

      public CheckSchemaDependency(String property, Instruction schema) {
        this.property = property;
        this.schema = schema;
      }

      @Override
      public Json apply(Json param) {
        if (!param.isObject()) {
          return null;
        } else if (!param.has(this.property)) {
          return null;
        } else {
          return (this.schema.apply(param));
        }
      }
    }

    class CheckPropertyDependency implements Instruction {
      Json required;
      String property;

      public CheckPropertyDependency(String property, Json required) {
        this.property = property;
        this.required = required;
      }

      @Override
      public Json apply(Json param) {
        if (!param.isObject()) {
          return null;
        }
        if (!param.has(this.property)) {
          return null;
        } else {
          Json errors = null;
          for (final Json p : this.required.asJsonList()) {
            if (!param.has(p.asString())) {
              errors = maybeError(errors, Json.make("Conditionally required property " + p + " missing from object " + param.toString(DefaultSchema.this.maxchars)));
            }
          }
          return errors;
        }
      }
    }

    Instruction compile(Json schemaJson, Map<Json, Instruction> compiled) {
      return compile(schemaJson, compiled, false);
    }

    Instruction compile(Json schemaJson, Map<Json, Instruction> compiled, boolean ignoreEvaluation) {
      schemaJson = resolveBooleanSchema(schemaJson);
      final Instruction result = compiled.get(schemaJson);
      if (result != null) {
        return result;
      }
      final Sequence seq = new Sequence();
      compiled.put(schemaJson, seq);
      seq.add(new StartContext());

      if (schemaJson.has("type") && !schemaJson.is("type", "any")) {
        seq.add(new CheckType(schemaJson.at("type").isString() ? Json.array().add(schemaJson.at("type")) : schemaJson.at("type")));
      }
      if (schemaJson.has("format")) {
        final Pattern pattern = PATTERN_BY_FORMAT.get(schemaJson.at("format").asString());
        if (pattern != null) {
          final CheckString checkString = new CheckString();
          checkString.pattern = pattern;
          seq.add(checkString);
        } else {
          if (schemaJson.is("format", "uri")) {
            seq.add(new Instruction() {

              @Override
              public Json apply(Json json) {
                if (!json.isString()) {
                  return null;
                }
                try {
                  URI.create(json.asString());
                  return null;
                } catch (final IllegalArgumentException e) {
                  return Json.array().add("Element " + json.toString(DefaultSchema.this.maxchars) + " is not a valid uri");
                }
              }
            });
          } else if (schemaJson.is("format", "idn-hostname")) {
            seq.add(new Instruction() {

              @Override
              public Json apply(Json json) {
                if (!json.isString()) {
                  return null;
                }
                try {
                  IDN.toASCII(json.asString());
                  return null;
                } catch (final IllegalArgumentException e) {
                  return Json.array().add("Element " + json.toString(DefaultSchema.this.maxchars) + " is not a valid idn hostname");
                }
              }
            });
          } else if (schemaJson.is("format", "uri-reference")) {
            seq.add(new Instruction() {

              @Override
              public Json apply(Json json) {
                if (!json.isString()) {
                  return null;
                }
                try {
                  URI.create(json.asString());
                  return null;
                } catch (final IllegalArgumentException e) {
                  return Json.array().add("Element " + json.toString(DefaultSchema.this.maxchars) + " is not a valid uri reference");
                }
              }
            });
          }
        }
      }
      if (schemaJson.has("const")) {
        seq.add(new CheckEquals(schemaJson.at("const")));
      }
      if (schemaJson.has("enum")) {
        seq.add(new CheckEnum(schemaJson.at("enum")));
      }
      if (schemaJson.has("allOf")) {
        final Sequence sub = new Sequence();
        for (final Json x : schemaJson.at("allOf").asJsonList()) {
          sub.add(new WithContext(compile(x, compiled, false)));
        }
        seq.add(sub);
      }
      if (schemaJson.has("anyOf")) {
        final CheckAny any = new CheckAny();
        any.schema = schemaJson.at("anyOf");
        for (final Json x : any.schema.asJsonList()) {
          any.alternates.add(new WithContext(compile(x, compiled, false)));
        }
        seq.add(any);
      }
      if (schemaJson.has("oneOf")) {
        final CheckOne any = new CheckOne();
        any.schema = schemaJson.at("oneOf");
        for (final Json x : any.schema.asJsonList()) {
          any.alternates.add(new WithContext(compile(x, compiled, false)));
        }
        seq.add(any);
      }
      if (schemaJson.has("not")) {
        seq.add(new CheckNot(compile(schemaJson.at("not"), compiled, true), schemaJson.at("not")));
      }

      if (schemaJson.has("required") && schemaJson.at("required").isArray()) {
        for (final Json p : schemaJson.at("required").asJsonList()) {
          seq.add(new CheckPropertyPresent(p.asString()));
        }
      }
      final CheckObject objectCheck = new CheckObject();
      if (schemaJson.has("properties")) {
        for (final Map.Entry<String, Json> p : schemaJson.at("properties").asJsonMap().entrySet()) {
          objectCheck.props.add(objectCheck.new CheckProperty(p.getKey(), compile(p.getValue(), compiled)));
        }
      }
      if (schemaJson.has("patternProperties")) {
        for (final Map.Entry<String, Json> p : schemaJson.at("patternProperties").asJsonMap().entrySet()) {
          objectCheck.patternProps.add(objectCheck.new CheckPatternProperty(p.getKey(), compile(p.getValue(), compiled)));
        }
      }
      if (schemaJson.has("additionalProperties")) {
        objectCheck.additionalSchema = compile(schemaJson.at("additionalProperties"), compiled);
      }
      final EndContext endContext = new EndContext();
      if (schemaJson.has("unevaluatedProperties")) {
        endContext.unevaluatedSchema = compile(schemaJson.at("unevaluatedProperties"), compiled);
      }
      if (schemaJson.has("minProperties")) {
        objectCheck.min = schemaJson.at("minProperties").asInteger();
      }
      if (schemaJson.has("maxProperties")) {
        objectCheck.max = schemaJson.at("maxProperties").asInteger();
      }
      if (schemaJson.has("propertyNames")) {
        objectCheck.propertyNames = compile(schemaJson.at("propertyNames"), compiled);
      }

      if (!objectCheck.props.isEmpty() || !objectCheck.patternProps.isEmpty() || objectCheck.additionalSchema != any || objectCheck.min > 0 || objectCheck.max < Integer.MAX_VALUE || objectCheck.propertyNames != null) {
        seq.add(objectCheck);
      }

      if (schemaJson.has("if")) {
        final CheckIfThenElse checkIfThenElse = new CheckIfThenElse();
        checkIfThenElse.ifInstruction = compile(schemaJson.at("if"), compiled, false);
        checkIfThenElse.thenInstruction = schemaJson.has("then") ? compile(schemaJson.at("then"), compiled) : null;
        checkIfThenElse.elseInstruction = schemaJson.has("else") ? compile(schemaJson.at("else"), compiled) : null;
        seq.add(checkIfThenElse);
      }

      final CheckArray arrayCheck = new CheckArray();
      arrayCheck.ignoreEvaluation = ignoreEvaluation;
      if (schemaJson.has("prefixItems")) {
        if (schemaJson.at("prefixItems").isObject()) {
          arrayCheck.prefixSchema = compile(schemaJson.at("prefixItems"), compiled);
        } else {
          arrayCheck.prefixSchemas = new ArrayList<Instruction>();
          for (final Json s : schemaJson.at("prefixItems").asJsonList()) {
            arrayCheck.prefixSchemas.add(compile(s, compiled));
          }
        }
      }
      if (schemaJson.has("additionalItems")) {
        arrayCheck.additionalSchema = compile(schemaJson.at("additionalItems"), compiled);
      }
      if (schemaJson.has("items")) {
        if (schemaJson.at("items").isObject() || schemaJson.is("items", true)) {
          arrayCheck.schema = compile(schemaJson.at("items"), compiled);
        } else if (!schemaJson.at("items").asBoolean()) {
          arrayCheck.additionalSchema = null;
          if (arrayCheck.schema == null && arrayCheck.prefixSchemas == null && arrayCheck.prefixSchema == null) {
            arrayCheck.schema = never;
          }
        }
      }
      if (schemaJson.has("uniqueItems")) {
        arrayCheck.uniqueitems = schemaJson.at("uniqueItems").asBoolean();
      }
      if (schemaJson.has("minItems")) {
        arrayCheck.min = schemaJson.at("minItems").asInteger();
      }
      if (schemaJson.has("maxItems")) {
        arrayCheck.max = schemaJson.at("maxItems").asInteger();
      }
      if (schemaJson.has("contains")) {
        arrayCheck.contains = compile(schemaJson.at("contains"), compiled);
      }
      if (schemaJson.has("minContains")) {
        arrayCheck.minContains = schemaJson.at("minContains").asInteger();
      }
      if (schemaJson.has("maxContains")) {
        arrayCheck.maxContains = schemaJson.at("maxContains").asInteger();
      }
      if (arrayCheck.contains != null || arrayCheck.schema != null || arrayCheck.prefixSchemas != null || arrayCheck.additionalSchema != any || arrayCheck.uniqueitems != null || arrayCheck.max < Integer.MAX_VALUE || arrayCheck.min > 0) {
        seq.add(arrayCheck);
      }
      final CheckNumber numberCheck = new CheckNumber();
      if (schemaJson.has("minimum")) {
        numberCheck.min = schemaJson.at("minimum").asDouble();
      }
      if (schemaJson.has("maximum")) {
        numberCheck.max = schemaJson.at("maximum").asDouble();
      }
      if (schemaJson.has("multipleOf")) {
        numberCheck.multipleOf = schemaJson.at("multipleOf").asDouble();
      }
      if (schemaJson.has("exclusiveMinimum")) {
        numberCheck.exclusiveMin = schemaJson.at("exclusiveMinimum").asDouble();
      }
      if (schemaJson.has("exclusiveMaximum")) {
        numberCheck.exclusiveMax = schemaJson.at("exclusiveMaximum").asDouble();
      }
      if (!Double.isNaN(numberCheck.min) || !Double.isNaN(numberCheck.max) || !Double.isNaN(numberCheck.multipleOf)) {
        seq.add(numberCheck);
      }

      final CheckString stringCheck = new CheckString();
      if (schemaJson.has("minLength")) {
        stringCheck.min = schemaJson.at("minLength").asInteger();
      }
      if (schemaJson.has("maxLength")) {
        stringCheck.max = schemaJson.at("maxLength").asInteger();
      }
      if (schemaJson.has("pattern")) {
        stringCheck.pattern = Pattern.compile(schemaJson.at("pattern").asString().replace("\\p{Letter}", "\\p{L}").replace("\\p{digit}", "\\p{N}"));
      }
      if (stringCheck.min > 0 || stringCheck.max < Integer.MAX_VALUE || stringCheck.pattern != null) {
        seq.add(stringCheck);
      }
      if (schemaJson.has("dependentSchemas")) {
        for (final Map.Entry<String, Json> e : schemaJson.at("dependentSchemas").asJsonMap().entrySet()) {
          seq.add(new CheckSchemaDependency(e.getKey(), compile(e.getValue(), compiled)));
        }
      }
      if (schemaJson.has("dependentRequired")) {
        for (final Map.Entry<String, Json> e : schemaJson.at("dependentRequired").asJsonMap().entrySet()) {
          seq.add(new CheckPropertyDependency(e.getKey(), e.getValue()));
        }
      }
      if (schemaJson.has("unevaluatedItems")) {
        endContext.unevaluatedSchema = compile(schemaJson.at("unevaluatedItems"), compiled);
      }
      seq.add(endContext);
      return seq;
    }

    int maxchars = 50;
    URI uri;
    Json theschema;
    Instruction start;

    DefaultSchema(URI uri, Json theschema, Function<URI, Json> relativeReferenceResolver) {
      try {
        this.uri = uri == null ? new URI("") : uri;
        if (relativeReferenceResolver == null) {
          relativeReferenceResolver = new Function<URI, Json>() {
            @Override
            public Json apply(URI docuri) {
              try {
                return Json.read(fetchContent(docuri.toURL()));
              } catch (final Exception ex) {
                throw new RuntimeException(ex);
              }
            }
          };
        }
        this.theschema = theschema.dup();
        this.theschema = expandReferences(null, this.theschema, this.theschema, this.uri, new CompileContext(relativeReferenceResolver));
        this.start = compile(this.theschema, new IdentityHashMap<Json, Instruction>());
      } catch (final Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public Json validate(Json document) {
      final Json result = Json.object("ok", true);
      final Json errors = this.start.apply(document);
      return errors == null ? result : result.set("errors", errors).set("ok", false);
    }

    @Override
    public Json toJson() {
      return this.theschema;
    }

    public Json generate(Json options) {
      // TODO...
      return Json.nil();
    }

  }

  public static Schema schema(Json S) {
    return new DefaultSchema(null, S, null);
  }

  public static Schema schema(URI uri) {
    return schema(uri, null);
  }

  public static Schema schema(URI uri, Function<URI, Json> relativeReferenceResolver) {
    try {
      return new DefaultSchema(uri, Json.read(Json.fetchContent(uri.toURL())), relativeReferenceResolver);
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Schema schema(Json S, URI uri) {
    return new DefaultSchema(uri, S, null);
  }

  public static class DefaultFactory implements Factory {
    @Override
    public Json nil() {
      return Json.topnull;
    }

    @Override
    public Json bool(boolean x) {
      return new BooleanJson(x ? Boolean.TRUE : Boolean.FALSE, null);
    }

    @Override
    public Json string(String x) {
      return new StringJson(x, null);
    }

    @Override
    public Json number(Number x) {
      return new NumberJson(x, null);
    }

    @Override
    public Json array() {
      return new ArrayJson();
    }

    @Override
    public Json object() {
      return new ObjectJson();
    }

    @Override
    public Json make(Object anything) {
      if (anything == null) {
        return topnull;
      } else if (anything instanceof Json) {
        return (Json) anything;
      } else if (anything instanceof String) {
        return factory().string((String) anything);
      } else if (anything instanceof Collection<?>) {
        final Json L = array();
        for (final Object x : (Collection<?>) anything) {
          L.add(factory().make(x));
        }
        return L;
      } else if (anything instanceof Map<?, ?>) {
        final Json O = object();
        for (final Map.Entry<?, ?> x : ((Map<?, ?>) anything).entrySet()) {
          O.set(x.getKey().toString(), factory().make(x.getValue()));
        }
        return O;
      } else if (anything instanceof Boolean) {
        return factory().bool((Boolean) anything);
      } else if (anything instanceof Number) {
        return factory().number((Number) anything);
      } else if (anything.getClass().isArray()) {
        final Class<?> comp = anything.getClass().getComponentType();
        if (!comp.isPrimitive()) {
          return Json.array((Object[]) anything);
        }
        final Json A = array();
        if (boolean.class == comp) {
          for (final boolean b : (boolean[]) anything) {
            A.add(b);
          }
        } else if (byte.class == comp) {
          for (final byte b : (byte[]) anything) {
            A.add(b);
          }
        } else if (char.class == comp) {
          for (final char b : (char[]) anything) {
            A.add(b);
          }
        } else if (short.class == comp) {
          for (final short b : (short[]) anything) {
            A.add(b);
          }
        } else if (int.class == comp) {
          for (final int b : (int[]) anything) {
            A.add(b);
          }
        } else if (long.class == comp) {
          for (final long b : (long[]) anything) {
            A.add(b);
          }
        } else if (float.class == comp) {
          for (final float b : (float[]) anything) {
            A.add(b);
          }
        } else if (double.class == comp) {
          for (final double b : (double[]) anything) {
            A.add(b);
          }
        }
        return A;
      } else {
        throw new IllegalArgumentException("Don't know how to convert to Json : " + anything);
      }
    }
  }

  public static final Factory defaultFactory = new DefaultFactory();

  private static Factory globalFactory = defaultFactory;

  // TODO: maybe use initialValue thread-local method to attach global factory by
  // default here...
  private static ThreadLocal<Factory> threadFactory = new ThreadLocal<Factory>();

  /**
   * <p>
   * Return the {@link Factory} currently in effect. This is the factory that the
   * {@link #make(Object)} method will dispatch on upon determining the type of
   * its argument. If you already know the type of element to construct, you can
   * avoid the type introspection implicit to the make method and call the factory
   * directly. This will result in an optimization.
   * </p>
   *
   * @return the factory
   */
  public static Factory factory() {
    final Factory f = threadFactory.get();
    return f != null ? f : globalFactory;
  }

  /**
   * <p>
   * Specify a global Json {@link Factory} to be used by all threads that don't
   * have a specific thread-local factory attached to them.
   * </p>
   *
   * @param factory The new global factory
   */
  public static void setGlobalFactory(Factory factory) {
    globalFactory = factory;
  }

  /**
   * <p>
   * Attach a thread-local Json {@link Factory} to be used specifically by this
   * thread. Thread-local Json factories are the only means to have different
   * {@link Factory} implementations used simultaneously in the same application
   * (well, more accurately, the same ClassLoader).
   * </p>
   *
   * @param factory the new thread local factory
   */
  public static void attachFactory(Factory factory) {
    threadFactory.set(factory);
  }

  /**
   * <p>
   * Clear the thread-local factory previously attached to this thread via the
   * {@link #attachFactory(Factory)} method. The global factory takes effect after
   * a call to this method.
   * </p>
   */
  public static void detachFactory() {
    threadFactory.remove();
  }

  /**
   * <p>
   * Parse a JSON entity from its string representation.
   * </p>
   *
   * @param jsonAsString A valid JSON representation as per the
   *                     <a href="http://www.json.org">json.org</a> grammar.
   *                     Cannot be <code>null</code>.
   * @return The JSON entity parsed: an object, array, string, number or boolean,
   *         or null. Note that this method will never return the actual Java
   *         <code>null</code>.
   */
  public static Json read(String jsonAsString) {
    return (Json) new Reader().read(jsonAsString);
  }

  /**
   * <p>
   * Parse a JSON entity from a <code>URL</code>.
   * </p>
   *
   * @param location A valid URL where to load a JSON document from. Cannot be
   *                 <code>null</code>.
   * @return The JSON entity parsed: an object, array, string, number or boolean,
   *         or null. Note that this method will never return the actual Java
   *         <code>null</code>.
   */
  public static Json read(URL location) {
    return (Json) new Reader().read(fetchContent(location));
  }

  /**
   * <p>
   * Parse a JSON entity from a {@link CharacterIterator}.
   * </p>
   *
   * @param it A character iterator.
   * @return the parsed JSON element
   * @see #read(String)
   */
  public static Json read(CharacterIterator it) {
    return (Json) new Reader().read(it);
  }

  /**
   * @return the <code>null Json</code> instance.
   */
  public static Json nil() {
    return factory().nil();
  }

  /**
   * @return a newly constructed, empty JSON object.
   */
  public static Json object() {
    return factory().object();
  }

  /**
   * <p>
   * Return a new JSON object initialized from the passed list of name/value
   * pairs. The number of arguments must be even. Each argument at an even
   * position is taken to be a name for the following value. The name arguments
   * are normally of type Java String, but they can be of any other type having an
   * appropriate <code>toString</code> method. Each value is first converted to a
   * <code>Json</code> instance using the {@link #make(Object)} method.
   * </p>
   *
   * @param args A sequence of name value pairs.
   * @return the new JSON object.
   */
  public static Json object(Object... args) {
    final Json j = object();
    if (args.length % 2 != 0) {
      throw new IllegalArgumentException("An even number of arguments is expected.");
    }
    for (int i = 0; i < args.length; i++) {
      j.set(args[i].toString(), factory().make(args[++i]));
    }
    return j;
  }

  /**
   * @return a new constructed, empty JSON array.
   */
  public static Json array() {
    return factory().array();
  }

  /**
   * <p>
   * Return a new JSON array filled up with the list of arguments.
   * </p>
   *
   * @param args The initial content of the array.
   * @return the new JSON array
   */
  public static Json array(Object... args) {
    final Json A = array();
    for (final Object x : args) {
      A.add(factory().make(x));
    }
    return A;
  }

  /**
   * <p>
   * Exposes some internal methods that are useful for
   * {@link org.sharegov.mjson.Json.Factory} implementations or other
   * extension/layers of the library.
   * </p>
   *
   * @author Borislav Iordanov
   *
   */
  public static class help {
    /**
     * <p>
     * Perform JSON escaping so that ", <, >, etc. characters are properly encoded
     * in the JSON string representation before returning to the client code. This
     * is useful when serializing property names or string values.
     * </p>
     */
    public static String escape(String string) {
      return escaper.escapeJsonString(string);
    }

    /**
     * <p>
     * Given a JSON Pointer, as per RFC 6901, return the nested JSON value within
     * the <code>element</code> parameter.
     * </p>
     */
    public static Json resolvePointer(String pointer, Json element) {
      return Json.resolvePointer(pointer, element);
    }
  }

  static class JsonSingleValueIterator implements Iterator<Json> {
    private boolean retrieved = false;

    @Override
    public boolean hasNext() {
      return !this.retrieved;
    }

    @Override
    public Json next() {
      this.retrieved = true;
      return null;
    }

    @Override
    public void remove() {
    }
  }

  /**
   * <p>
   * Convert an arbitrary Java instance to a {@link Json} instance.
   * </p>
   *
   * <p>
   * Maps, Collections and arrays are recursively copied where each of their
   * elements concerted into <code>Json</code> instances as well. The keys of a
   * {@link Map} parameter are normally strings, but anything with a meaningful
   * <code>toString</code> implementation will work as well.
   * </p>
   *
   * @param anything Any Java object that the current JSON factory in effect is
   *                 capable of handling.
   * @return The <code>Json</code>. This method will never return
   *         <code>null</code>. It will throw an {@link IllegalArgumentException}
   *         if it doesn't know how to convert the argument to a <code>Json</code>
   *         instance.
   * @throws IllegalArgumentException when the concrete type of the parameter is
   *                                  unknown.
   */
  public static Json make(Object anything) {
    return factory().make(anything);
  }

  // end of static utility method section

  Json enclosing = null;

  protected Json() {
  }

  protected Json(Json enclosing) {
    this.enclosing = enclosing;
  }

  /**
   * <p>
   * Return a string representation of <code>this</code> that does not exceed a
   * certain maximum length. This is useful in constructing error messages or any
   * other place where only a "preview" of the JSON element should be displayed.
   * Some JSON structures can get very large and this method will help avoid
   * string serializing the whole of them.
   * </p>
   *
   * @param maxCharacters The maximum number of characters for the string
   *                      representation.
   * @return The string representation of this object.
   */
  public String toString(int maxCharacters) {
    return toString();
  }

  /**
   * <p>
   * Explicitly set the parent of this element. The parent is presumably an array
   * or an object. Normally, there's no need to call this method as the parent is
   * automatically set by the framework. You may need to call it however, if you
   * implement your own {@link Factory} with your own implementations of the Json
   * types.
   * </p>
   *
   * @param enclosing The parent element.
   */
  public void attachTo(Json enclosing) {
    this.enclosing = enclosing;
  }

  /**
   * @return the <code>Json</code> entity, if any, enclosing this
   *         <code>Json</code>. The returned value can be <code>null</code> or a
   *         <code>Json</code> object or list, but not one of the primitive types.
   * @deprecated This method is both problematic and rarely if every used and it
   *             will be removed in 2.0.
   */
  @Deprecated
  public final Json up() {
    return this.enclosing;
  }

  /**
   * @return a clone (a duplicate) of this <code>Json</code> entity. Note that
   *         cloning is deep if array and objects. Primitives are also cloned,
   *         even though their values are immutable because the new enclosing
   *         entity (the result of the {@link #up()} method) may be different.
   *         since they are immutable.
   */
  public Json dup() {
    return this;
  }

  /**
   * <p>
   * Return the <code>Json</code> element at the specified index of this
   * <code>Json</code> array. This method applies only to Json arrays.
   * </p>
   *
   * @param index The index of the desired element.
   * @return The JSON element at the specified index in this array.
   */
  public Json at(int index) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Return the specified property of a <code>Json</code> object or
   * <code>null</code> if there's no such property. This method applies only to
   * Json objects.
   * </p>
   *
   * @param The property name.
   * @return The JSON element that is the value of that property.
   */
  public Json at(String property) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Return the specified property of a <code>Json</code> object if it exists. If
   * it doesn't, then create a new property with value the <code>def</code>
   * parameter and return that parameter.
   * </p>
   *
   * @param property The property to return.
   * @param def      The default value to set and return in case the property
   *                 doesn't exist.
   */
  public final Json at(String property, Json def) {
    final Json x = at(property);
    if (x == null) {
//			set(property, def);
      return def;
    } else {
      return x;
    }
  }

  /**
   * <p>
   * Return the specified property of a <code>Json</code> object if it exists. If
   * it doesn't, then create a new property with value the <code>def</code>
   * parameter and return that parameter.
   * </p>
   *
   * @param property The property to return.
   * @param def      The default value to set and return in case the property
   *                 doesn't exist.
   */
  public final Json at(String property, Object def) {
    return at(property, make(def));
  }

  /**
   * <p>
   * Return true if this <code>Json</code> object has the specified property and
   * false otherwise.
   * </p>
   *
   * @param property The name of the property.
   */
  public boolean has(String property) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Return <code>true</code> if and only if this <code>Json</code> object has a
   * property with the specified value. In particular, if the object has no such
   * property <code>false</code> is returned.
   * </p>
   *
   * @param property The property name.
   * @param value    The value to compare with. Comparison is done via the equals
   *                 method. If the value is not an instance of <code>Json</code>,
   *                 it is first converted to such an instance.
   * @return
   */
  public boolean is(String property, Object value) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Return <code>true</code> if and only if this <code>Json</code> array has an
   * element with the specified value at the specified index. In particular, if
   * the array has no element at this index, <code>false</code> is returned.
   * </p>
   *
   * @param index The 0-based index of the element in a JSON array.
   * @param value The value to compare with. Comparison is done via the equals
   *              method. If the value is not an instance of <code>Json</code>, it
   *              is first converted to such an instance.
   * @return
   */
  public boolean is(int index, Object value) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Add the specified <code>Json</code> element to this array.
   * </p>
   *
   * @return this
   */
  public Json add(Json el) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Add an arbitrary Java object to this <code>Json</code> array. The object is
   * first converted to a <code>Json</code> instance by calling the static
   * {@link #make} method.
   * </p>
   *
   * @param anything Any Java object that can be converted to a Json instance.
   * @return this
   */
  public final Json add(Object anything) {
    return add(make(anything));
  }

  /**
   * <p>
   * Remove the specified property from a <code>Json</code> object and return that
   * property.
   * </p>
   *
   * @param property The property to be removed.
   * @return The property value or <code>null</code> if the object didn't have
   *         such a property to begin with.
   */
  public Json atDel(String property) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Remove the element at the specified index from a <code>Json</code> array and
   * return that element.
   * </p>
   *
   * @param index The index of the element to delete.
   * @return The element value.
   */
  public Json atDel(int index) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Delete the specified property from a <code>Json</code> object.
   * </p>
   *
   * @param property The property to be removed.
   * @return this
   */
  public Json delAt(String property) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Remove the element at the specified index from a <code>Json</code> array.
   * </p>
   *
   * @param index The index of the element to delete.
   * @return this
   */
  public Json delAt(int index) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Remove the specified element from a <code>Json</code> array.
   * </p>
   *
   * @param el The element to delete.
   * @return this
   */
  public Json remove(Json el) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Remove the specified Java object (converted to a Json instance) from a
   * <code>Json</code> array. This is equivalent to
   * <code>remove({@link #make(Object)})</code>.
   * </p>
   *
   * @param anything The object to delete.
   * @return this
   */
  public final Json remove(Object anything) {
    return remove(make(anything));
  }

  /**
   * <p>
   * Set a <code>Json</code> objects's property.
   * </p>
   *
   * @param property The property name.
   * @param value    The value of the property.
   * @return this
   */
  public Json set(String property, Json value) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Set a <code>Json</code> objects's property.
   * </p>
   *
   * @param property The property name.
   * @param value    The value of the property, converted to a <code>Json</code>
   *                 representation with {@link #make}.
   * @return this
   */
  public final Json set(String property, Object value) {
    return set(property, make(value));
  }

  /**
   * <p>
   * Change the value of a JSON array element. This must be an array.
   * </p>
   *
   * @param index 0-based index of the element in the array.
   * @param value the new value of the element
   * @return this
   */
  public Json set(int index, Object value) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * <p>
   * Combine this object or array with the passed in object or array. The types of
   * <code>this</code> and the <code>object</code> argument must match. If both
   * are <code>Json</code> objects, all properties of the parameter are added to
   * <code>this</code>. If both are arrays, all elements of the parameter are
   * appended to <code>this</code>
   * </p>
   *
   * @param object  The object or array whose properties or elements must be added
   *                to this Json object or array.
   * @param options A sequence of options that governs the merging process.
   * @return this
   */
  public Json with(Json object, Json[] options) {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * Same as <code>{}@link #with(Json,Json...options)}</code> with each option
   * argument converted to <code>Json</code> first.
   */
  public Json with(Json object, Object... options) {
    final Json[] jopts = new Json[options.length];
    for (int i = 0; i < jopts.length; i++) {
      jopts[i] = make(options[i]);
    }
    return with(object, jopts);
  }

  /**
   * @return the underlying value of this <code>Json</code> entity. The actual
   *         value will be a Java Boolean, String, Number, Map, List or null. For
   *         complex entities (objects or arrays), the method will perform a deep
   *         copy and extra underlying values recursively for all nested elements.
   */
  public Object getValue() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the boolean value of a boolean <code>Json</code> instance. Call
   *         {@link #isBoolean()} first if you're not sure this instance is indeed
   *         a boolean.
   */
  public boolean asBoolean() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the string value of a string <code>Json</code> instance. Call
   *         {@link #isString()} first if you're not sure this instance is indeed
   *         a string.
   */
  public String asString() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the integer value of a number <code>Json</code> instance. Call
   *         {@link #isNumber()} first if you're not sure this instance is indeed
   *         a number.
   */
  public int asInteger() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the float value of a float <code>Json</code> instance. Call
   *         {@link #isNumber()} first if you're not sure this instance is indeed
   *         a number.
   */
  public float asFloat() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the double value of a number <code>Json</code> instance. Call
   *         {@link #isNumber()} first if you're not sure this instance is indeed
   *         a number.
   */
  public double asDouble() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the long value of a number <code>Json</code> instance. Call
   *         {@link #isNumber()} first if you're not sure this instance is indeed
   *         a number.
   */
  public long asLong() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the short value of a number <code>Json</code> instance. Call
   *         {@link #isNumber()} first if you're not sure this instance is indeed
   *         a number.
   */
  public short asShort() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the byte value of a number <code>Json</code> instance. Call
   *         {@link #isNumber()} first if you're not sure this instance is indeed
   *         a number.
   */
  public byte asByte() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the first character of a string <code>Json</code> instance. Call
   *         {@link #isString()} first if you're not sure this instance is indeed
   *         a string.
   */
  public char asChar() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return a map of the properties of an object <code>Json</code> instance. The
   *         map is a clone of the object and can be modified safely without
   *         affecting it. Call {@link #isObject()} first if you're not sure this
   *         instance is indeed a <code>Json</code> object.
   */
  public Map<String, Object> asMap() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the underlying map of properties of a <code>Json</code> object. The
   *         returned map is the actual object representation so any modifications
   *         to it are modifications of the <code>Json</code> object itself. Call
   *         {@link #isObject()} first if you're not sure this instance is indeed
   *         a <code>Json</code> object.
   */
  public Map<String, Json> asJsonMap() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return a list of the elements of a <code>Json</code> array. The list is a
   *         clone of the array and can be modified safely without affecting it.
   *         Call {@link #isArray()} first if you're not sure this instance is
   *         indeed a <code>Json</code> array.
   */
  public List<Object> asList() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return the underlying {@link List} representation of a <code>Json</code>
   *         array. The returned list is the actual array representation so any
   *         modifications to it are modifications of the <code>Json</code> array
   *         itself. Call {@link #isArray()} first if you're not sure this
   *         instance is indeed a <code>Json</code> array.
   */
  public List<Json> asJsonList() {
    throw new UnsupportedOperationException(this.toString(MAX_CHARACTERS));
  }

  /**
   * @return <code>true</code> if this is a <code>Json</code> null entity and
   *         <code>false</code> otherwise.
   */
  public boolean isNull() {
    return false;
  }

  /**
   * @return <code>true</code> if this is a <code>Json</code> string entity and
   *         <code>false</code> otherwise.
   */
  public boolean isString() {
    return false;
  }

  /**
   * @return <code>true</code> if this is a <code>Json</code> number entity and
   *         <code>false</code> otherwise.
   */
  public boolean isNumber() {
    return false;
  }

  /**
   * @return <code>true</code> if this is a <code>Json</code> boolean entity and
   *         <code>false</code> otherwise.
   */
  public boolean isBoolean() {
    return false;
  }

  /**
   * @return <code>true</code> if this is a <code>Json</code> array (i.e. list)
   *         entity and <code>false</code> otherwise.
   */
  public boolean isArray() {
    return false;
  }

  /**
   * @return <code>true</code> if this is a <code>Json</code> object entity and
   *         <code>false</code> otherwise.
   */
  public boolean isObject() {
    return false;
  }

  /**
   * @return <code>true</code> if this is a <code>Json</code> primitive entity
   *         (one of string, number or boolean) and <code>false</code> otherwise.
   *
   */
  public boolean isPrimitive() {
    return isString() || isNumber() || isBoolean();
  }

  /**
   * <p>
   * Json-pad this object as an argument to a callback function.
   * </p>
   *
   * @param callback The name of the callback function. Can be null or empty, in
   *                 which case no padding is done.
   * @return The jsonpadded, stringified version of this object if the
   *         <code>callback</code> is not null or empty, or just the stringified
   *         version of the object.
   */
  public String pad(String callback) {
    return (callback != null && callback.length() > 0) ? callback + "(" + toString() + ");" : toString();
  }

  // -------------------------------------------------------------------------
  // END OF PUBLIC INTERFACE
  // -------------------------------------------------------------------------

  /**
   * Return an object representing the complete configuration of a merge. The
   * properties of the object represent paths of the JSON structure being merged
   * and the values represent the set of options that apply to each path.
   *
   * @param options the configuration options
   * @return the configuration object
   */
  protected Json collectWithOptions(Json... options) {
    final Json result = object();
    for (final Json opt : options) {
      if (opt.isString()) {
        if (!result.has("")) {
          result.set("", object());
        }
        result.at("").set(opt.asString(), true);
      } else {
        if (!opt.has("for")) {
          opt.set("for", array(""));
        }
        Json forPaths = opt.at("for");
        if (!forPaths.isArray()) {
          forPaths = array(forPaths);
        }
        for (final Json path : forPaths.asJsonList()) {
          if (!result.has(path.asString())) {
            result.set(path.asString(), object());
          }
          final Json at_path = result.at(path.asString());
          at_path.set("merge", opt.is("merge", true));
          at_path.set("dup", opt.is("dup", true));
          at_path.set("sort", opt.is("sort", true));
          at_path.set("compareBy", opt.at("compareBy", nil()));
        }
      }
    }
    return result;
  }

  static class NullJson extends Json {
    private static final long serialVersionUID = 1L;

    NullJson() {
    }

    NullJson(Json e) {
      super(e);
    }

    @Override
    public Object getValue() {
      return null;
    }

    @Override
    public Json dup() {
      return new NullJson();
    }

    @Override
    public boolean isNull() {
      return true;
    }

    @Override
    public void write(Writer writer) throws IOException {
      writer.write("null");
    }

    @Override
    public String toString() {
      return "null";
    }

    @Override
    public List<Object> asList() {
      return Collections.singletonList(null);
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(Object x) {
      return x instanceof NullJson;
    }

    @Override
    public Iterator<Json> iterator() {
      return new JsonSingleValueIterator() {
        @Override
        public Json next() {
          super.next();
          return NullJson.this;
        }
      };
    }

  }

  static NullJson topnull = new NullJson();

  /**
   * <p>
   * Set the parent (i.e. enclosing element) of Json element.
   * </p>
   *
   * @param el
   * @param parent
   */
  static void setParent(Json el, Json parent) {
    if (el.enclosing == null) {
      el.enclosing = parent;
    } else if (el.enclosing instanceof ParentArrayJson) {
      ((ParentArrayJson) el.enclosing).L.add(parent);
    } else {
      final ParentArrayJson A = new ParentArrayJson();
      A.L.add(el.enclosing);
      A.L.add(parent);
      el.enclosing = A;
    }
  }

  /**
   * <p>
   * Remove/unset the parent (i.e. enclosing element) of Json element.
   * </p>
   *
   * @param el
   * @param parent
   */
  static void removeParent(Json el, Json parent) {
    if (el.enclosing == parent) {
      el.enclosing = null;
    } else if (el.enclosing.isArray()) {
      final ArrayJson A = (ArrayJson) el.enclosing;
      int idx = 0;
      while (A.L.get(idx) != parent && idx < A.L.size()) {
        idx++;
      }
      if (idx < A.L.size()) {
        A.L.remove(idx);
      }
    }
  }

  static class BooleanJson extends Json {
    private static final long serialVersionUID = 1L;

    boolean val;

    BooleanJson() {
    }

    BooleanJson(Json e) {
      super(e);
    }

    BooleanJson(Boolean val, Json e) {
      super(e);
      this.val = val;
    }

    @Override
    public Object getValue() {
      return this.val;
    }

    @Override
    public Json dup() {
      return new BooleanJson(this.val, null);
    }

    @Override
    public boolean asBoolean() {
      return this.val;
    }

    @Override
    public boolean isBoolean() {
      return true;
    }

    @Override
    public void write(Writer writer) throws IOException {
      writer.write(this.val ? "true" : "false");
    }

    @Override
    public String toString() {
      return this.val ? "true" : "false";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> asList() {
      return (List<Object>) (List<?>) Collections.singletonList(this.val);
    }

    @Override
    public int hashCode() {
      return this.val ? 1 : 0;
    }

    @Override
    public boolean equals(Object x) {
      return x instanceof BooleanJson && ((BooleanJson) x).val == this.val;
    }

    @Override
    public Iterator<Json> iterator() {
      return new JsonSingleValueIterator() {
        @Override
        public Json next() {
          super.next();
          return BooleanJson.this;
        }
      };
    }

  }

  static class StringJson extends Json {
    private static final long serialVersionUID = 1L;

    String val;

    StringJson() {
    }

    StringJson(Json e) {
      super(e);
    }

    StringJson(String val, Json e) {
      super(e);
      this.val = val;
    }

    @Override
    public Json dup() {
      return new StringJson(this.val, null);
    }

    @Override
    public boolean isString() {
      return true;
    }

    @Override
    public Object getValue() {
      return this.val;
    }

    @Override
    public String asString() {
      return this.val;
    }

    @Override
    public int asInteger() {
      return Integer.parseInt(this.val);
    }

    @Override
    public float asFloat() {
      return Float.parseFloat(this.val);
    }

    @Override
    public double asDouble() {
      return Double.parseDouble(this.val);
    }

    @Override
    public long asLong() {
      return Long.parseLong(this.val);
    }

    @Override
    public short asShort() {
      return Short.parseShort(this.val);
    }

    @Override
    public byte asByte() {
      return Byte.parseByte(this.val);
    }

    @Override
    public char asChar() {
      return this.val.charAt(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> asList() {
      return (List<Object>) (List<?>) Collections.singletonList(this.val);
    }

    @Override
    public void write(Writer writer) throws IOException {
      writer.write('"' + escaper.escapeJsonString(this.val) + '"');
    }

    @Override
    public String toString() {
      return '"' + escaper.escapeJsonString(this.val) + '"';
    }

    @Override
    public String toString(int maxCharacters) {
      if (this.val.length() <= maxCharacters) {
        return toString();
      } else {
        return '"' + escaper.escapeJsonString(this.val.subSequence(0, maxCharacters)) + "...\"";
      }
    }

    @Override
    public int hashCode() {
      return this.val.hashCode();
    }

    @Override
    public boolean equals(Object x) {
      return x instanceof StringJson && ((StringJson) x).val.equals(this.val);
    }

    @Override
    public Iterator<Json> iterator() {
      return new JsonSingleValueIterator() {
        @Override
        public Json next() {
          super.next();
          return StringJson.this;
        }
      };
    }

  }

  static class NumberJson extends Json {
    private static final long serialVersionUID = 1L;

    Number val;

    NumberJson() {
    }

    NumberJson(Json e) {
      super(e);
    }

    NumberJson(Number val, Json e) {
      super(e);
      this.val = val;
    }

    @Override
    public Json dup() {
      return new NumberJson(this.val, null);
    }

    @Override
    public boolean isNumber() {
      return true;
    }

    @Override
    public Object getValue() {
      return this.val;
    }

    @Override
    public String asString() {
      return this.val.toString();
    }

    @Override
    public int asInteger() {
      return this.val.intValue();
    }

    @Override
    public float asFloat() {
      return this.val.floatValue();
    }

    @Override
    public double asDouble() {
      return this.val.doubleValue();
    }

    @Override
    public long asLong() {
      return this.val.longValue();
    }

    @Override
    public short asShort() {
      return this.val.shortValue();
    }

    @Override
    public byte asByte() {
      return this.val.byteValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> asList() {
      return (List<Object>) (List<?>) Collections.singletonList(this.val);
    }

    @Override
    public void write(Writer writer) throws IOException {
      writer.write(this.val.toString());
    }

    @Override
    public String toString() {
      return this.val.toString();
    }

    @Override
    public int hashCode() {
      return this.val.hashCode();
    }

    @Override
    public boolean equals(Object x) {
      return x instanceof NumberJson && this.val.doubleValue() == ((NumberJson) x).val.doubleValue();
    }

    @Override
    public Iterator<Json> iterator() {
      return new JsonSingleValueIterator() {
        @Override
        public Json next() {
          super.next();
          return NumberJson.this;
        }
      };
    }

  }

  static class ArrayJson extends Json {
    private static final long serialVersionUID = 1L;

    List<Json> L = new ArrayList<Json>();

    ArrayJson() {
    }

    ArrayJson(Json e) {
      super(e);
    }

    @Override
    public Iterator<Json> iterator() {
      return this.L.iterator();
    }

    @Override
    public Json dup() {
      final ArrayJson j = new ArrayJson();
      for (final Json e : this.L) {
        final Json v = e.dup();
        v.enclosing = j;
        j.L.add(v);
      }
      return j;
    }

    @Override
    public Json set(int index, Object value) {
      final Json jvalue = make(value);
      this.L.set(index, jvalue);
      setParent(jvalue, this);
      return this;
    }

    @Override
    public List<Json> asJsonList() {
      return this.L;
    }

    @Override
    public List<Object> asList() {
      final ArrayList<Object> A = new ArrayList<Object>();
      for (final Json x : this.L) {
        A.add(x.getValue());
      }
      return A;
    }

    @Override
    public boolean is(int index, Object value) {
      if (index < 0 || index >= this.L.size()) {
        return false;
      } else {
        return this.L.get(index).equals(make(value));
      }
    }

    @Override
    public Object getValue() {
      return asList();
    }

    @Override
    public boolean isArray() {
      return true;
    }

    @Override
    public Json at(int index) {
      return this.L.get(index);
    }

    @Override
    public Json add(Json el) {
      this.L.add(el);
      setParent(el, this);
      return this;
    }

    @Override
    public Json remove(Json el) {
      this.L.remove(el);
      el.enclosing = null;
      return this;
    }

    boolean isEqualJson(Json left, Json right) {
      if (left == null) {
        return right == null;
      } else {
        return left.equals(right);
      }
    }

    boolean isEqualJson(Json left, Json right, Json fields) {
      if (fields.isNull()) {
        return left.equals(right);
      } else if (fields.isString()) {
        return isEqualJson(resolvePointer(fields.asString(), left), resolvePointer(fields.asString(), right));
      } else if (fields.isArray()) {
        for (final Json field : fields.asJsonList()) {
          if (!isEqualJson(resolvePointer(field.asString(), left), resolvePointer(field.asString(), right))) {
            return false;
          }
        }
        return true;
      } else {
        throw new IllegalArgumentException("Compare by options should be either a property name or an array of property names: " + fields);
      }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    int compareJson(Json left, Json right, Json fields) {
      if (fields.isNull()) {
        return ((Comparable) left.getValue()).compareTo(right.getValue());
      } else if (fields.isString()) {
        final Json leftProperty = resolvePointer(fields.asString(), left);
        final Json rightProperty = resolvePointer(fields.asString(), right);
        return ((Comparable) leftProperty).compareTo(rightProperty);
      } else if (fields.isArray()) {
        for (final Json field : fields.asJsonList()) {
          final Json leftProperty = resolvePointer(field.asString(), left);
          final Json rightProperty = resolvePointer(field.asString(), right);
          final int result = ((Comparable) leftProperty).compareTo(rightProperty);
          if (result != 0) {
            return result;
          }
        }
        return 0;
      } else {
        throw new IllegalArgumentException("Compare by options should be either a property name or an array of property names: " + fields);
      }
    }

    Json withOptions(Json array, Json allOptions, String path) {
      final Json opts = allOptions.at(path, object());
      final boolean dup = opts.is("dup", true);
      final Json compareBy = opts.at("compareBy", nil());
      if (opts.is("sort", true)) {
        int thisIndex = 0, thatIndex = 0;
        while (thatIndex < array.asJsonList().size()) {
          final Json thatElement = array.at(thatIndex);
          if (thisIndex == this.L.size()) {
            this.L.add(dup ? thatElement.dup() : thatElement);
            thisIndex++;
            thatIndex++;
            continue;
          }
          final int compared = compareJson(at(thisIndex), thatElement, compareBy);
          if (compared < 0) { // this < that
            thisIndex++;
          } else if (compared > 0) // this > that
          {
            this.L.add(thisIndex, dup ? thatElement.dup() : thatElement);
            thatIndex++;
          } else { // equal, ignore
            thatIndex++;
          }
        }
      } else {
        for (final Json thatElement : array.asJsonList()) {
          boolean present = false;
          for (final Json thisElement : this.L) {
            if (isEqualJson(thisElement, thatElement, compareBy)) {
              present = true;
              break;
            }
          }
          if (!present) {
            this.L.add(dup ? thatElement.dup() : thatElement);
          }
        }
      }
      return this;
    }

    @Override
    public Json with(Json object, Json... options) {
      if (object == null) {
        return this;
      }
      if (!object.isArray()) {
        add(object);
      } else if (options.length > 0) {
        final Json O = collectWithOptions(options);
        return withOptions(object, O, "");
      } else {
        // what about "enclosing" here? we don't have a provision where a Json
        // element belongs to more than one enclosing elements...
        this.L.addAll(((ArrayJson) object).L);
      }
      return this;
    }

    @Override
    public Json atDel(int index) {
      final Json el = this.L.remove(index);
      if (el != null) {
        el.enclosing = null;
      }
      return el;
    }

    @Override
    public Json delAt(int index) {
      final Json el = this.L.remove(index);
      if (el != null) {
        el.enclosing = null;
      }
      return this;
    }

    @Override
    public void write(Writer writer) throws IOException {
      writer.write("[");
      for (final Iterator<Json> i = this.L.iterator(); i.hasNext();) {
        final Json value = i.next();
        value.write(writer);
        if (i.hasNext()) {
          writer.write(",");
        }
      }
      writer.write("]");
    }

    @Override
    public String toString() {
      return toString(Integer.MAX_VALUE);
    }

    @Override
    public String toString(int maxCharacters) {
      return toStringImpl(maxCharacters, new IdentityHashMap<Json, Json>());
    }

    String toStringImpl(int maxCharacters, Map<Json, Json> done) {
      final StringBuilder sb = new StringBuilder("[");
      for (final Iterator<Json> i = this.L.iterator(); i.hasNext();) {
        final Json value = i.next();
        String s = value.isObject() ? ((ObjectJson) value).toStringImpl(maxCharacters, done) : value.isArray() ? ((ArrayJson) value).toStringImpl(maxCharacters, done) : value.toString(maxCharacters);
        if (sb.length() + s.length() > maxCharacters) {
          s = s.substring(0, Math.max(0, maxCharacters - sb.length()));
        } else {
          sb.append(s);
        }
        if (i.hasNext()) {
          sb.append(",");
        }
        if (sb.length() >= maxCharacters) {
          sb.append("...");
          break;
        }
      }
      sb.append("]");
      return sb.toString();
    }

    @Override
    public int hashCode() {
      return this.L.hashCode();
    }

    @Override
    public boolean equals(Object x) {
      return x instanceof ArrayJson && ((ArrayJson) x).L.equals(this.L);
    }
  }

  static class ParentArrayJson extends ArrayJson {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

  }

  static class ObjectJson extends Json {
    private static final long serialVersionUID = 1L;

    Map<String, Json> object = new HashMap<String, Json>();

    @Override
    public Iterator<Json> iterator() {
      return this.object.values().iterator();
    }

    ObjectJson() {
    }

    ObjectJson(Json e) {
      super(e);
    }

    @Override
    public Json dup() {
      final ObjectJson j = new ObjectJson();
      for (final Map.Entry<String, Json> e : this.object.entrySet()) {
        final Json v = e.getValue().dup();
        v.enclosing = j;
        j.object.put(e.getKey(), v);
      }
      return j;
    }

    @Override
    public boolean has(String property) {
      return this.object.containsKey(property);
    }

    @Override
    public boolean is(String property, Object value) {
      final Json p = this.object.get(property);
      if (p == null) {
        return false;
      } else {
        return p.equals(make(value));
      }
    }

    @Override
    public Json at(String property) {
      return this.object.get(property);
    }

    protected Json withOptions(Json other, Json allOptions, String path) {
      if (!allOptions.has(path)) {
        allOptions.set(path, object());
      }
      final Json options = allOptions.at(path, object());
      final boolean duplicate = options.is("dup", true);
      if (options.is("merge", true)) {
        for (final Map.Entry<String, Json> e : other.asJsonMap().entrySet()) {
          final Json local = this.object.get(e.getKey());
          if (local instanceof ObjectJson) {
            ((ObjectJson) local).withOptions(e.getValue(), allOptions, path + "/" + e.getKey());
          } else if (local instanceof ArrayJson) {
            ((ArrayJson) local).withOptions(e.getValue(), allOptions, path + "/" + e.getKey());
          } else {
            set(e.getKey(), duplicate ? e.getValue().dup() : e.getValue());
          }
        }
      } else if (duplicate) {
        for (final Map.Entry<String, Json> e : other.asJsonMap().entrySet()) {
          set(e.getKey(), e.getValue().dup());
        }
      } else {
        for (final Map.Entry<String, Json> e : other.asJsonMap().entrySet()) {
          set(e.getKey(), e.getValue());
        }
      }
      return this;
    }

    @Override
    public Json with(Json x, Json... options) {
      if (x == null) {
        return this;
      }
      if (!x.isObject()) {
        throw new UnsupportedOperationException();
      }
      if (options.length > 0) {
        final Json O = collectWithOptions(options);
        return withOptions(x, O, "");
      } else {
        for (final Map.Entry<String, Json> e : x.asJsonMap().entrySet()) {
          set(e.getKey(), e.getValue());
        }
      }
      return this;
    }

    @Override
    public Json set(String property, Json el) {
      if (property == null) {
        throw new IllegalArgumentException("Null property names are not allowed, value is " + el);
      }
      if (el == null) {
        el = nil();
      }
      setParent(el, this);
      this.object.put(property, el);
      return this;
    }

    @Override
    public Json atDel(String property) {
      final Json el = this.object.remove(property);
      removeParent(el, this);
      return el;
    }

    @Override
    public Json delAt(String property) {
      final Json el = this.object.remove(property);
      removeParent(el, this);
      return this;
    }

    @Override
    public Object getValue() {
      return asMap();
    }

    @Override
    public boolean isObject() {
      return true;
    }

    @Override
    public Map<String, Object> asMap() {
      final HashMap<String, Object> m = new HashMap<String, Object>();
      for (final Map.Entry<String, Json> e : this.object.entrySet()) {
        m.put(e.getKey(), e.getValue().getValue());
      }
      return m;
    }

    @Override
    public Map<String, Json> asJsonMap() {
      return this.object;
    }

    @Override
    public void write(Writer writer) throws IOException {
      writer.write("{");
      for (final Iterator<Map.Entry<String, Json>> i = this.object.entrySet().iterator(); i.hasNext();) {
        final Map.Entry<String, Json> entry = i.next();
        final String key = entry.getKey();
        final Json value = entry.getValue();
        writer.append('"');
        writer.append(escaper.escapeJsonString(key));
        writer.write('"');
        writer.write(":");
        value.write(writer);
        if (i.hasNext()) {
          writer.write(",");
        }
      }
      writer.write("}");
    }

    @Override
    public String toString() {
      return toString(Integer.MAX_VALUE);
    }

    @Override
    public String toString(int maxCharacters) {
      return toStringImpl(maxCharacters, new IdentityHashMap<Json, Json>());
    }

    String toStringImpl(int maxCharacters, Map<Json, Json> done) {
      final StringBuilder sb = new StringBuilder("{");
      if (done.containsKey(this)) {
        return sb.append("...}").toString();
      }
      done.put(this, this);
      for (final Iterator<Map.Entry<String, Json>> i = this.object.entrySet().iterator(); i.hasNext();) {
        final Map.Entry<String, Json> x = i.next();
        sb.append('"');
        sb.append(escaper.escapeJsonString(x.getKey()));
        sb.append('"');
        sb.append(":");
        String s = x.getValue().isObject() ? ((ObjectJson) x.getValue()).toStringImpl(maxCharacters, done) : x.getValue().isArray() ? ((ArrayJson) x.getValue()).toStringImpl(maxCharacters, done) : x.getValue().toString(maxCharacters);
        if (sb.length() + s.length() > maxCharacters) {
          s = s.substring(0, Math.max(0, maxCharacters - sb.length()));
        }
        sb.append(s);
        if (i.hasNext()) {
          sb.append(",");
        }
        if (sb.length() >= maxCharacters) {
          sb.append("...");
          break;
        }
      }
      sb.append("}");
      return sb.toString();
    }

    @Override
    public int hashCode() {
      return this.object.hashCode();
    }

    @Override
    public boolean equals(Object x) {
      return x instanceof ObjectJson && ((ObjectJson) x).object.equals(this.object);
    }
  }

  // ------------------------------------------------------------------------
  // Extra utilities, taken from around the internet:
  // ------------------------------------------------------------------------

  /*
   * Copyright (C) 2008 Google Inc.
   *
   * Licensed under the Apache License, Version 2.0 (the "License"); you may not
   * use this file except in compliance with the License. You may obtain a copy of
   * the License at
   *
   * http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   * License for the specific language governing permissions and limitations under
   * the License.
   */

  /**
   * A utility class that is used to perform JSON escaping so that ", <, >, etc.
   * characters are properly encoded in the JSON string representation before
   * returning to the client code.
   *
   * <p>
   * This class contains a single method to escape a passed in string value:
   *
   * <pre>
   * String jsonStringValue = "beforeQuote\"afterQuote";
   * String escapedValue = Escaper.escapeJsonString(jsonStringValue);
   * </pre>
   * </p>
   *
   * @author Inderjeet Singh
   * @author Joel Leitch
   */
  static Escaper escaper = new Escaper(false);

  final static class Escaper {

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final Set<Character> JS_ESCAPE_CHARS;
    private static final Set<Character> HTML_ESCAPE_CHARS;

    static {
      final Set<Character> mandatoryEscapeSet = new HashSet<Character>();
      mandatoryEscapeSet.add('"');
      mandatoryEscapeSet.add('\\');
      JS_ESCAPE_CHARS = Collections.unmodifiableSet(mandatoryEscapeSet);

      final Set<Character> htmlEscapeSet = new HashSet<Character>();
      htmlEscapeSet.add('<');
      htmlEscapeSet.add('>');
      htmlEscapeSet.add('&');
      htmlEscapeSet.add('=');
      htmlEscapeSet.add('\'');
//	    htmlEscapeSet.add('/');  -- Removing slash for now since it causes some incompatibilities
      HTML_ESCAPE_CHARS = Collections.unmodifiableSet(htmlEscapeSet);
    }

    private final boolean escapeHtmlCharacters;

    Escaper(boolean escapeHtmlCharacters) {
      this.escapeHtmlCharacters = escapeHtmlCharacters;
    }

    public String escapeJsonString(CharSequence plainText) {
      final StringBuilder escapedString = new StringBuilder(plainText.length() + 20);
      try {
        escapeJsonString(plainText, escapedString);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
      return escapedString.toString();
    }

    private void escapeJsonString(CharSequence plainText, StringBuilder out) throws IOException {
      int pos = 0; // Index just past the last char in plainText written to out.
      final int len = plainText.length();

      for (int charCount, i = 0; i < len; i += charCount) {
        final int codePoint = Character.codePointAt(plainText, i);
        charCount = Character.charCount(codePoint);

        if (!isControlCharacter(codePoint) && !mustEscapeCharInJsString(codePoint)) {
          continue;
        }

        out.append(plainText, pos, i);
        pos = i + charCount;
        switch (codePoint) {
        case '\b':
          out.append("\\b");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\f':
          out.append("\\f");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '/':
          out.append("\\/");
          break;
        case '"':
          out.append("\\\"");
          break;
        default:
          appendHexJavaScriptRepresentation(codePoint, out);
          break;
        }
      }
      out.append(plainText, pos, len);
    }

    private boolean mustEscapeCharInJsString(int codepoint) {
      if (!Character.isSupplementaryCodePoint(codepoint)) {
        final char c = (char) codepoint;
        return JS_ESCAPE_CHARS.contains(c) || (this.escapeHtmlCharacters && HTML_ESCAPE_CHARS.contains(c));
      }
      return false;
    }

    private static boolean isControlCharacter(int codePoint) {
      // JSON spec defines these code points as control characters, so they must be
      // escaped
      return codePoint < 0x20 || codePoint == 0x2028 // Line separator
          || codePoint == 0x2029 // Paragraph separator
          || (codePoint >= 0x7f && codePoint <= 0x9f);
    }

    private static void appendHexJavaScriptRepresentation(int codePoint, Appendable out) throws IOException {
      if (Character.isSupplementaryCodePoint(codePoint)) {
        // Handle supplementary unicode values which are not representable in
        // javascript. We deal with these by escaping them as two 4B sequences
        // so that they will round-trip properly when sent from java to javascript
        // and back.
        final char[] surrogates = Character.toChars(codePoint);
        appendHexJavaScriptRepresentation(surrogates[0], out);
        appendHexJavaScriptRepresentation(surrogates[1], out);
        return;
      }
      out.append("\\u").append(HEX_CHARS[(codePoint >>> 12) & 0xf]).append(HEX_CHARS[(codePoint >>> 8) & 0xf]).append(HEX_CHARS[(codePoint >>> 4) & 0xf]).append(HEX_CHARS[codePoint & 0xf]);
    }
  }

  public static class MalformedJsonException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MalformedJsonException(String msg) {
      super(msg);
    }
  }

  private static class Reader {
    private static final Object OBJECT_END = new String("}");
    private static final Object ARRAY_END = new String("]");
    private static final Object OBJECT_START = new String("{");
    private static final Object ARRAY_START = new String("[");
    private static final Object COLON = new String(":");
    private static final Object COMMA = new String(",");
    private static final HashSet<Object> PUNCTUATION = new HashSet<Object>(Arrays.asList(OBJECT_END, OBJECT_START, ARRAY_END, ARRAY_START, COLON, COMMA));
    public static final int FIRST = 0;
    public static final int CURRENT = 1;
    public static final int NEXT = 2;

    private static Map<Character, Character> escapes = new HashMap<Character, Character>();
    static {
      escapes.put(new Character('"'), new Character('"'));
      escapes.put(new Character('\\'), new Character('\\'));
      escapes.put(new Character('/'), new Character('/'));
      escapes.put(new Character('b'), new Character('\b'));
      escapes.put(new Character('f'), new Character('\f'));
      escapes.put(new Character('n'), new Character('\n'));
      escapes.put(new Character('r'), new Character('\r'));
      escapes.put(new Character('t'), new Character('\t'));
    }

    private CharacterIterator it;
    private char c;
    private Object token;
    private final StringBuffer buf = new StringBuffer();

    private char next() {
      if (this.it.getIndex() == this.it.getEndIndex()) {
        throw new MalformedJsonException("Reached end of input at the " + this.it.getIndex() + "th character.");
      }
      this.c = this.it.next();
      return this.c;
    }

    private char previous() {
      this.c = this.it.previous();
      return this.c;
    }

    private void skipWhiteSpace() {
      do {
        if (Character.isWhitespace(this.c)) {
          ;
        } else if (this.c == '/') {
          next();
          if (this.c == '*') {
            // skip multiline comments
            while (this.c != CharacterIterator.DONE) {
              if (next() == '*' && next() == '/') {
                break;
              }
            }
            if (this.c == CharacterIterator.DONE) {
              throw new MalformedJsonException("Unterminated comment while parsing JSON string.");
            }
          } else if (this.c == '/') {
            while (this.c != '\n' && this.c != CharacterIterator.DONE) {
              next();
            }
          } else {
            previous();
            break;
          }
        } else {
          break;
        }
      } while (next() != CharacterIterator.DONE);
    }

    public Object read(CharacterIterator ci, int start) {
      this.it = ci;
      switch (start) {
      case FIRST:
        this.c = this.it.first();
        break;
      case CURRENT:
        this.c = this.it.current();
        break;
      case NEXT:
        this.c = this.it.next();
        break;
      }
      return read();
    }

    public Object read(CharacterIterator it) {
      return read(it, NEXT);
    }

    public Object read(String string) {
      return read(new StringCharacterIterator(string), FIRST);
    }

    private void expected(Object expectedToken, Object actual) {
      if (expectedToken != actual) {
        throw new MalformedJsonException("Expected " + expectedToken + ", but got " + actual + " instead");
      }
    }

    @SuppressWarnings("unchecked")
    private <T> T read() {
      skipWhiteSpace();
      final char ch = this.c;
      next();
      switch (ch) {
      case '"':
        this.token = readString();
        break;
      case '[':
        this.token = readArray();
        break;
      case ']':
        this.token = ARRAY_END;
        break;
      case ',':
        this.token = COMMA;
        break;
      case '{':
        this.token = readObject();
        break;
      case '}':
        this.token = OBJECT_END;
        break;
      case ':':
        this.token = COLON;
        break;
      case 't':
        if (this.c != 'r' || next() != 'u' || next() != 'e') {
          throw new MalformedJsonException("Invalid JSON token: expected 'true' keyword.");
        }
        next();
        this.token = factory().bool(Boolean.TRUE);
        break;
      case 'f':
        if (this.c != 'a' || next() != 'l' || next() != 's' || next() != 'e') {
          throw new MalformedJsonException("Invalid JSON token: expected 'false' keyword.");
        }
        next();
        this.token = factory().bool(Boolean.FALSE);
        break;
      case 'n':
        if (this.c != 'u' || next() != 'l' || next() != 'l') {
          throw new MalformedJsonException("Invalid JSON token: expected 'null' keyword.");
        }
        next();
        this.token = nil();
        break;
      default:
        this.c = this.it.previous();
        if (Character.isDigit(this.c) || this.c == '-') {
          this.token = readNumber();
        } else {
          throw new MalformedJsonException("Invalid JSON near position: " + this.it.getIndex());
        }
      }
      return (T) this.token;
    }

    private String readObjectKey() {
      final Object key = read();
      if (key == null) {
        throw new MalformedJsonException("Missing object key (don't forget to put quotes!).");
      } else if (key == OBJECT_END) {
        return null;
      } else if (PUNCTUATION.contains(key)) {
        throw new MalformedJsonException("Missing object key, found: " + key);
      } else {
        return ((Json) key).asString();
      }
    }

    private Json readObject() {
      final Json ret = object();
      String key = readObjectKey();
      while (this.token != OBJECT_END) {
        expected(COLON, read()); // should be a colon
        if (this.token != OBJECT_END) {
          final Json value = read();
          ret.set(key, value);
          if (read() == COMMA) {
            key = readObjectKey();
            if (key == null || PUNCTUATION.contains(key)) {
              throw new MalformedJsonException("Expected a property name, but found: " + key);
            }
          } else {
            expected(OBJECT_END, this.token);
          }
        }
      }
      return ret;
    }

    private Json readArray() {
      final Json ret = array();
      Object value = read();
      while (this.token != ARRAY_END) {
        if (PUNCTUATION.contains(value)) {
          throw new MalformedJsonException("Expected array element, but found: " + value);
        }
        ret.add((Json) value);
        if (read() == COMMA) {
          value = read();
          if (value == ARRAY_END) {
            throw new MalformedJsonException("Expected array element, but found end of array after command.");
          }
        } else {
          expected(ARRAY_END, this.token);
        }
      }
      return ret;
    }

    private Json readNumber() {
      int length = 0;
      boolean isFloatingPoint = false;
      this.buf.setLength(0);

      if (this.c == '-') {
        add();
      }
      length += addDigits();
      if (this.c == '.') {
        add();
        length += addDigits();
        isFloatingPoint = true;
      }
      if (this.c == 'e' || this.c == 'E') {
        add();
        if (this.c == '+' || this.c == '-') {
          add();
        }
        addDigits();
        isFloatingPoint = true;
      }

      final String s = this.buf.toString();
      final Number n = isFloatingPoint ? (length < 17) ? Double.valueOf(s) : new BigDecimal(s) : (length < 20) ? Long.valueOf(s) : new BigInteger(s);
      return factory().number(n);
    }

    private int addDigits() {
      int ret;
      for (ret = 0; Character.isDigit(this.c); ++ret) {
        add();
      }
      return ret;
    }

    private Json readString() {
      this.buf.setLength(0);
      while (this.c != '"') {
        if (this.c == '\\') {
          next();
          if (this.c == 'u') {
            add(unicode());
          } else {
            final Object value = escapes.get(new Character(this.c));
            if (value != null) {
              add(((Character) value).charValue());
            }
          }
        } else {
          add();
        }
      }
      next();
      return factory().string(this.buf.toString());
    }

    private void add(char cc) {
      this.buf.append(cc);
      next();
    }

    private void add() {
      add(this.c);
    }

    private char unicode() {
      int value = 0;
      for (int i = 0; i < 4; ++i) {
        switch (next()) {
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          value = (value << 4) + this.c - '0';
          break;
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
          value = (value << 4) + (this.c - 'a') + 10;
          break;
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
          value = (value << 4) + (this.c - 'A') + 10;
          break;
        }
      }
      return (char) value;
    }
  }
  // END Reader

  /**
   * Converts potentially Unicode input to punycode. If conversion fails, returns
   * the original input.
   *
   * @param input the string to convert, not null
   * @return converted input, or original input if conversion fails
   */
  // Needed by UrlValidator
  static String unicodeToASCII(final String input) {
    if (isOnlyASCII(input)) { // skip possibly expensive processing
      return input;
    }
    try {
      final String ascii = IDN.toASCII(input);
      if (IDNBUGHOLDER.IDN_TOASCII_PRESERVES_TRAILING_DOTS) {
        return ascii;
      }
      final int length = input.length();
      if (length == 0) {// check there is a last character
        return input;
      }
      // RFC3490 3.1. 1)
      // Whenever dots are used as label separators, the following
      // characters MUST be recognized as dots: U+002E (full stop), U+3002
      // (ideographic full stop), U+FF0E (fullwidth full stop), U+FF61
      // (halfwidth ideographic full stop).
      final char lastChar = input.charAt(length - 1);// fetch original last char
      switch (lastChar) {
      case '\u002E': // "." full stop
      case '\u3002': // ideographic full stop
      case '\uFF0E': // fullwidth full stop
      case '\uFF61': // halfwidth ideographic full stop
        return ascii + "."; // restore the missing stop
      default:
        return ascii;
      }
    } catch (final IllegalArgumentException e) { // input is not valid
      return input;
    }
  }

  private static class IDNBUGHOLDER {
    private static boolean keepsTrailingDot() {
      final String input = "a."; // must be a valid name
      return input.equals(IDN.toASCII(input));
    }

    private static final boolean IDN_TOASCII_PRESERVES_TRAILING_DOTS = keepsTrailingDot();
  }

  /*
   * Check if input contains only ASCII Treats null as all ASCII
   */
  private static boolean isOnlyASCII(final String input) {
    if (input == null) {
      return true;
    }
    for (int i = 0; i < input.length(); i++) {
      if (input.charAt(i) > 0x7F) { // CHECKSTYLE IGNORE MagicNumber
        return false;
      }
    }
    return true;
  }

  public static void main(String[] argv) {
    try {
      final URI assetUri = new URI("https://raw.githubusercontent.com/pudo/aleph/master/aleph/schema/entity/asset.json");
      final URI schemaRoot = new URI("https://raw.githubusercontent.com/pudo/aleph/master/aleph/schema/");

      // This fails
      Json.schema(assetUri);

      // And so does this
      final Json asset = Json.read(assetUri.toURL());
      Json.schema(asset, schemaRoot);
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }
}
