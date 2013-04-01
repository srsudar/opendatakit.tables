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
package org.opendatakit.tables.Activity.util;

import java.util.ArrayList;

import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.Table;

import android.content.Context;

/**
 * A utility class for functions related to security.
 * 
 * @author hkworden@gmail.com
 */
public class SecurityUtil {
    
    public static final String USER_COLUMN_NAME = "user";
    public static final String PHONENUM_COLUMN_NAME = "phone";
    public static final String PASSWORD_COLUMN_NAME = "pass";
    
    public static boolean couldBeSecurityTable(ArrayList<String> arrayList) {
        return (getSecurityIndices(arrayList) != null);
    }
    
    private static int[] getSecurityIndices(ArrayList<String> arrayList) {
        int[] indices = {-1, -1, -1};
        for (int i = 0; i < arrayList.size(); i++) {
            String column = arrayList.get(i);
            if (column.contains(USER_COLUMN_NAME)) {
                if (indices[0] != -1) {
                    return null;
                }
                indices[0] = i;
            }
            if (column.contains(PHONENUM_COLUMN_NAME)) {
                if (indices[1] != -1) {
                    return null;
                }
                indices[1] = i;
            }
            if (column.contains(PASSWORD_COLUMN_NAME)) {
                if (indices[2] != -1) {
                    return null;
                }
                indices[2] = i;
            }
        }
        for (int index : indices) {
            if (index == -1) {
                return null;
            }
        }
        return indices;
    }
    
    public static boolean isValid(Context context, String tableId,
            String phoneNum, String password) {
        DbHelper dbh = DbHelper.getDbHelper(context);
        DbTable dbt = DbTable.getDbTable(dbh, tableId);
        ArrayList<String> columns = new ArrayList<String>();
        columns.add(DataTableColumns.ROW_ID);
        Table table = dbt.getRaw(columns,
                new String[] {DataTableColumns.SAVED, PHONENUM_COLUMN_NAME, PASSWORD_COLUMN_NAME},
                new String[] {DbTable.SavedStatus.COMPLETE.name(), phoneNum, password}, null);
        return (table.getHeight() > 0);
    }
}
