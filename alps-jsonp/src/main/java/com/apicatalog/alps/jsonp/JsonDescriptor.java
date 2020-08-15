/*
 * Copyright 2020 the original author or authors.
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
package com.apicatalog.alps.jsonp;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.apicatalog.alps.dom.element.Descriptor;
import com.apicatalog.alps.dom.element.DescriptorType;
import com.apicatalog.alps.dom.element.Documentation;
import com.apicatalog.alps.dom.element.Extension;
import com.apicatalog.alps.dom.element.Link;
import com.apicatalog.alps.error.DocumentError;
import com.apicatalog.alps.error.InvalidDocumentException;

final class JsonDescriptor implements Descriptor {

    private URI id;
    
    private URI href;
    
    private String name;
    
    private DescriptorType type;
    
    private URI returnType;
    
    private Set<Documentation> doc;
    
    private Set<Descriptor> descriptors;
    
    private Set<Link> links;
    
    private Set<Extension> extensions;
    
    private Descriptor parent;
    
    private JsonDescriptor() {
        // default values
        this.type = DescriptorType.SEMANTIC;
    }
    
    @Override
    public Optional<URI> getId() {
        return Optional.ofNullable(id);
    }

    @Override
    public Optional<URI> getHref() {
        return Optional.ofNullable(href);
    }

    @Override
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public DescriptorType getType() {
        return type;
    }

    @Override
    public Optional<URI> getReturnType() {
        return Optional.ofNullable(returnType);
    }

    @Override
    public Set<Documentation> getDocumentation() {
        return doc;
    }

    @Override
    public Set<Extension> getExtensions() {
        return extensions;
    }

    @Override
    public Set<Descriptor> getDescriptors() {
        return descriptors;
    }

    @Override
    public Set<Link> getLinks() {
        return links;
    }
    
    @Override
    public Optional<Descriptor> getParent() {
        return Optional.ofNullable(parent);
    }

    public static Set<Descriptor> parse(Map<URI, Descriptor> index, JsonValue jsonValue) throws InvalidDocumentException {
        return parse(index, null, jsonValue);
    }
    
    private static Set<Descriptor> parse(Map<URI, Descriptor> index, JsonDescriptor parent, JsonValue jsonValue) throws InvalidDocumentException {
        
        if (JsonUtils.isObject(jsonValue)) {

            return Set.of(parseObject(index, parent, jsonValue.asJsonObject()));

        } else if (JsonUtils.isArray(jsonValue)) {
            
            final HashSet<Descriptor> descriptors = new HashSet<>();
            
            for (final JsonValue item : jsonValue.asJsonArray()) {
                
                if (JsonUtils.isObject(item)) {
                    descriptors.add(parseObject(index, parent, item.asJsonObject()));
                    
                } else {
                    throw new InvalidDocumentException(DocumentError.INVALID_DESCRIPTOR, "The 'descriptor' property must be an object or an array of objects but was " + item.getValueType());
                }
            }
            
            return descriptors;
            
        } else {
            throw new InvalidDocumentException(DocumentError.INVALID_DESCRIPTOR, "The 'descriptor' property must be an object or an array of objects but was " + jsonValue.getValueType());
        }
    }
    
    private static Descriptor parseObject(Map<URI, Descriptor> index, JsonDescriptor parent, JsonObject jsonObject) throws InvalidDocumentException {
        
        final JsonDescriptor descriptor = new JsonDescriptor();
        descriptor.parent = parent;

        // id
        if (!jsonObject.containsKey(JsonConstants.ID) || JsonUtils.isNull(jsonObject.get(JsonConstants.ID))) {
            throw new InvalidDocumentException(DocumentError.MISSING_ID, "The 'id' property value must be valid URI represented as JSON string");            
        }
        
        if (JsonUtils.isNotString(jsonObject.get(JsonConstants.ID))) {
            throw new InvalidDocumentException(DocumentError.INVALID_ID, "The 'id' property value must be valid URI represented as JSON string but was " + jsonObject.get(JsonConstants.ID));
        }
        
        try {
            descriptor.id = URI.create(jsonObject.getString(JsonConstants.ID));
                    
        } catch (IllegalArgumentException e) {
            throw new InvalidDocumentException(DocumentError.MALFORMED_URI, "The 'id' must be valid URI but was " + jsonObject.getString(JsonConstants.ID));
        }
        
        // check id conflict
        if (index.containsKey(descriptor.id)) {
            throw new InvalidDocumentException(DocumentError.DUPLICATED_ID, "Duplicate 'id' property value " + descriptor.id);
        }
        
        index.put(descriptor.id, descriptor);

        // name
        if (jsonObject.containsKey(JsonConstants.NAME)) {
            final JsonValue name = jsonObject.get(JsonConstants.NAME);
            
            if (JsonUtils.isNotString(name)) {
                throw new InvalidDocumentException(DocumentError.INVALID_NAME, "The 'name' property value must be JSON string but was " + name);
            }
            
            descriptor.name = JsonUtils.getString(name);
        }

        // type
        if (jsonObject.containsKey(JsonConstants.TYPE)) {
            
            final JsonValue type = jsonObject.get(JsonConstants.TYPE);
            
            if (JsonUtils.isNotString(type)) {
                throw new InvalidDocumentException(DocumentError.INVALID_TYPE, "The 'type' property value must be JSON string but was " + type);
            }
            
            try {
                descriptor.type = DescriptorType.valueOf(JsonUtils.getString(type).toUpperCase());
                
            } catch (IllegalArgumentException e) {
                throw new InvalidDocumentException(DocumentError.INVALID_TYPE, "The 'type' property value must be one of " + (Arrays.stream(DescriptorType.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.joining(", " ))) +  " but was " + type);
            }
        }

        // documentation
        if (jsonObject.containsKey(JsonConstants.DOCUMENTATION)) {
            descriptor.doc = JsonDocumentation.parse(jsonObject.get(JsonConstants.DOCUMENTATION));
            
        } else {
            descriptor.doc = Collections.emptySet();
        }
        
        // links
        if (jsonObject.containsKey(JsonConstants.LINK)) {
            descriptor.links = JsonLink.parse(jsonObject.get(JsonConstants.LINK));
            
        } else {
            descriptor.links = Collections.emptySet();
        }
        
        // href
        if (jsonObject.containsKey(JsonConstants.HREF)) {
            descriptor.href = JsonUtils.getHref(jsonObject);
        }

        // return type
        if (jsonObject.containsKey(JsonConstants.RETURN_TYPE)) {
            
            final JsonValue returnType = jsonObject.get(JsonConstants.RETURN_TYPE);
            
            if (JsonUtils.isNotString(returnType)) {
                throw new InvalidDocumentException(DocumentError.INVALID_RT, "The 'rt' property value must be URI represented as JSON string but was " + returnType);
            }

            try {
                descriptor.returnType = URI.create(JsonUtils.getString(returnType));
                
            } catch (IllegalArgumentException e) {
                throw new InvalidDocumentException(DocumentError.MALFORMED_URI, "The 'rt' property value must be URI represented as JSON string but was " + returnType);
            }            
        }
        
        // nested descriptors
        if (jsonObject.containsKey(JsonConstants.DESCRIPTOR)) {
            descriptor.descriptors = JsonDescriptor.parse(index, descriptor, jsonObject.get(JsonConstants.DESCRIPTOR));
            
        } else {
            descriptor.descriptors = Collections.emptySet();
        }
        
        // extensions
        if (jsonObject.containsKey(JsonConstants.EXTENSION)) {
            descriptor.extensions = JsonExtension.parse(jsonObject.get(JsonConstants.EXTENSION));
            
        } else {
            descriptor.extensions = Collections.emptySet();
        }
        
        return descriptor;
    }
    
    public static final JsonValue toJson(final Set<Descriptor> descriptors) {
        
        if (descriptors.size() == 1) {
            return toJson(descriptors.iterator().next());
        }
        
        final JsonArrayBuilder jsonDescriptors = Json.createArrayBuilder();
        
        descriptors.stream().map(JsonDescriptor::toJson).forEach(jsonDescriptors::add);
        
        return jsonDescriptors.build();
    }

    public static final JsonValue toJson(final Descriptor descriptor) {
        
        final JsonObjectBuilder jsonDescriptor = Json.createObjectBuilder();
        
        descriptor.getId().ifPresent(id -> jsonDescriptor.add(JsonConstants.ID, id.toString()));
        
        if (descriptor.getType() != null && !DescriptorType.SEMANTIC.equals(descriptor.getType())) {
            jsonDescriptor.add(JsonConstants.TYPE, descriptor.getType().name().toLowerCase());
        }
        
        descriptor.getHref().ifPresent(href -> jsonDescriptor.add(JsonConstants.HREF, href.toString()));
        descriptor.getName().ifPresent(name -> jsonDescriptor.add(JsonConstants.NAME, name));
        descriptor.getReturnType().ifPresent(rt -> jsonDescriptor.add(JsonConstants.RETURN_TYPE, rt.toString()));

        // documentation
        if (JsonDocument.isNotEmpty(descriptor.getDocumentation())) {
            jsonDescriptor.add(JsonConstants.DOCUMENTATION, JsonDocumentation.toJson(descriptor.getDocumentation()));
        }
        
        // descriptors
        if (JsonDocument.isNotEmpty(descriptor.getDescriptors())) {
            jsonDescriptor.add(JsonConstants.DESCRIPTOR, toJson(descriptor.getDescriptors()));
        }

        // links
        if (JsonDocument.isNotEmpty(descriptor.getLinks())) {
            jsonDescriptor.add(JsonConstants.LINK, JsonLink.toJson(descriptor.getLinks()));
        }

        // extensions
        if (JsonDocument.isNotEmpty(descriptor.getExtensions())) {
            jsonDescriptor.add(JsonConstants.EXTENSION, JsonExtension.toJson(descriptor.getExtensions()));
        }

        return jsonDescriptor.build();
    }
    
}
