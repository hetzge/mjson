package mjson;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import mjson.Json.Function;

public final class JsonSchema implements Json.Schema {

  private static final int MAX_CHARACTERS = 200;

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
  static class IsObject implements Instruction {
    @Override
    public Json apply(Json param) {
      return param.isObject() ? null : Json.make(param.toString(MAX_CHARACTERS));
    }
  }

  static class IsArray implements Instruction {
    @Override
    public Json apply(Json param) {
      return param.isArray() ? null : Json.make(param.toString(MAX_CHARACTERS));
    }
  }

  static class IsString implements Instruction {
    @Override
    public Json apply(Json param) {
      return param.isString() ? null : Json.make(param.toString(MAX_CHARACTERS));
    }
  }

  static class IsBoolean implements Instruction {
    @Override
    public Json apply(Json param) {
      return param.isBoolean() ? null : Json.make(param.toString(MAX_CHARACTERS));
    }
  }

  static class IsNull implements Instruction {
    @Override
    public Json apply(Json param) {
      return param.isNull() ? null : Json.make(param.toString(MAX_CHARACTERS));
    }
  }

  static class IsNumber implements Instruction {
    @Override
    public Json apply(Json param) {
      return param.isNumber() ? null : Json.make(param.toString(MAX_CHARACTERS));
    }
  }

  static class IsInteger implements Instruction {
    @Override
    public Json apply(Json param) {
      return param.isNumber() && ((Number) param.getValue()) instanceof Integer ? null : Json.make(param.toString(MAX_CHARACTERS));
    }
  }

