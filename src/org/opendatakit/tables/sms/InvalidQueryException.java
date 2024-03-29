/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.sms;

/**
 * An exception for invalid queries
 */
public class InvalidQueryException extends Exception {
	
    private static final long serialVersionUID = 1L;
    
    enum Type {
        INVALID_FORMAT,
        NONEXISTENT_TARGET
    }
    
    private Type type;
    
    public InvalidQueryException(Type type) {
        this(type, null);
    }
    
    public InvalidQueryException(Type type, String msg) {
        super(msg);
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
}
