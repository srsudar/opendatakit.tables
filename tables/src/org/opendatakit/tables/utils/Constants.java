/*
 * Copyright (C) 2013 University of Washington
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
package org.opendatakit.tables.utils;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment;

import android.graphics.Color;

/**
 * 
 * @author sudar.sam@gmail.com
 * @author unknown
 *
 */
public class Constants {
  
  public static final int DEFAULT_TEXT_COLOR = Color.BLACK;
  public static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;
  
  public static class HTML {
    /**
     * The default HTML to be displayed if no file name has been set.
     */
    public static final String NO_FILE_NAME = "<html><body>"
        + "<p>No filename has been specified.</p>" + "</body></html>";
  }
  
  public static class MimeTypes {
    public static final String TEXT_HTML = "text/html";
  }
  

  
  /**
   * Intent keys to be used to communicate between activities.
   * @author sudar.sam@gmail.com
   *
   */
  public static class IntentKeys {
    public static final String TABLE_ID = "tableId";
    public static final String APP_NAME = "appName";
    /**
     * Tells {@link TableDisplayActivity} what time of view it should be
     * displaying.
     */
    public static final String TABLE_DISPLAY_VIEW_TYPE = 
        "tableDisplayViewType";
    public static final String FILE_NAME = "filename";
    public static final String ROW_ID = "rowId";
    /**
     * Key to the where clause if this list view is to be opened with a more
     * complex query than permissible by the simple query object. Must conform
     * to the expectations of {@link DbTable#rawSqlQuery} and
     * {@link CustomView$Control#queryWithSql}.
     *
     * @see INTENT_KEY_SQL_SELECTION_ARGS
     */
    public static final String SQL_WHERE = "sqlWhereClause";
    /**
     * An array of strings for restricting the rows displayed in the table.
     *
     * @see INTENT_KEY_SQL_WHERE
     */
    public static final String SQL_SELECTION_ARGS = "sqlSelectionArgs";
    /**
     * An array of strings giving the group by columns.
     * What was formerly 'overview' mode is a non-null groupBy list.
     */
    public static final String SQL_GROUP_BY_ARGS = "sqlGroupByArgs";
    /**
     * The having clause, if present
     */
    public static final String SQL_HAVING = "sqlHavingClause";
    /**
     * The order by column. NOTE: restricted to a single column
     */
    public static final String SQL_ORDER_BY_ELEMENT_KEY = "sqlOrderByElementKey";
    /**
     * The order by direction (ASC or DESC)
     */
    public static final String SQL_ORDER_BY_DIRECTION = "sqlOrderByDirection";
  }
  
  public static class FragmentTags {
    /** Tag for {@link TopLevelTableMenuFragment} */
    public static final String TABLE_MENU = "tagTableMenuFragment";
    public static final String SPREADSHEET = "tagSpreadsheetFragment";
    public static final String MAP = "tagMapFragment";
    public static final String LIST = "tagListFragment";
    public static final String GRAPH = "tagGraphFragment";
    public static final String MAP_INNER_MAP = "tagInnerMapFragment";
    public static final String MAP_LIST = "tagMapListFragment";
    public static final String DETAIL_FRAGMENT = "tagDetailFragment";
    public static final String INITIALIZE_TASK_DIALOG = "tagInitializeTask";
    public static final String TABLE_MANAGER = "tagFragmentManager";
    public static final String WEB_FRAGMENT = "tagWebFragment";
  }

  public static class PreferenceKeys {
    
    /**
     * Preference keys for table-level preferences.
     * @author sudar.sam@gmail.com
     *
     */
    public static class Table {
      public static String DISPLAY_NAME = "table_pref_display_name";
      public static String TABLE_ID = "table_pref_table_id";
      public static String DEFAULT_VIEW_TYPE = "table_pref_default_view_type";
      public static String DEFAULT_FORM = "table_pref_default_form";
      public static String TABLE_COLOR_RULES = "table_pref_table_color_rules";
      public static String STATUS_COLOR_RULES = 
          "table_pref_status_column_color_rules";
      public static String MAP_COLOR_RULE = "table_pref_map_color_rule";
      public static String LIST_FILE = "table_pref_list_file";
      public static String DETAIL_FILE = "table_pref_detail_file";
      public static String GRAPH_MANAGER = "table_pref_graph_view_manager";
    }
  }
  
  public static class RequestCodes {
    public static final int DISPLAY_VIEW = 1;
    public static final int CHOOSE_DETAIL_FILE = 2;
    public static final int CHOOSE_LIST_FILE = 3;
    public static final int CHOOSE_MAP_FILE = 4;
    /** A generic code for now. Can refactor to make more specific if needed.*/
    public static final int LAUNCH_VIEW = 5;
    public static final int LAUNCH_DISPLAY_PREFS = 6;
    public static final int LAUNCH_IMPORT_EXPORT = 7;
    public static final int LAUNCH_SYNC = 8;
    public static final int LAUNCH_TABLE_MANAGER = 9;
    /** For launching an HTML file not associated with a table. */
    public static final int LAUNCH_WEB_VIEW = 10;
  }
  
  /**
   * The names of the JavaScript interfaces that are attached to the window
   * object.
   * @author sudar.sam@gmail.com
   *
   */
  public static class JavaScriptHandles {
    public static final String CONTROL = "control";
    public static final String DATA = "data";
  }

}
