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
package org.opendatakit.tables.activities;

import java.util.ArrayList;

import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.FooterMode;
import org.opendatakit.tables.data.JoinColumn;
import org.opendatakit.tables.data.KeyValueHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.preferences.SliderPreference;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.MultipleChoiceSettingDialog;
import org.opendatakit.tables.views.SpreadsheetView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

/*
 * Activity that allows users to change column property.
 * Column properties includes abreviations for the column
 * names, SMS-IN, SMS-OUT. Please see ColumnProperty.java
 * for more information about the column property.
 *
 *  @Author : YoonSung Hong (hys235@cs.washington.edu)
 */
public class PropertyManager extends PreferenceActivity {

  public static final String INTENT_KEY_TABLE_ID = "tableId";
  public static final String INTENT_KEY_ELEMENT_KEY = "elementKey";

  // Private Fields
  private String tableId;
  private String elementKey;
  private TableProperties tp;
  private ColumnProperties cp;
  private int colIndex;
  private boolean showingMcDialog;
  private KeyValueStoreHelper columnKVSH;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Set title of activity
    setTitle("ODK Tables > Column Property");

    // Column Name
    this.tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    this.elementKey = getIntent().getStringExtra(INTENT_KEY_ELEMENT_KEY);
    DbHelper dbh = DbHelper.getDbHelper(this, TableFileUtils.ODK_TABLES_APP_NAME);
    tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
        KeyValueStore.Type.ACTIVE);
    this.columnKVSH =
        tp.getKeyValueStoreHelper(ColumnProperties.KVS_PARTITION);
    cp = tp.getColumnByElementKey(elementKey);
    colIndex = tp.getColumnIndex(elementKey);
    showingMcDialog = false;
    loadPreferenceScreen();
  }

  // View that allows users to chane column properties on a specific column.
  private void loadPreferenceScreen() {
    // Preference Screen
    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    // Preference Category
    PreferenceCategory category = new PreferenceCategory(this);
    category.setTitle(cp.getDisplayName());
    root.addPreference(category);

    String displayName = cp.getDisplayName();
    category.addPreference(createEditTextPreference("DISPLAY_NAME",
        "Display Name", "Change display name of column", displayName));

    // SMS Label<EditText>
    // Get the SMS abbreviation on this column.
    String smsLabel = cp.getSmsLabel();
    category.addPreference(createEditTextPreference("SMSLABEL", "SMS Label",
        "Change SMS Label for Column", (smsLabel == null) ? "" : smsLabel));

    // Type<List>
    ArrayList<ColumnType> types = ColumnType.getAllColumnTypes();
    String[] typeLabels = new String[types.size()];
    String[] typeNames = new String[types.size()];
    for ( int i = 0 ; i < types.size(); ++i ) {
      ColumnType t = types.get(i);
      typeLabels[i] = t.label();
      typeNames[i] = t.name();
    }
    String typeLabel = cp.getColumnType().label();
    String typeName = cp.getColumnType().name();
    category.addPreference(createListPreference("TYPE", "Type", typeLabel, typeName, typeLabels, typeNames));

    // SMS-IN<CheckBox>
    category
        .addPreference(createCheckBoxPreference("SMSIN", "Get from Incoming", getSMSIn(elementKey)));

    // SMS-OUT<CheckBox>
    category
        .addPreference(createCheckBoxPreference("SMSOUT", "Put in Outgoing", getSMSOut(elementKey)));

    // Footer Mode<List>
    FooterMode[] footerModes = FooterMode.values();
    String footerModeLabels[] = new String[footerModes.length];
    String footerModeNames[] = new String[footerModes.length];
    for ( int i = 0 ; i < footerModes.length ; ++i ) {
      footerModeLabels[i] = footerModes[i].toString();
      footerModeNames[i] = footerModes[i].name();
    }
    String footerModeLabel = cp.getFooterMode().toString();
    String footerModeName = cp.getFooterMode().name();

    category.addPreference(createListPreference("FOOTER", "Footer Mode", footerModeLabel, footerModeName,
        footerModeLabels, footerModeNames));

    SliderPreference colWidthPref = new SliderPreference(this);
    colWidthPref.setTitle("Column Width");
    colWidthPref.setDialogTitle("Change Column Width");
    colWidthPref.setMaxValue(500);
    final KeyValueHelper aspectHelper =
        columnKVSH.getAspectHelper(elementKey);
    Integer savedColumnWidth =
        aspectHelper.getInteger(SpreadsheetView.KEY_COLUMN_WIDTH);
    if (savedColumnWidth == null) {
      savedColumnWidth = SpreadsheetView.DEFAULT_COL_WIDTH;
    }
    colWidthPref.setValue(savedColumnWidth);
    colWidthPref.setOnPreferenceChangeListener(
        new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference,
          Object newValue) {
        int width = (Integer) newValue;
        aspectHelper.setInteger(SpreadsheetView.KEY_COLUMN_WIDTH, width);
        return true;
      }
    });
    category.addPreference(colWidthPref);

    category.addPreference(new DisplayPreferencesDialogPreference(this));

    if (cp.getColumnType() == ColumnType.MC_OPTIONS) {
      showingMcDialog = true;
      category.addPreference(new McOptionSettingsDialogPreference(this));
    } else if (cp.getColumnType() == ColumnType.TABLE_JOIN) {
      // first handle what happens if it's null.
      ArrayList<JoinColumn> joins = cp.getJoins();
      if (joins == null) {
        joins = new ArrayList<JoinColumn>();
        cp.setJoins(joins);
      }
      String joinTableId = (joins != null && joins.size() == 1) ? joins.get(0).getTableId() : null;
      TableProperties[] tps = TableProperties.getTablePropertiesForAll(
          DbHelper.getDbHelper(this, TableFileUtils.ODK_TABLES_APP_NAME), KeyValueStore.Type.ACTIVE);
      TableProperties selectedTp = null;
      String[] tableIds = new String[tps.length];
      String selectedTableId = tableIds[0] = null;
      String[] tableNames = new String[tps.length];
      String selectedDisplayName = tableNames[0] = "Choose a Table";
      int index = 1;
      for (TableProperties tp : tps) {
        if (tp.getTableId().equals(tableId)) {
          continue;
        }
        tableIds[index] = tp.getTableId();
        tableNames[index] = tp.getDbTableName();
        if (tp.getTableId().equals(joinTableId)) {
          selectedTp = tp;
          selectedTableId = tp.getTableId();
          selectedDisplayName = tp.getDisplayName();
        }
        index++;
      }
      category.addPreference(createListPreference("JOIN_TABLE", "Join Table", selectedDisplayName,
          selectedTableId, tableNames, tableIds));
      if (selectedTp != null) {
        // TODO: resolve how joins work
        String joinColName = (joins != null && joins.size() == 1) ? joins.get(0).getElementKey() : null;
        int numberOfDisplayColumns = selectedTp.getNumberOfDisplayColumns();
        String[] colDbNames = new String[numberOfDisplayColumns + 1];
        String selectedDbName = colDbNames[0] = null;
        String[] colDisplayNames = new String[numberOfDisplayColumns + 1];
        String selectedColDisplayName = colDisplayNames[0] = "Choose a Column";
        for (int i = 0; i < numberOfDisplayColumns; i++) {
          ColumnProperties cp = selectedTp.getColumnByIndex(i);
          String elementKey = cp.getElementKey();
          String colDisplayName = cp.getDisplayName();
          colDbNames[i + 1] = elementKey;
          colDisplayNames[i + 1] = colDisplayName;
          if ((joinColName != null) && elementKey.equals(joinColName)) {
            selectedDbName = elementKey;
            selectedColDisplayName = colDisplayName;
          }
        }
        category.addPreference(createListPreference("JOIN_COLUMN", "Join Column",
            selectedColDisplayName, selectedDbName, colDisplayNames, colDbNames));
      }
    }

    // Set
    // getListView().setBackgroundColor(Color.TRANSPARENT);
    // getListView().setCacheColorHint(Color.TRANSPARENT);
    // getListView().setBackgroundColor(Color.rgb(255, 255, 255));
    setPreferenceScreen(root);

  }

  // Check if this is SMS-IN column.
  private boolean getSMSIn(String colName) {
    return cp.getSmsIn();
  }

  // Check if this is SMS-OUT column.
  private boolean getSMSOut(String colName) {
    return cp.getSmsOut();
  }


  // If any of fields change, direct the request to appropriate actions.
  public void onFieldChangeRouter(String key, String newVal) {
    // Get corresponding preference
    Preference pref = findPreference(key);

    // Routing
    if (key.equals("SMSLABEL")) {
      String boxVal = getEditBoxContent(pref);
      if ( !boxVal.equals(cp.getSmsLabel()) ) {
        cp.setSmsLabel(boxVal);
      }
    } else if (key.equals("TYPE")) {
      for (ColumnType t : ColumnType.getAllColumnTypes()) {
        if (t.name().equals(newVal)) {
          cp.setColumnType(tp, t);
          if ((t == ColumnType.MC_OPTIONS) && !showingMcDialog) {
            loadPreferenceScreen();
          } else if (t == ColumnType.TABLE_JOIN) {
            loadPreferenceScreen();
          }
          break;
        }
      }
    } else if (key.equals("SMSIN")) {
      boolean checkVal = getCheckBoxContent(pref);
      if ( cp.getSmsIn() != checkVal ) {
        cp.setSmsIn(checkVal);
      }
    } else if (key.equals("SMSOUT")) {
      boolean checkVal = getCheckBoxContent(pref);
      if ( cp.getSmsOut() != checkVal ) {
        cp.setSmsOut(checkVal);
      }
    } else if (key.equals("FOOTER")) {
      FooterMode[] footerModes = FooterMode.values();
      for (int i = 0; i < footerModes.length; i++) {
        if (footerModes[i].name().equals(newVal)) {
          if ( !cp.getFooterMode().equals(footerModes[i]) ) {
            cp.setFooterMode(footerModes[i]);
          }
          break;
        }
      }
    } else if (key.equals("JOIN_TABLE")) {
      // TODO: resolve the ifs here for joins
      ArrayList<JoinColumn> oldJoins = cp.getJoins();
      if ( oldJoins == null ) {
        oldJoins = new ArrayList<JoinColumn>();
      }
      if ( oldJoins.size() == 0 ) {
        oldJoins.add( new JoinColumn(newVal, JoinColumn.DEFAULT_NOT_SET_VALUE));
      } else {
        JoinColumn jc = oldJoins.get(0);
        jc.setTableId(newVal);
      }
      cp.setJoins(oldJoins);
    } else if (key.equals("JOIN_COLUMN")) {
      ArrayList<JoinColumn> oldJoins = cp.getJoins();
      if ( oldJoins == null ) {
        oldJoins = new ArrayList<JoinColumn>();
      }
      if ( oldJoins.size() == 0 ) {
        oldJoins.add( new JoinColumn(JoinColumn.DEFAULT_NOT_SET_VALUE, newVal));
      } else {
        JoinColumn jc = oldJoins.get(0);
        jc.setElementKey(newVal);
      }
      cp.setJoins(oldJoins);
    } else if (key.equals("DISPLAY_NAME")) {
      if ( !newVal.equals(cp.getDisplayName()) ) {
        cp.setDisplayName(tp.createDisplayName(newVal));
      }
    }

    // Refresh
    getPreferenceScreen().removeAll();
    loadPreferenceScreen();
  }


  @Override
  public void onBackPressed() {
    super.onBackPressed();
  }

  // What value is in this edit box?
  private String getEditBoxContent(Preference pref) {
    EditTextPreference etp = (EditTextPreference) pref;
    return etp.getEditText().getText().toString();
  }

  // What value is in this check box?
  private boolean getCheckBoxContent(Preference pref) {
    CheckBoxPreference cbp = (CheckBoxPreference) pref;
    // Reverse original when checked/unchecked.
    if (cbp.isChecked()) {
      // result = "0";
      return false;
    } else {
      // result = "1";
      return true;
    }
  }

  // Create a check box with the specified specifications.
  private CheckBoxPreference createCheckBoxPreference(String key, String title, boolean val) {
    // Create a check box & configure
    CheckBoxPreference cbp = new CheckBoxPreference(this);
    cbp.setKey(key);
    cbp.setTitle(title);
    cbp.setPersistent(false);
    cbp.setSummaryOn("True");
    cbp.setSummaryOff("False");
    cbp.setChecked(val);

    // Reaction if the check box gets changes
    cbp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Ask the router what to do
        onFieldChangeRouter(preference.getKey(), null);
        return false;
      }
    });

    return cbp;
  }

  // Create edit box with the specified specifications.
  private EditTextPreference createEditTextPreference(String key,
      String textPreferenceTitle, String dialogTitle,
      String valueString) {
    // Create a edit box & configure
    EditTextPreference etp = new EditTextPreference(this);
    etp.setKey(key);
    etp.setTitle(textPreferenceTitle);
    etp.setPersistent(false);
    etp.setDialogTitle(dialogTitle);
    etp.setSummary(valueString);
    etp.setText(valueString);

    // Reaction if the edit box changes
    etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Ask the router what to do
//        onFieldChangeRouter(preference.getKey(), null);
        onFieldChangeRouter(preference.getKey(), (String) newValue);
        return false;
      }
    });

    return etp;
  }

  private ListPreference createListPreference(String key, String title, String labelText, String nameText,
      String[] labelsList, String[] namesList) {
    // Create a list preference & configure
    ListPreference lp = new ListPreference(this);
    lp.setEntries(labelsList);
    lp.setEntryValues(namesList);
    lp.setKey(key);
    lp.setTitle(title);
    lp.setPersistent(false);
    lp.setSummary(labelText);
    lp.setValue(nameText);

    // When an option chosen by the user
    lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Ask the router what to do
        onFieldChangeRouter(preference.getKey(), (String) newValue);
        return false;
      }
    });

    return lp;
  }

  private class DisplayPreferencesDialogPreference extends Preference {

    public DisplayPreferencesDialogPreference(Context context) {
      super(context);
      setTitle("Edit Column Color Rules");
    }

    @Override
    protected void onClick() {
      Intent i = new Intent(PropertyManager.this,
          ColorRuleManagerActivity.class);
      i.putExtra(ColorRuleManagerActivity.INTENT_KEY_ELEMENT_KEY, elementKey);
      i.putExtra(ColorRuleManagerActivity.INTENT_KEY_TABLE_ID,
          tp.getTableId());
      i.putExtra(ColorRuleManagerActivity.INTENT_KEY_RULE_GROUP_TYPE,
          ColorRuleGroup.Type.COLUMN.name());
      startActivity(i);
    }
  }

  private class McOptionSettingsDialogPreference extends Preference {

    private final MultipleChoiceSettingDialog dialog;

    public McOptionSettingsDialogPreference(Context context) {
      super(context);
      dialog = new MultipleChoiceSettingDialog(PropertyManager.this, cp);
      setTitle("Multiple Choice Settings");
    }

    @Override
    protected void onClick() {
      dialog.show();
    }
  }
}