/*
 * Copyright 2021 Simple JSON Patch contributors
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
 */

package mjson;

/**
 * A simple (one class) implementation of
 * <a href="https://tools.ietf.org/html/rfc6902">RFC 6902 JSON Patch</a> using
 * Jackson.
 * <p>
 * This class just applies a patch to a JSON document, nothing fancy like diffs
 * or patch generation.
 * </p>
 */
public class JsonPatch {
  /**
   * Applies all JSON patch operations to a JSON document.
   *
   * @return the patched JSON document
   */
  public Json apply(Json patch, Json source) {
    if (!patch.isArray()) {
      throw new IllegalArgumentException("Invalid JSON patch, an array is required");
    }
    if (!source.isObject() && !source.isArray()) {
      throw new IllegalArgumentException("Invalid JSON document, an object or array is required");
    }

    Json result = source.dup();
    for (final Json operation : patch) {
      if (!operation.isObject()) {
        throw new IllegalArgumentException("Invalid operation: " + operation);
      }
      result = perform(operation, result);
    }

    return result;
  }

  /**
   * Perform one JSON patch operation
   *
   * @return the patched JSON document
   */
  protected Json perform(Json operation, Json doc) {
    final Json opNode = operation.at("op");
    if (opNode == null || !opNode.isString()) {
      throw new IllegalArgumentException("Invalid \"op\" property: " + opNode);
    }
    final String op = opNode.asString();
    final Json pathNode = operation.at("path");
    if (pathNode == null || !pathNode.isString()) {
      throw new IllegalArgumentException("Invalid \"path\" property: " + pathNode);
    }
    final String path = pathNode.asString();
    if (path.length() != 0 && path.charAt(0) != '/') {
      throw new IllegalArgumentException("Invalid \"path\" property: " + path);
    }

    switch (op) {

    case "add": {
      final Json value = operation.at("value");
      if (value == null) {
        throw new IllegalArgumentException("Missing \"value\" property");
      }
      return add(doc, path, value);
    }

    case "remove": {
      return remove(doc, path);
    }

    case "replace": {
      final Json value = operation.at("value");
      if (value == null) {
        throw new IllegalArgumentException("Missing \"value\" property");
      }
      return replace(doc, path, value);
    }

    case "move": {
      final Json fromNode = operation.at("from");
      if (fromNode == null || !fromNode.isString()) {
        throw new IllegalArgumentException("Invalid \"from\" property: " + fromNode);
      }
      final String from = fromNode.asString();
      if (from.length() != 0 && from.charAt(0) != '/') {
        throw new IllegalArgumentException("Invalid \"from\" property: " + fromNode);
      }
      return move(doc, path, from);
    }

    case "copy": {
      final Json fromNode = operation.at("from");
      if (fromNode == null || !fromNode.isString()) {
        throw new IllegalArgumentException("Invalid \"from\" property: " + fromNode);
      }
      final String from = fromNode.asString();
      if (from.length() != 0 && from.charAt(0) != '/') {
        throw new IllegalArgumentException("Invalid \"from\" property: " + fromNode);
      }
      return copy(doc, path, from);
    }

    case "test": {
      final Json value = operation.at("value");
      if (value == null) {
        throw new IllegalArgumentException("Missing \"value\" property");
      }
      return test(doc, path, value);
    }

    default:
      throw new IllegalArgumentException("Invalid \"op\" property: " + op);
    }
  }

