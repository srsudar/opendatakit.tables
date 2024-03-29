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
package org.opendatakit.tables.exceptions;

/**
 * Exception to show that a table has tried to be created identifiers that
 * already exist. Likely should only be used when the user cannot simply 
 * be notified with a toast and correct the problem (eg if you are importing
 * from csv and cannot just change the contents of a dialog).
 * @author sudar.sam@gmail.com
 *
 */
public class TableAlreadyExistsException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 4973813106577082817L;
  
  public TableAlreadyExistsException() {
    super();
  }

  /**
   * @param detailMessage
   * @param throwable
   */
  public TableAlreadyExistsException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  /**
   * @param detailMessage
   */
  public TableAlreadyExistsException(String detailMessage) {
    super(detailMessage);
  }

  /**
   * @param throwable
   */
  public TableAlreadyExistsException(Throwable throwable) {
    super(throwable);
  }

}
