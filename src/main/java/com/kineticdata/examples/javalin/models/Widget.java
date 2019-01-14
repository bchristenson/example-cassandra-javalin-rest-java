package com.kineticdata.examples.javalin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Widget {

    /*----------------------------------------------------------------------------------------------
     * CONSTRUCTOR
     *--------------------------------------------------------------------------------------------*/
    
    private final String tenantKey;
    private final String key;
    private final String description;

    @JsonCreator
    public Widget(
        @JsonProperty("tenantKey") String tenantKey, 
        @JsonProperty("key") String key, 
        @JsonProperty("description") String description
    ) {
        this.tenantKey = tenantKey;
        this.key = key;
        this.description = description;
    }
    
    /*----------------------------------------------------------------------------------------------
     * ACCESSORS
     *--------------------------------------------------------------------------------------------*/

    public String getTenantKey() {
        return tenantKey;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }
    
    /*----------------------------------------------------------------------------------------------
     * METHODS
     *--------------------------------------------------------------------------------------------*/
    
    public Builder builder() {
        return new Builder(this);
    }
    
    /*----------------------------------------------------------------------------------------------
     * BUILDER
     *--------------------------------------------------------------------------------------------*/
    
    public static class Builder {
        private String tenantKey;
        private String key;
        private String description;
        
        public Builder() {
            
        }
        
        public Builder(Widget widget) {
            this.tenantKey = widget.getTenantKey();
            this.key = widget.getKey();
            this.description = widget.getDescription();
        }
        
        @JsonCreator
        public Builder(
            @JsonProperty("tenantKey") String tenantKey, 
            @JsonProperty("key") String key, 
            @JsonProperty("description") String description
        ) {
            this.tenantKey = tenantKey;
            this.key = key;
            this.description = description;
        }

        public Widget build() {
            return new Widget(
                this.tenantKey,
                this.key,
                this.description
            );
        }

        public String getTenantKey() {
            return tenantKey;
        }

        public Builder setTenantKey(String tenantKey) {
            this.tenantKey = tenantKey;
            return this;
        }

        public String getKey() {
            return key;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }
        
    }
}