  static class CheckString implements Instruction {
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
        errors = maybeError(errors, Json.make("String " + param.toString(MAX_CHARACTERS) + " has length outside of the permitted range [" + this.min + "," + this.max + "]."));
      }
      if (this.pattern != null && !this.pattern.matcher(s).find()) {
        errors = maybeError(errors, Json.make("String " + param.toString(MAX_CHARACTERS) + " does not match regex '" + this.pattern.toString() + "'"));
      }
      return errors;
    }
  }

  static class CheckNumber implements Instruction {
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

  static class StartContext implements Instruction {

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

  static class EndContext implements Instruction {

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

//      for (int i = 0; i < json.asJsonList().size(); i++) {
//        if (i >= ValidationContext.LOCAL.get().getEvaluatedCount(json)) {
//          errors = maybeError(errors, this.unevaluatedSchema.apply(json.at(i)));
//        }
//      }
//      ValidationContext.LOCAL.get().setEvaluatedCount(json, json.asJsonList().size());

      // TODO
      System.out.println(evaluated.toString());

      return errors;
    }
  }

  static class WithContext implements Instruction {
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

  static class CheckIfThenElse implements Instruction {
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

  static class CheckArray implements Instruction {
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
            errors = maybeError(errors, Json.make("Additional items are not permitted: " + item + " in " + param.toString(MAX_CHARACTERS)));
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
          errors = maybeError(errors, Json.make(format("Array requires minimum %s matches", this.minContains)));
        }
        if (size < this.min || size > this.max) {
          errors = maybeError(errors, Json.make("Array  " + param.toString(MAX_CHARACTERS) + " has number of elements outside of the permitted range [" + this.min + "," + this.max + "]."));
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

  static class CheckPropertyPresent implements Instruction {
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
        return Json.array().add(Json.make("Required property " + this.propname + " missing from object " + param.toString(MAX_CHARACTERS)));
      }
    }
  }

  static class CheckObject implements Instruction {
    int min = 0, max = Integer.MAX_VALUE;
    Instruction additionalSchema = any;
    ArrayList<CheckProperty> props = new ArrayList<CheckProperty>();
    ArrayList<CheckPatternProperty> patternProps = new ArrayList<CheckPatternProperty>();
    Instruction propertyNames;

    // Object validation
    static class CheckProperty implements Instruction {
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

    static class CheckPatternProperty // implements Instruction
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
            errors = maybeError(errors, Json.make(format("Property name '%s' is not valid", propertyName)));
          }
        }
      }
      if (param.asJsonMap().size() < this.min) {
        errors = maybeError(errors, Json.make("Object " + param.toString(MAX_CHARACTERS) + " has fewer than the permitted " + this.min + "  number of properties."));
      }
      if (param.asJsonMap().size() > this.max) {
        errors = maybeError(errors, Json.make("Object " + param.toString(MAX_CHARACTERS) + " has more than the permitted " + this.min + "  number of properties."));
      }
      return errors;
    }
  }

  static class Sequence implements Instruction {
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

  static class CheckType implements Instruction {
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
      return Json.array().add(Json.make("Type mistmatch for " + param.toString(MAX_CHARACTERS) + ", allowed types: " + this.types));
    }
  }

  static class CheckEnum implements Instruction {
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
      return Json.array().add("Element " + param.toString(MAX_CHARACTERS) + " doesn't match any of enumerated possibilities " + this.theenum);
    }
  }

  static class CheckEquals implements Instruction {
    Json value;

    public CheckEquals(Json value) {
      this.value = value;
    }

    @Override
    public Json apply(Json param) {
      return param.equals(this.value) ? null : Json.array().add("Element " + param.toString(MAX_CHARACTERS) + " is not equal " + param.toString(MAX_CHARACTERS));
    }
  }

  static class CheckAny implements Instruction {
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
      return Json.array().add("Element " + param.toString(MAX_CHARACTERS) + " must conform to at least one of available sub-schemas " + this.schema.toString(MAX_CHARACTERS));
    }
  }

  static class CheckOne implements Instruction {
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
        return Json.array().add("Element " + param.toString(MAX_CHARACTERS) + " must conform to exactly one of available sub-schemas, but not more " + this.schema.toString(MAX_CHARACTERS)).add(errors);
      } else {
        return null;
      }
    }
  }

  static class CheckNot implements Instruction {
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
        return Json.array().add("Element " + param.toString(MAX_CHARACTERS) + " must NOT conform to the schema " + this.schema.toString(MAX_CHARACTERS));
      }
    }
  }

  static class CheckSchemaDependency implements Instruction {
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

  static class CheckPropertyDependency implements Instruction {
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
            errors = maybeError(errors, Json.make("Conditionally required property " + p + " missing from object " + param.toString(MAX_CHARACTERS)));
          }
        }
        return errors;
      }
    }
  }

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

  private final Json json;
  private final Instruction start;

  private JsonSchema(Json json, Instruction start) {
    this.json = json;
    this.start = start;
  }

  @Override
  public Json validate(Json document) {
    final Json result = Json.object("ok", true);
    final Json errors = this.start.apply(document);
    return errors == null ? result : result.set("errors", errors).set("ok", false);
  }

  @Override
  public Json toJson() {
    return this.json;
  }

  public static JsonSchema initialize(Json json) {
    final URI rootUri = URI.create("http://__default__");
    final JsonIndex index = index(json, rootUri);
    resolveRefs(0, "root", json, rootUri, index, new LinkedList<>(), new IdentityHashMap<>());
    return new JsonSchema(json, compile(json, new IdentityHashMap<>()));
  }

  private static JsonIndex index(Json json, URI rootUri) {
    final HashMap<URI, Json> index = new HashMap<>();
    index.put(rootUri, json);
    index.put(rootUri.resolve("#"), json);
    initIndex(0, "root", index, json, rootUri, new IdentityHashMap<>());
    return new JsonIndex(index);
  }

  private static void initIndex(int level, String field, Map<URI, Json> index, Json json, URI uri, Map<Json, Json> alreadyIndexed) {
    System.out.println("<i>" + "-".repeat(level * 2) + field);
    if (alreadyIndexed.containsKey(json)) {
      // Already indexed
      return;
    } else {
      // Mark already indexed
      alreadyIndexed.put(json, json);
    }
    if (json.isObject()) {
      if (json.has("$id") && !field.equals("properties")) {
        final String id = json.at("$id").asString();
        uri = resolveUri(uri, id);
        System.out.println("index " + uri.toString());
        index.put(uri, json);
      }
      if (json.has("$anchor") && !field.equals("properties")) {
        final String anchor = json.at("$anchor").asString();
        final URI anchorUri = resolveUri(uri, "#" + anchor);
        System.out.println("index " + anchorUri.toString());
        index.put(anchorUri, json);
      }
      if (json.has("$dynamicAnchor") && !field.equals("properties")) {
        final String dynamicAnchor = json.at("$dynamicAnchor").asString();
        final URI dynamicAnchorUri = resolveUri(uri, "#" + dynamicAnchor);
        System.out.println("index dynamic " + dynamicAnchorUri.toString());
        index.put(dynamicAnchorUri, json);
      }
      for (final Entry<String, Json> entry : json.asJsonMap().entrySet()) {
        initIndex(level + 1, entry.getKey(), index, entry.getValue(), uri, alreadyIndexed);
      }
    } else if (json.isArray()) {
      for (final Json item : json.asJsonList()) {
        initIndex(level + 1, "[]", index, item, uri, alreadyIndexed);
      }
    }
  }

  static URI resolveUri(URI uri, String sub) {
    if (uri.getScheme() != null && uri.getScheme().equals("urn")) {
      return URI.create(uri.toString() + sub);
    } else {
      System.out.println("resolve " + uri + " with " + sub + " = " + uri.resolve(sub));
      return uri.resolve(sub);
    }
  }

  private static void resolveRefs(int level, String field, Json json, URI uri, JsonIndex index, LinkedList<URI> scopes, Map<Json, Json> alreadyResolved) {
    System.out.println("<r>" + "-".repeat(level * 2) + field);
    if (alreadyResolved.containsKey(json)) {
      // Already resolved
      return;
    } else {
      // Mark already resolved
      alreadyResolved.put(json, json);
    }
    if (json.isObject()) {
      if (json.has("$id") && !field.equals("properties")) {
        final String id = json.at("$id").asString();
        uri = resolveUri(uri, id);
      }
      scopes.add(uri);
      if (json.has("$ref") && !field.equals("properties")) {
        final String ref = json.at("$ref").asString();
        json.delAt("$ref"); // mark as resolved
        final URI refUri = resolveUri(uri, ref);
        final Json resolved = resolve(index, refUri);
        resolveRefs(level + 1, "$ref", resolved, refUri, index, scopes, alreadyResolved);
        deepMerge(json, resolved);
      }
      if (json.has("$dynamicRef") && !field.equals("properties")) {
        final String dynamicRef = json.at("$dynamicRef").asString();
        final Json dynamicResolved = scopes.stream().map(scope -> scope.resolve(dynamicRef)).peek(it -> System.out.println("Scope: " + it)).map(index::get).filter(Optional::isPresent).map(Optional::get).findFirst().orElseThrow();
        deepMerge(json, dynamicResolved); // TODO prevent recursive loop
        // TODO not sure if this is correct !?
        if (json.has("$id") && !field.equals("properties")) {
          final String id = json.at("$id").asString();
          uri = resolveUri(uri, id);
        }
      }
      for (final Entry<String, Json> entry : json.asJsonMap().entrySet()) {
        resolveRefs(level + 1, entry.getKey(), entry.getValue(), uri, index, scopes, alreadyResolved);
      }
      scopes.pollLast();
    } else if (json.isArray()) {
      for (final Json item : json.asJsonList()) {
        resolveRefs(level + 1, "[]", item, uri, index, scopes, alreadyResolved);
      }
    }
  }

  public static Json resolve(JsonIndex index, URI uri) {
    requireNonNull(index, "'index' is null");
    requireNonNull(uri, "'uri' is null");
    return JsonUriUtils.getPointer(uri).map(pointer -> {
      return resolvePointer(resolveInIndex(index, JsonUriUtils.getSchemaUri(uri)), pointer);
    }).or(() -> JsonUriUtils.getAnchor(uri).map(anchor -> {
      return resolveInIndex(index, uri);
    })).or(() -> {
      return Optional.of(resolveInIndex(index, JsonUriUtils.getSchemaUri(uri)));
    }).orElseThrow(() -> new RuntimeException(format("Failed to resolve '%s'. Not found.", uri)));
  }

  private static Json resolveInIndex(JsonIndex index, URI uri) {
    requireNonNull(index, "'index' is null");
    requireNonNull(uri, "'uri' is null");
    return index.get(uri).orElseGet(() -> {
      try {
        System.out.println("fetch from " + uri.toString());
        final Json fetchedJson = Json.read(uri.toURL());
        index.add(index(fetchedJson, JsonUriUtils.getSchemaUri(uri)));
        return fetchedJson;
      } catch (final MalformedURLException exception) {
        throw new RuntimeException(exception);
      }
    });
  }

  private static Json resolvePointer(Json json, String pointer) {
    requireNonNull(json, "'json' is null");
    requireNonNull(pointer, "'pointer' is null");
    if (pointer.isBlank()) {
      return json;
    }
    final String[] steps = pointer.split("/");
    Json result = json;
    for (final String step : steps) {
      if (step.isBlank()) {
        continue;
      }
      result = getAt(result, step.replace("~1", "/").replace("~0", "~"));
    }
    return result;
  }

  private static Json getAt(Json json, String at) {
    requireNonNull(json, format("Try to get at '%s' on null object", at));
    requireNonNull(at, "'at' is null");
    if (json.isArray()) {
      return json.at(parseInt(at));
    } else if (json.isObject()) {
      return json.at(at);
    } else {
      throw new RuntimeException(format("Can't resolve at '%s' in document '%s'", at, json.toString(200)));
    }
  }

  static Instruction compile(Json schemaJson, Map<Json, Instruction> compiled) {
    return compile(schemaJson, compiled, false);
  }

  static Instruction compile(Json schemaJson, Map<Json, Instruction> compiled, boolean ignoreEvaluation) {
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
          seq.add(json -> {
            if (!json.isString()) {
              return null;
            }
            try {
              URI.create(json.asString());
              return null;
            } catch (final IllegalArgumentException e) {
              return Json.array().add("Element " + json.toString(MAX_CHARACTERS) + " is not a valid uri");
            }
          });
        } else if (schemaJson.is("format", "idn-hostname")) {
          seq.add(json -> {
            if (!json.isString()) {
              return null;
            }
            try {
              IDN.toASCII(json.asString());
              return null;
            } catch (final IllegalArgumentException e) {
              return Json.array().add("Element " + json.toString(MAX_CHARACTERS) + " is not a valid idn hostname");
            }
          });
        } else if (schemaJson.is("format", "uri-reference")) {
          seq.add(json -> {
            if (!json.isString()) {
              return null;
            }
            try {
              URI.create(json.asString());
              return null;
            } catch (final IllegalArgumentException e) {
              return Json.array().add("Element " + json.toString(MAX_CHARACTERS) + " is not a valid uri reference");
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
        objectCheck.props.add(new CheckObject.CheckProperty(p.getKey(), compile(p.getValue(), compiled)));
      }
    }
    if (schemaJson.has("patternProperties")) {
      for (final Map.Entry<String, Json> p : schemaJson.at("patternProperties").asJsonMap().entrySet()) {
        objectCheck.patternProps.add(new CheckObject.CheckPatternProperty(p.getKey(), compile(p.getValue(), compiled)));
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

  private static Json deepMerge(Json a, Json b) {
    if (a != null && b != null && a.isObject() && b.isObject()) {
      final Json result = a;
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

  private static class JsonUriUtils {

    private JsonUriUtils() {
    }

    public static Optional<String> getPointer(URI uri) {
      return Optional.ofNullable(uri.getFragment()).filter(JsonUriUtils::isPointer);
    }

    public static Optional<String> getAnchor(URI uri) {
      return Optional.ofNullable(uri.getFragment()).filter(not(JsonUriUtils::isPointer));
    }

    public static URI getSchemaUri(URI uri) {
      return URI.create(uri.toString().replace("#" + uri.getFragment(), ""));
    }

    public static boolean isPointer(String fragment) {
      return fragment.startsWith("/");
    }
  }

  private static class JsonIndex {
    private final Map<URI, Json> index;

    public JsonIndex(Map<URI, Json> index) {
      requireNonNull(index, "'index' is null");
      this.index = index;
    }

    public void add(JsonIndex other) {
      requireNonNull(other, "'other' is null");
      this.index.putAll(other.index);
    }

    public Optional<Json> get(URI uri) {
      requireNonNull(uri, "'uri' is null");
      return Optional.ofNullable(this.index.get(uri));
    }
  }
}
