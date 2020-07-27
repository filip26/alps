package com.apicatalog.alps.jsonp;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.apicatalog.alps.AlpsParserException;
import com.apicatalog.alps.dom.element.AlpsDescriptor;
import com.apicatalog.alps.dom.element.AlpsDescriptorType;
import com.apicatalog.alps.dom.element.AlpsDocumentation;
import com.apicatalog.alps.dom.element.AlpsExtension;
import com.apicatalog.alps.dom.element.AlpsLink;

final class JsonDescriptor implements AlpsDescriptor {

    private URI id;
    
    private URI href;
    
    private String name;
    
    private AlpsDescriptorType type;
    
    private URI returnType;
    
    private Set<AlpsDocumentation> doc;
    
    private Set<AlpsDescriptor> descriptors;
    
    private Set<AlpsLink> links;
    
    private Set<AlpsExtension> extensions;
    
    private AlpsDescriptor parent;
    
    private JsonDescriptor() {
        // default values
        this.type = AlpsDescriptorType.SEMANTIC;
    }
    
    @Override
    public URI getId() {
        return id;
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
    public AlpsDescriptorType getType() {
        return type;
    }

    @Override
    public Optional<URI> getReturnType() {
        return Optional.ofNullable(returnType);
    }

    @Override
    public Set<AlpsDocumentation> getDocumentation() {
        return doc;
    }

    @Override
    public Set<AlpsExtension> getExtensions() {
        return extensions;
    }

    @Override
    public Set<AlpsDescriptor> getDescriptors() {
        return descriptors;
    }

    @Override
    public Set<AlpsLink> getLinks() {
        return links;
    }
    
    @Override
    public Optional<AlpsDescriptor> getParent() {
        return Optional.ofNullable(parent);
    }

    public static Set<AlpsDescriptor> parse(Map<URI, AlpsDescriptor> index, JsonValue jsonValue) throws AlpsParserException {
        return parse(index, null, jsonValue);
    }
    
    private static Set<AlpsDescriptor> parse(Map<URI, AlpsDescriptor> index, JsonDescriptor parent, JsonValue jsonValue) throws AlpsParserException {
        
        if (JsonUtils.isObject(jsonValue)) {

            return Set.of(parseObject(index, parent, jsonValue.asJsonObject()));

        } else if (JsonUtils.isArray(jsonValue)) {
            
            final HashSet<AlpsDescriptor> descriptors = new HashSet<>();
            
            for (final JsonValue item : jsonValue.asJsonArray()) {
                
                if (JsonUtils.isObject(item)) {
                    descriptors.add(parseObject(index, parent, item.asJsonObject()));
                    
                } else {
                    throw new AlpsParserException("The 'descriptor' property must be an object or an array of objects but was " + item.getValueType());
                }
            }
            
            return descriptors;
            
        } else {
            throw new AlpsParserException("The 'descriptor' property must be an object or an array of objects but was " + jsonValue.getValueType());
        }
    }
    
    private static AlpsDescriptor parseObject(Map<URI, AlpsDescriptor> index, JsonDescriptor parent, JsonObject jsonObject) throws AlpsParserException {
        
        final JsonDescriptor descriptor = new JsonDescriptor();
        descriptor.parent = parent;
        
        // id
        if (JsonUtils.isNotString(jsonObject.get(AlpsJsonKeys.ID))) {
            throw new AlpsParserException("The 'id' property value must be valid URI represented as JSON string but was " + jsonObject.get(AlpsJsonKeys.ID));
        }
        
        try {
            descriptor.id = URI.create(jsonObject.getString(AlpsJsonKeys.ID));
                    
        } catch (IllegalArgumentException e) {
            throw new AlpsParserException("The 'id' must be valid URI but was " + jsonObject.getString(AlpsJsonKeys.ID));
        }
        
        // check id conflict
        if (index.containsKey(descriptor.id)) {
            throw new AlpsParserException("Duplicate 'id' property value " + descriptor.id);
        }
        
        index.put(descriptor.id, descriptor);

        // name
        if (jsonObject.containsKey(AlpsJsonKeys.NAME)) {
            final JsonValue name = jsonObject.get(AlpsJsonKeys.NAME);
            
            if (JsonUtils.isNotString(name)) {
                throw new AlpsParserException("The 'name' property value must be JSON string but was " + name);
            }
            
            descriptor.name = JsonUtils.getString(name);
        }

        // type
        if (jsonObject.containsKey(AlpsJsonKeys.TYPE)) {
            
            final JsonValue type = jsonObject.get(AlpsJsonKeys.TYPE);
            
            if (JsonUtils.isNotString(type)) {
                throw new AlpsParserException("The 'type' property value must be JSON string but was " + type);
            }
            
            try {
                descriptor.type = AlpsDescriptorType.valueOf(JsonUtils.getString(type).toUpperCase());
                
            } catch (IllegalArgumentException e) {
                throw new AlpsParserException("The 'type' property value must be one of " + (Arrays.stream(AlpsDescriptorType.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.joining(", " ))) +  " but was " + type);
            }
        }

        // documentation
        if (jsonObject.containsKey(AlpsJsonKeys.DOCUMENTATION)) {
            descriptor.doc = JsonDocumentation.parse(jsonObject.get(AlpsJsonKeys.DOCUMENTATION));
            
        } else {
            descriptor.doc = Collections.emptySet();
        }
        
        // links
        if (jsonObject.containsKey(AlpsJsonKeys.LINK)) {
            descriptor.links = JsonLink.parse(jsonObject.get(AlpsJsonKeys.LINK));
            
        } else {
            descriptor.links = Collections.emptySet();
        }
        
        // href
        if (jsonObject.containsKey(AlpsJsonKeys.HREF)) {
            descriptor.href = JsonUtils.getHref(jsonObject);
        }

        // return type
        if (jsonObject.containsKey(AlpsJsonKeys.RETURN_TYPE)) {
            
            final JsonValue returnType = jsonObject.get(AlpsJsonKeys.RETURN_TYPE);
            
            if (JsonUtils.isNotString(returnType)) {
                throw new AlpsParserException("The 'rt' property value must be URI represented as JSON string but was " + returnType);
            }

            try {
                descriptor.returnType = URI.create(JsonUtils.getString(returnType));
                
            } catch (IllegalArgumentException e) {
                throw new AlpsParserException("The 'rt' property value must be URI represented as JSON string but was " + returnType);
            }            
        }
        
        // nested descriptors
        if (jsonObject.containsKey(AlpsJsonKeys.DESCRIPTOR)) {
            descriptor.descriptors = JsonDescriptor.parse(index, descriptor, jsonObject.get(AlpsJsonKeys.DESCRIPTOR));
            
        } else {
            descriptor.descriptors = Collections.emptySet();
        }
        
        // extensions
        if (jsonObject.containsKey(AlpsJsonKeys.EXTENSION)) {
            descriptor.extensions = JsonExtesion.parse(jsonObject.get(AlpsJsonKeys.EXTENSION));
            
        } else {
            descriptor.extensions = Collections.emptySet();
        }
        
        return descriptor;
    }
}