  /**
   * Perform a JSON patch "add" operation on a JSON document
   *
   * @return the patched JSON document
   */
  protected Json add(Json doc, String path, Json value) {
    if (path.isEmpty()) {
      return value;
    }

    // get the path parent
    Json parent = null;
    final int lastPathIndex = path.lastIndexOf('/');
    if (lastPathIndex < 1) {
      parent = doc;
    } else {
      final String parentPath = path.substring(0, lastPathIndex);
      parent = Json.resolvePointer(parentPath, doc);
    }

    if (parent.isObject()) {
      // adding to an object
      final Json parentObject = parent;
      final String key = path.substring(lastPathIndex + 1);
      parentObject.set(key, value);
    } else if (parent.isArray()) {
      // adding to an array
      final String key = path.substring(lastPathIndex + 1);
      final Json parentArray = parent;
      if (key.equals("-")) {
        parentArray.add(value);
      } else {
        try {
          final int idx = Integer.parseInt(key);
          if (idx > parentArray.asJsonList().size() || idx < 0) {
            throw new IllegalArgumentException("Array index is out of bounds: " + idx);
          }
          while (parentArray.asJsonList().size() <= idx) {
            parentArray.add(Json.make(null));
          }
          parentArray.set(idx, value);
        } catch (final NumberFormatException e) {
          throw new IllegalArgumentException("Invalid array index: " + key, e);
        }
      }
    } else {
      throw new IllegalArgumentException("Invalid \"path\" property: " + path);
    }

    return doc;
  }

  /**
   * Perform a JSON patch "remove" operation on a JSON document
   *
   * @return the patched JSON document
   */
  protected Json remove(Json doc, String path) {
    if (path.equals("")) {
      if (doc.isObject()) {
        final Json docObject = doc;
        for (final String key : docObject.asJsonMap().keySet()) {
          docObject.delAt(key);
        }
        return doc;
      } else if (doc.isArray()) {
        final Json docArray = doc;
        for (int i = 0; i < docArray.asJsonList().size(); i++) {
          docArray.delAt(0);
        }
        return doc;
      }
    }

    // get the path parent
    Json parent = null;
    final int lastPathIndex = path.lastIndexOf('/');
    if (lastPathIndex == 0) {
      parent = doc;
    } else {
      final String parentPath = path.substring(0, lastPathIndex);
      parent = Json.resolvePointer(parentPath, doc);
      if (parent == null) {
        throw new IllegalArgumentException("Path does not exist: " + path);
      }
    }

    // removing from an object
    final String key = path.substring(lastPathIndex + 1);
    if (parent.isObject()) {
      final Json parentObject = parent;
      if (!parent.has(key)) {
        throw new IllegalArgumentException("Property does not exist: " + key);
      }
      parentObject.delAt(key);
    }

    // removing from an array
    else if (parent.isArray()) {
      try {
        final Json parentArray = parent;
        final int idx = Integer.parseInt(key);
        if (parent.asJsonList().size() <= idx) {
          throw new IllegalArgumentException("Index does not exist: " + key);
        }
        parentArray.delAt(idx);
      } catch (final NumberFormatException e) {
        throw new IllegalArgumentException("Invalid array index: " + key);
      }
    }

    else {
      throw new IllegalArgumentException("Invalid \"path\" property: " + path);
    }

    return doc;
  }

  /**
   * Perform a JSON patch "replace" operation on a JSON document
   *
   * @return the patched JSON document
   */
  protected Json replace(Json doc, String path, Json value) {
    doc = remove(doc, path);
    return add(doc, path, value);
  }

  /**
   * Perform a JSON patch "move" operation on a JSON document
   *
   * @return the patched JSON document
   */
  protected Json move(Json doc, String path, String from) {
    // get the value
    final Json value = Json.resolvePointer(from, doc);
    if (value == null) {
      throw new IllegalArgumentException("Invalid \"from\" property: " + from);
    }

    // do remove and then add
    doc = remove(doc, from);
    return add(doc, path, value);
  }

  /**
   * Perform a JSON patch "copy" operation on a JSON document
   *
   * @return the patched JSON document
   */
  protected Json copy(Json doc, String path, String from) {
    // get the value
    final Json value = Json.resolvePointer(from, doc);
    if (value == null) {
      throw new IllegalArgumentException("Invalid \"from\" property: " + from);
    }

    // do add
    return add(doc, path, value);
  }

  /**
   * Perform a JSON patch "test" operation on a JSON document
   *
   * @return the patched JSON document
   */
  protected Json test(Json doc, String path, Json value) {
    final Json node = Json.resolvePointer(path, doc);
    if (node == null) {
      throw new IllegalArgumentException("Invalid \"path\" property: " + path);
    }

    if (!node.equals(value)) {
      throw new IllegalArgumentException("The value does not equal path node");
    }

    return doc;
  }
}
