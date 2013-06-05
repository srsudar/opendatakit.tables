package org.opendatakit.tables.activities;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.graphs.GraphDisplayActivity;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.Query.Constraint;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewType;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.fragments.ITableFragment;
import org.opendatakit.tables.fragments.TableMapFragment;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.views.CellValueView;
import org.opendatakit.tables.views.ClearableEditText;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

/**
 * Base activity for all fragments that display information about a database.
 * Deals with maintaining the data and the actionbar.
 *
 * @author Chris Gelon (cgelon)
 */
public class TableActivity extends SherlockFragmentActivity {

  // / Static Strings ///
  public static final String INTENT_KEY_TABLE_ID = "tableId";
  public static final String INTENT_KEY_SEARCH = "search";
  public static final String INTENT_KEY_SEARCH_STACK = "searchStack";
  public static final String INTENT_KEY_IS_OVERVIEW = "isOverview";
  /** Key value store key for table activity. */
  public static final String KVS_PARTITION = "TableActivity";

  public static final int VIEW_ID_SEARCH_FIELD = 0;
  public static final int VIEW_ID_SEARCH_BUTTON = 1;

  private static final int MENU_ITEM_ID_SEARCH_BUTTON = 0;
  private static final int MENU_ITEM_ID_VIEW_TYPE_SUBMENU = 1;
  // The add row button serves as an edit row button in DetailDisplayActivity
  public static final int MENU_ITEM_ID_ADD_ROW_BUTTON = 2;
  private static final int MENU_ITEM_ID_SETTINGS_SUBMENU = 3;
  // Display preferences is used differently in SpreadsheetDisplayActivity
  public static final int MENU_ITEM_ID_DISPLAY_PREFERENCES = 4;
  private static final int MENU_ITEM_ID_OPEN_TABLE_PROPERTIES = 5;
  private static final int MENU_ITEM_ID_OPEN_COLUMN_MANAGER = 6;
  private static final int MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER = 7;
  static final int FIRST_FREE_MENU_ITEM_ID = 8;

  // Return codes from different activities.
  private static final int RCODE_TABLE_PROPERTIES_MANAGER = 0;
  private static final int RCODE_COLUMN_MANAGER = 1;
  private static final int RCODE_ODKCOLLECT_ADD_ROW = 2;
  private static final int RCODE_ODKCOLLECT_EDIT_ROW = 3;
  private static final int RCODE_LIST_VIEW_MANAGER = 4;

  private static final String COLLECT_FORMS_URI_STRING = "content://org.odk.collect.android.provider.odk.forms/forms";
  private static final Uri ODKCOLLECT_FORMS_CONTENT_URI = Uri.parse(COLLECT_FORMS_URI_STRING);
  private static final String COLLECT_INSTANCES_URI_STRING = "content://org.odk.collect.android.provider.odk.instances/instances";
  private static final Uri COLLECT_INSTANCES_CONTENT_URI = Uri.parse(COLLECT_INSTANCES_URI_STRING);

  /** The current fragment being displayed. */
  private ITableFragment mCurrentFragment;

  /** The fragment that contains map information. */
  private TableMapFragment mMapFragment;

  /** Table that represents all of the data in the query. */
  private UserTable mTable;

  public UserTable getTable() {
    return mTable;
  }

  /** The properties of the user table. */
  private TableProperties mTableProperties;

  public TableProperties getTableProperties() {
    return mTableProperties;
  }

  private Query mQuery;

  private String mRowId;

  private DataUtil mDataUtil;
  private DataManager mDataManager;
  private DbTable mDbTable;
  private Stack<String> mSearchText;
  private boolean mIsOverview;
  private Activity mActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.standard_table_layout);

    mActivity = this;

    // Set up the data utility.
    mDataUtil = DataUtil.getDefaultDataUtil();

    // Find the table id.
    String tableId = getIntent().getExtras().getString(INTENT_KEY_TABLE_ID);
    if (tableId == null) {
      throw new RuntimeException("Table id was not passed in through the bundle.");
    }

    // Add the search texts.
    mSearchText = new Stack<String>();
    if (getIntent().getExtras().containsKey(INTENT_KEY_SEARCH_STACK)) {
      String[] searchValues = getIntent().getExtras().getStringArray(INTENT_KEY_SEARCH_STACK);
      for (String searchValue : searchValues) {
        mSearchText.add(searchValue);
      }
    } else {
      String initialSearchText = getIntent().getExtras().getString(INTENT_KEY_SEARCH);
      mSearchText.add((initialSearchText == null) ? "" : initialSearchText);
    }

    mIsOverview = getIntent().getExtras().getBoolean(INTENT_KEY_IS_OVERVIEW, false);

    // Initialize data objects.
    mDataManager = new DataManager(DbHelper.getDbHelper(this));
    mTableProperties = mDataManager.getTableProperties(tableId, KeyValueStore.Type.ACTIVE);
    mDbTable = mDataManager.getDbTable(tableId);
    mQuery = new Query(mDataManager.getAllTableProperties(KeyValueStore.Type.ACTIVE),
        mTableProperties);

    // Initialize layout fields.
    setSearchFieldText(mSearchText.peek());
    setInfoBarText("Table: " + mTableProperties.getDisplayName());

    mQuery.clear();
    mQuery.loadFromUserQuery(mSearchText.peek());

    mTable = mIsOverview ? mDbTable.getUserOverviewTable(mQuery) : mDbTable.getUserTable(mQuery);

    // Create the map fragment.
    if (savedInstanceState == null) {
      mMapFragment = new TableMapFragment();
      getSupportFragmentManager().beginTransaction().add(R.id.main, mMapFragment).commit();
    } else {
      mMapFragment = (TableMapFragment) getSupportFragmentManager().findFragmentById(R.id.main);
    }

    // Set the current fragment.
    mCurrentFragment = mMapFragment;
  }

  public void init() {
    refreshDbTable();
    mQuery = new Query(mDataManager.getAllTableProperties(KeyValueStore.Type.ACTIVE),
        mTableProperties);
    mQuery.clear();
    mQuery.loadFromUserQuery(mSearchText.peek());
    mTable = mIsOverview ? mDbTable.getUserOverviewTable(mQuery) : mDbTable.getUserTable(mQuery);
    mCurrentFragment.init();
  }

  public void onSearchButtonClick(View v) {
    // when you click the search button, save that query.
    KeyValueStoreHelper kvsh = mTableProperties
        .getKeyValueStoreHelper(TableProperties.KVS_PARTITION);
    kvsh.setString(TableProperties.KEY_CURRENT_QUERY, getSearchFieldText());
    mCurrentFragment.onSearch();
  }

  /**
   * @return The text in the search field.
   */
  public String getSearchFieldText() {
    return ((ClearableEditText) findViewById(R.id.search_field)).getEditText().getText().toString();
  }

  /**
   * Set the text in the search field.
   */
  public void setSearchFieldText(String text) {
    ((ClearableEditText) findViewById(R.id.search_field)).getEditText().setText(text);
  }

  /**
   * Appends the text to the search box.
   */
  public void appendToSearchBoxText(String text) {
    setSearchFieldText((getSearchFieldText() + text).trim());
  }

  /**
   * Set the text in the info bar.
   */
  public void setInfoBarText(String text) {
    ((TextView) findViewById(R.id.info_bar)).setText(text);
  }

  /**
   * @return The text in the info bar.
   */
  public String getInfoBarText() {
    return ((TextView) findViewById(R.id.info_bar)).getText().toString();
  }

  /**
   * Update the dbTable that Controller is monitoring. This should be called
   * only if there is no way to update the dbTable held by the Controller if a
   * change happens outside of the Controller's realm of control. For instance,
   * changing a column display name in PropertyManager does not get updated to
   * the dbTable without calling this method. This is a messy way of doing
   * things, and a refactor should probably end up fixing this.
   */
  void refreshDbTable() {
    mDbTable = mDataManager.getDbTable(mTableProperties.getTableId());
  }

  /**
   * @return DbTable this data table
   */
  DbTable getDbTable() {
    mTableProperties.refreshColumns();
    return mDbTable;
  }

  /**
   * @return True if this is an overview type, false if this is collection view
   *         type
   */
  boolean getIsOverview() {
    return mIsOverview;
  }

  /**
   * @return String text currently in the search bar
   */
  String getSearchText() {
    return mSearchText.peek();
  }

  /**
   * True if the x and y are in the search box, false otherwise.
   */
  boolean isInSearchBox(int x, int y) {
    y -= findViewById(R.id.control_wrap).getHeight();
    Rect bounds = new Rect();
    findViewById(R.id.search_field).getHitRect(bounds);
    return ((bounds.left <= x) && (bounds.right >= x) && (bounds.top <= y) && (bounds.bottom >= y));
  }

  /**
   * This is used to invert the color of the search box. The boolean parameter
   * specifies whether or not the color should be inverted or returned to
   * normal.
   * <p>
   * The inversion values are not tied to any particular theme, but are set
   * using the ActionBarSherlock themes. These need to change if the app themes
   * are changed.
   *
   * @param invert
   */
  void invertSearchBoxColor(boolean invert) {
    ClearableEditText searchField = (ClearableEditText) findViewById(R.id.search_field);
    if (invert) {
      searchField.setBackgroundResource(R.color.abs__background_holo_light);
      searchField.getEditText().setTextColor(
          searchField.getContext().getResources().getColor(R.color.abs__background_holo_dark));
      searchField.getClearButton().setBackgroundResource(R.drawable.content_remove_dark);
    } else {
      searchField.setBackgroundResource(R.color.abs__background_holo_dark);
      searchField.getEditText().setTextColor(
          searchField.getContext().getResources().getColor(R.color.abs__background_holo_light));
      searchField.getClearButton().setBackgroundResource(R.drawable.content_remove_light);
    }
  }

  void recordSearch() {
    mSearchText.add(getSearchFieldText());
  }

  public void onBackPressed() {
    if (mSearchText.size() == 1) {
      finish();
    } else {
      mSearchText.pop();
      setSearchFieldText(mSearchText.peek());
    }
  }

  /**
   * This should launch Collect to edit the data for the row. If there is a
   * custom form defined for the table, its info should be loaded in params. If
   * the formId in params is null, then the default form is generated, which is
   * just every column with its own entry field on a single screen.
   *
   * @param table
   * @param rowNum
   * @param params
   */
  void editRow(UserTable table, int rowNum, CollectFormParameters params) {
    Intent intent = null;
    intent = getIntentForOdkCollectEditRow(table, rowNum, params);
    if (intent != null) {
      mRowId = table.getRowId(rowNum);
      startActivityForResult(intent, RCODE_ODKCOLLECT_EDIT_ROW);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
    case RCODE_TABLE_PROPERTIES_MANAGER:
      handleTablePropertiesManagerReturn();
      break;
    case RCODE_COLUMN_MANAGER:
      handleColumnManagerReturn();
      break;
    case RCODE_ODKCOLLECT_ADD_ROW:
      handleOdkCollectAddReturn(resultCode, data);
      break;
    case RCODE_ODKCOLLECT_EDIT_ROW:
      handleOdkCollectEditReturn(resultCode, data);
      break;
    case RCODE_LIST_VIEW_MANAGER:
      handleListViewManagerReturn();
      break;
    default:
      break;
    }
    if (resultCode == SherlockActivity.RESULT_OK) {
      init();
    }
  }

  private void handleListViewManagerReturn() {
    mTableProperties = mDataManager.getTableProperties(mTableProperties.getTableId(),
        KeyValueStore.Type.ACTIVE);
    mDbTable = mDataManager.getDbTable(mTableProperties.getTableId());
  }

  private void handleTablePropertiesManagerReturn() {
    TableViewType oldViewType = mTableProperties.getCurrentViewType();
    mTableProperties = mDataManager.getTableProperties(mTableProperties.getTableId(),
        KeyValueStore.Type.ACTIVE);
    mDbTable = mDataManager.getDbTable(mTableProperties.getTableId());
    if (oldViewType == mTableProperties.getCurrentViewType()) {
      init();
    } else {
      launchTableActivity(this, mTableProperties, mSearchText, mIsOverview);
    }
  }

  private void handleColumnManagerReturn() {
    mTableProperties = mDataManager.getTableProperties(mTableProperties.getTableId(),
        KeyValueStore.Type.ACTIVE);
    mDbTable = mDataManager.getDbTable(mTableProperties.getTableId());
  }

  void deleteRow(String rowId) {
    mDbTable.markDeleted(rowId);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Set the app icon as an action to go home.
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle("");

    // Search
    MenuItem searchItem = menu.add(Menu.NONE, MENU_ITEM_ID_SEARCH_BUTTON, Menu.NONE, "Search");
    searchItem.setIcon(R.drawable.ic_action_search);
    searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    searchItem.setEnabled(true);

    // View type submenu
    // -determine the possible view types
    final TableViewType[] viewTypes = mTableProperties.getPossibleViewTypes();
    // -build a checkable submenu to select the view type
    SubMenu viewTypeSubMenu = menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_VIEW_TYPE_SUBMENU, Menu.NONE,
        "ViewType");
    MenuItem viewType = viewTypeSubMenu.getItem();
    viewType.setIcon(R.drawable.view);
    viewType.setEnabled(true);
    viewType.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    // This will be the name of the default list view, which if exists
    // means we should display the list view as an option.
    KeyValueStoreHelper kvsh = mTableProperties
        .getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
    String nameOfView = kvsh.getString(ListDisplayActivity.KEY_LIST_VIEW_NAME);
    for (int i = 0; i < viewTypes.length; i++) {
      MenuItem item = viewTypeSubMenu.add(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, viewTypes[i].getId(), i,
          viewTypes[i].name());
      // Mark the current viewType as selected.
      if (mTableProperties.getCurrentViewType() == viewTypes[i]) {
        item.setChecked(true);
      }
      // Disable list view if no file is specified
      if (viewTypes[i] == TableViewType.List && nameOfView == null) {
        item.setEnabled(false);
      }
    }

    viewTypeSubMenu.setGroupCheckable(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, true, true);

    // Add Row
    MenuItem addItem = menu.add(Menu.NONE, MENU_ITEM_ID_ADD_ROW_BUTTON, Menu.NONE, "Add Row")
        .setEnabled(true);
    addItem.setIcon(R.drawable.content_new);
    addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    // Settings submenu
    SubMenu settings = menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_SETTINGS_SUBMENU, Menu.NONE,
        "Settings");
    MenuItem settingsItem = settings.getItem();
    settingsItem.setIcon(R.drawable.settings_icon2);
    settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    MenuItem display = settings.add(Menu.NONE, MENU_ITEM_ID_DISPLAY_PREFERENCES, Menu.NONE,
        "Display Preferences").setEnabled(true);
    // Always disable DisplayPreferences if it is currently in list view
    if (mTableProperties.getCurrentViewType() == TableViewType.List) {
      display.setEnabled(false);
    }
    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_TABLE_PROPERTIES, Menu.NONE, "Table Properties")
        .setEnabled(true);
    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_COLUMN_MANAGER, Menu.NONE, "Column Manager")
        .setEnabled(true);
    // Now an option for editing list views.
    MenuItem manageListViews = settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER,
        Menu.NONE, "List View Manager").setEnabled(true);
    // TODO: add manageListViews to the menu?
    return true;
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    // If the item is part of the sub-menu for view type, set the view type
    // with its itemId else, handle accordingly.
    if (item.getGroupId() == MENU_ITEM_ID_VIEW_TYPE_SUBMENU) {
      mTableProperties.setCurrentViewType(TableViewType.getViewTypeFromId(item.getItemId()));
      launchTableActivity(this, mTableProperties, mSearchText, mIsOverview);
      return true;
    } else {
      switch (item.getItemId()) {
      case MENU_ITEM_ID_SEARCH_BUTTON:
        if (getControlWrapVisibility() == View.GONE)
          setControlWrapVisiblity(View.VISIBLE);
        else
          setControlWrapVisiblity(View.GONE);
        return true;
      case MENU_ITEM_ID_VIEW_TYPE_SUBMENU:
        return true;
      case MENU_ITEM_ID_ADD_ROW_BUTTON:
        if (!getSearchText().equals("")) {
          addRow(getMapFromLimitedQuery());
        } else {
          addRow(null);
        }
        return true;
      case MENU_ITEM_ID_SETTINGS_SUBMENU:
        return true;
      case MENU_ITEM_ID_DISPLAY_PREFERENCES:
        Intent k = new Intent(this, DisplayPrefsActivity.class);
        k.putExtra(DisplayPrefsActivity.INTENT_KEY_TABLE_ID, mTableProperties.getTableId());
        startActivity(k);
        return true;
      case MENU_ITEM_ID_OPEN_TABLE_PROPERTIES:
        Intent tablePropertiesIntent = new Intent(this, TablePropertiesManager.class);
        tablePropertiesIntent.putExtra(TablePropertiesManager.INTENT_KEY_TABLE_ID,
            mTableProperties.getTableId());
        startActivityForResult(tablePropertiesIntent, RCODE_TABLE_PROPERTIES_MANAGER);
        return true;
      case MENU_ITEM_ID_OPEN_COLUMN_MANAGER:
        Intent columnManagerIntent = new Intent(this, ColumnManager.class);
        columnManagerIntent.putExtra(ColumnManager.INTENT_KEY_TABLE_ID,
            mTableProperties.getTableId());
        startActivityForResult(columnManagerIntent, RCODE_COLUMN_MANAGER);
        return true;
      case MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER:
        Intent listViewManagerIntent = new Intent(this, ListViewManager.class);
        listViewManagerIntent.putExtra(ListViewManager.INTENT_KEY_TABLE_ID,
            mTableProperties.getTableId());
        startActivityForResult(listViewManagerIntent, RCODE_LIST_VIEW_MANAGER);
        return true;
      case android.R.id.home:
        Intent tableManagerIntent = new Intent(this, TableManager.class);
        // Add this flag so that you don't back from TableManager back
        // into the table.
        tableManagerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(tableManagerIntent);
        return true;
      default:
        return false;
      }
    }
  }

  /**
   * Adds a row to the table.
   *
   * @param elementNameToValue
   *          Element names to values to prepopulate the row with before
   *          launching the activity.
   */
  public void addRow(Map<String, String> elementNameToValue) {
    CollectFormParameters params = CollectUtil.CollectFormParameters
        .constructCollectFormParameters(mTableProperties);
    // Try to construct the values currently in the search bar to
    // prepopulate with the form. We're going to ignore joins. This
    // means that if there IS a join column, we'll throw an error!!!
    // So be careful.
    Intent intentAddRow = getIntentForOdkCollectAddRow(params, elementNameToValue);
    if (intentAddRow != null) {
      startActivityForResult(intentAddRow, RCODE_ODKCOLLECT_ADD_ROW);
    }
  }

  private int getControlWrapVisibility() {
    return findViewById(R.id.control_wrap).getVisibility();
  }

  private void setControlWrapVisiblity(int visibility) {
    findViewById(R.id.control_wrap).setVisibility(visibility);
  }

  /**
   * The idea here is that we might want to edit a row of the table using a
   * pre-set Collect form. This form would be user-defined and would be a more
   * user-friendly thing that would display only the pertinent information for a
   * particular user.
   */
  /*
   * This is a move away from the general "odk add row" usage that is going on
   * when no row is defined. As I understand it, the new case will work as
   * follows.
   *
   * There exits an "tableEditRow" form for a particular table. This form, as I
   * understand it, must exist both in the tables directory, as well as in
   * Collect so that Collect can launch it with an Intent.
   *
   * You then also construct a "values" sort of file, that is the data from the
   * database that will pre-populate the fields. Mitch referred to something
   * like this as the "instance" file.
   *
   * Once you have both of these files, the form and the data, you insert the
   * data into the form. When you launch the form, it is then pre-populated with
   * data from the database.
   *
   * In order to make this work, the form must exist both within the places
   * Collect knows to look, as well as in the Tables folder. You also must know
   * the:
   *
   * collectFormVersion collectFormId collectXFormRootElement (default to
   * "data")
   *
   * These will most likely exist as keys in the key value store. They must
   * match the form.
   *
   * Other things needed will be:
   *
   * instanceFilePath // I think the filepath with all the values displayName //
   * just text, eg a row ID formId // the same thing as collectFormId?
   * formVersion status // either INCOMPLETE or COMPLETE
   *
   * Examples for how this is done in Collect can be found in the Collect code
   * in org.odk.collect.android.tasks.SaveToDiskTask.java, in the
   * updateInstanceDatabase() method.
   */
  public Intent getIntentForOdkCollectEditRow(UserTable table, int rowNum,
      CollectFormParameters params) {
    // Check if there is a custom form. If there is not, we want to delete
    // the old form and write the new form.
    if (!params.isCustom()) {
      boolean formIsReady = CollectUtil.deleteWriteAndInsertFormIntoCollect(getContentResolver(),
          params, mTableProperties);
      if (!formIsReady) {
        Log.e(/* TODO: TAG! */"Activity", "could not delete, write, or insert a generated form");
        return null;
      }
    }
    Map<String, String> elementNameToValue = new HashMap<String, String>();
    for (ColumnProperties cp : mTableProperties.getColumns()) {
      String value = table.getData(rowNum, mTableProperties.getColumnIndex(cp.getElementName()));
      elementNameToValue.put(cp.getElementName(), value);
    }
    boolean writeDataSuccessful = CollectUtil.writeRowDataToBeEdited(elementNameToValue,
        mTableProperties, params);
    if (!writeDataSuccessful) {
      Log.e(/* TODO: TAG! */"Activity", "could not write instance file successfully!");
    }
    Uri insertUri = CollectUtil.getUriForInsertedData(params, getContentResolver());
    // Copied the below from getIntentForOdkCollectEditRow().
    Intent intent = new Intent();
    intent.setComponent(new ComponentName("org.odk.collect.android",
        "org.odk.collect.android.activities.FormEntryActivity"));
    intent.setAction(Intent.ACTION_EDIT);
    intent.setData(insertUri);
    return intent;
  }

  /**
   * Generate the Intent to add a row using Collect. For safety, the params
   * object, particularly it's isCustom field, determines exactly which action
   * is taken. If a custom form is defined, it launches that form. If there is
   * not, it writes a new form, inserts it into collect, and launches it.
   *
   * @param params
   * @return
   */
  /*
   * So, there are several things to check here. The first thing we want to do
   * is see if a custom form has been defined for this table. If there is not,
   * then we will need to write a custom one. When we do this, we will then have
   * to call delete on Collect to remove the old form, which will have used the
   * same id. This will not fail if a form has not been already been
   * written--delete will simply return 0.
   */
  public Intent getIntentForOdkCollectAddRow(CollectFormParameters params,
      Map<String, String> elementNameToValue) {
    // Check if there is a custom form. If there is not, we want to delete
    // the old form and write the new form.
    if (!params.isCustom()) {
      boolean formIsReady = CollectUtil.deleteWriteAndInsertFormIntoCollect(getContentResolver(),
          params, mTableProperties);
      if (!formIsReady) {
        Log.e(/* TODO: TAG! */"Activity", "could not delete, write, or insert a generated form");
        return null;
      }
    }
    Uri formToLaunch;
    if (elementNameToValue == null) {
      formToLaunch = CollectUtil.getUriOfForm(getContentResolver(), params.getFormId());
      if (formToLaunch == null) {
        Log.e(/* TODO: TAG! */"Activity", "URI of the form to pass to Collect and launch was null");
        return null;
      }
    } else {
      // we've received some values to prepopulate the add row with.
      boolean writeDataSuccessful = CollectUtil.writeRowDataToBeEdited(elementNameToValue,
          mTableProperties, params);
      if (!writeDataSuccessful) {
        Log.e(/* TODO: TAG! */"Activity", "could not write instance file successfully!");
      }
      // Here we'll just act as if we're inserting 0, which really doesn't
      // matter?
      formToLaunch = CollectUtil.getUriForInsertedData(params, getContentResolver());
    }
    // And now finally create the intent.
    Intent intent = new Intent();
    intent.setComponent(new ComponentName("org.odk.collect.android",
        "org.odk.collect.android.activities.FormEntryActivity"));
    intent.setAction(Intent.ACTION_EDIT);
    intent.setData(formToLaunch);
    return intent;
  }

  private Map<String, String> getMapFromLimitedQuery() {
    Map<String, String> elementNameToValue = new HashMap<String, String>();
    // First add all empty strings. We will overwrite the ones that are
    // queried
    // for in the search box. We need this so that if an add is canceled, we
    // can check for equality and know not to add it. If we didn't do this,
    // but we've prepopulated an add with a query, when we return and don't
    // do
    // a check, we'll add a blank row b/c there are values in the key value
    // pairs, even though they were our prepopulated values.
    for (ColumnProperties cp : mTableProperties.getColumns()) {
      elementNameToValue.put(cp.getElementName(), "");
    }
    Query currentQuery = new Query(null, mTableProperties);
    currentQuery.loadFromUserQuery(getSearchText());
    for (int i = 0; i < currentQuery.getConstraintCount(); i++) {
      Constraint constraint = currentQuery.getConstraint(i);
      // NB: This is predicated on their only ever being a single
      // search value. I'm not sure how additional values could be
      // added.
      elementNameToValue.put(constraint.getColumnDbName(), constraint.getValue(0));
    }
    return elementNameToValue;
  }

  boolean addRowFromOdkCollectForm(int instanceId) {
    Map<String, String> formValues = getOdkCollectFormValues(instanceId);
    if (formValues == null) {
      return false;
    }
    Map<String, String> values = new HashMap<String, String>();
    for (String key : formValues.keySet()) {
      ColumnProperties cp = mTableProperties.getColumnByElementKey(key);
      if (cp == null) {
        continue;
      }
      String value = mDataUtil.validifyValue(cp, formValues.get(key));
      if (value != null) {
        values.put(key, value);
      }
    }
    // Now we want to check for equality of this and the query map. If they
    // are the same, we know we hit ignore and didn't save anything.
    Map<String, String> prepopulatedValues = getMapFromLimitedQuery();
    if (prepopulatedValues.equals(values)) {
      return false;
    }
    // TODO: get these values from the form...
    Long timestamp = null; // should be endTime in form?
    String uriUser = null; // should be this user
    String formId = null; // collect formId
    String instanceName = null; // if exists, meta/instanceName value
    String locale = null; // current locale string
    DbTable dbTable = DbTable.getDbTable(DbHelper.getDbHelper(this),
        mTableProperties.getTableId());
    dbTable.addRow(values, null, timestamp, uriUser, instanceName, formId,
        locale);
    return true;
  }

  private void handleOdkCollectAddReturn(int returnCode, Intent data) {
    if (returnCode != SherlockActivity.RESULT_OK) {
      return;
    }
    int instanceId = Integer.valueOf(data.getData().getLastPathSegment());
    addRowFromOdkCollectForm(instanceId);
  }

  private void handleOdkCollectEditReturn(int returnCode, Intent data) {
    if (returnCode != SherlockActivity.RESULT_OK) {
      return;
    }
    int instanceId = Integer.valueOf(data.getData().getLastPathSegment());
    updateRowFromOdkCollectForm(instanceId);
  }

  boolean updateRowFromOdkCollectForm(int instanceId) {
    Map<String, String> formValues = getOdkCollectFormValues(instanceId);
    if (formValues == null) {
      return false;
    }
    Map<String, String> values = getMapForInsertion(formValues);
    // TODO Update these nulls
    mDbTable.updateRow(mRowId, values, null, null, null, null, null);
    mRowId = null;
    return true;
  }

  /**
   * This gets a map of values for insertion into a row after returning from a
   * Collect form.
   *
   * @param formValues
   * @return
   */
  Map<String, String> getMapForInsertion(Map<String, String> formValues) {
    Map<String, String> values = new HashMap<String, String>();
    for (ColumnProperties cp : mTableProperties.getColumns()) {
      // we want to use element name here, b/c that is what Collect should
      // be
      // using to access all of the columns/elements.
      String elementName = cp.getElementName();
      String value = mDataUtil.validifyValue(cp, formValues.get(elementName));
      if (value != null) {
        values.put(elementName, value);
      }
    }
    return values;
  }

  protected Map<String, String> getOdkCollectFormValues(int instanceId) {
    String[] projection = { "instanceFilePath" };
    String selection = "_id = ?";
    String[] selectionArgs = { (instanceId + "") };
    Cursor c = managedQuery(COLLECT_INSTANCES_CONTENT_URI, projection, selection, selectionArgs,
        null);
    if (c.getCount() != 1) {
      return null;
    }
    c.moveToFirst();
    String instancepath = c.getString(c.getColumnIndexOrThrow("instanceFilePath"));
    Document xmlDoc = new Document();
    KXmlParser xmlParser = new KXmlParser();
    try {
      xmlParser.setInput(new FileReader(instancepath));
      xmlDoc.parse(xmlParser);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } catch (XmlPullParserException e) {
      e.printStackTrace();
      return null;
    }
    Element rootEl = xmlDoc.getRootElement();
    Node rootNode = rootEl.getRoot();
    Element dataEl = rootNode.getElement(0);
    Map<String, String> values = new HashMap<String, String>();
    for (int i = 0; i < dataEl.getChildCount(); i++) {
      Element child = dataEl.getElement(i);
      String key = child.getName();
      String value = child.getChildCount() > 0 ? child.getText(0) : null;
      values.put(key, value);
    }
    return values;
  }

  /** TODO: What does this do? */
  void openCellEditDialog(String rowId, String value, int colIndex) {
    (new CellEditDialog(rowId, value, colIndex)).show();
  }

  public static void launchTableActivity(Context context, TableProperties tp, boolean isOverview) {
    launchTableActivity(context, tp, null, null, isOverview, null);
  }

  public static void launchTableActivity(Context context, TableProperties tp, String searchText,
      boolean isOverview) {
    launchTableActivity(context, tp, searchText, null, isOverview, null);
  }

  private static void launchTableActivity(Activity context, TableProperties tp,
      Stack<String> searchStack, boolean isOverview) {
    launchTableActivity(context, tp, null, searchStack, isOverview, null);
    context.finish();
  }

  /**
   * This is based on the other launch table activity methods. This one,
   * however, allows a filename to be passed to the launching activity. This is
   * intended to be used to launch things like list view activities with a file
   * other than the default.
   */
  public static void launchTableActivityWithFilename(Activity context, TableProperties tp,
      Stack<String> searchStack, boolean isOverview, String filename) {
    launchTableActivity(context, tp, null, searchStack, isOverview, filename);
    context.finish();
  }

  /**
   * This method should launch the custom app view that is a generic user-
   * customizable home screen or html page for the app.
   */
  public static void launchAppViewActivity(Context context) {

  }

  private static void launchTableActivity(Context context, TableProperties tp, String searchText,
      Stack<String> searchStack, boolean isOverview, String filename) {
    // TODO: need to figure out how CollectionViewSettings should work.
    // make them work.
    // TableViewSettings tvs = isOverview ? tp.getOverviewViewSettings() :
    // tp
    // .getCollectionViewSettings();
    TableViewType viewType = tp.getCurrentViewType();
    Intent intent;
    // switch (tvs.getViewType()) {
    switch (viewType) {
    // case TableViewSettings.Type.LIST:
    // TODO: figure out which of these graph was originally and update it.
    case List:
      intent = new Intent(context, ListDisplayActivity.class);
      if (filename != null) {
        intent.putExtra(ListDisplayActivity.INTENT_KEY_FILENAME, filename);
      }
      break;
    // case TableViewSettings.Type.LINE_GRAPH:
    // intent = new Intent(context, LineGraphDisplayActivity.class);
    // break;
    // case TableViewSettings.Type.BOX_STEM:
    // intent = new Intent(context, BoxStemGraphDisplayActivity.class);
    // break;
    // case TableViewSettings.Type.BAR_GRAPH:
    case Graph:
      intent = new Intent(context, GraphDisplayActivity.class);
      break;
    // case TableViewSettings.Type.MAP:
    case Map:
      intent = new Intent(context, TableActivity.class);
      break;
    case Spreadsheet:
      intent = new Intent(context, SpreadsheetDisplayActivity.class);
      break;
    default:
      intent = new Intent(context, SpreadsheetDisplayActivity.class);
    }
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    if (searchStack != null) {
      String[] stackValues = new String[searchStack.size()];
      for (int i = 0; i < searchStack.size(); i++) {
        stackValues[i] = searchStack.get(i);
      }
      intent.putExtra(INTENT_KEY_SEARCH_STACK, stackValues);
    } else if (searchText != null) {
      intent.putExtra(INTENT_KEY_SEARCH, searchText);
    } else if (searchText == null) {
      KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(TableProperties.KVS_PARTITION);
      String savedQuery = kvsh.getString(TableProperties.KEY_CURRENT_QUERY);
      if (savedQuery == null) {
        savedQuery = "";
      }
      intent.putExtra(INTENT_KEY_SEARCH, savedQuery);
    }
    intent.putExtra(INTENT_KEY_IS_OVERVIEW, isOverview);
    context.startActivity(intent);
  }

  public static void launchDetailActivity(Context context, TableProperties tp, UserTable table,
      int rowNum) {
    String[] keys = new String[table.getWidth()];
    String[] values = new String[table.getWidth()];
    for (int i = 0; i < table.getWidth(); i++) {
      keys[i] = tp.getColumns()[i].getElementKey();
      values[i] = table.getData(rowNum, i);
    }
    Intent intent = new Intent(context, DetailDisplayActivity.class);
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_ID, table.getRowId(rowNum));
    intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_KEYS, keys);
    intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_VALUES, values);
    context.startActivity(intent);
  }

  private class CellEditDialog extends AlertDialog {
    private final String rowId;
    private final int colIndex;
    private final CellValueView.CellEditView cev;

    public CellEditDialog(String rowId, String value, int colIndex) {
      super(mActivity);

      this.rowId = rowId;
      this.colIndex = colIndex;
      cev = CellValueView
          .getCellEditView(mActivity, mTableProperties.getColumns()[colIndex], value);
      buildView(mActivity);
    }

    private void buildView(Context context) {
      Button setButton = new Button(context);
      setButton.setText(getResources().getString(R.string.set));
      setButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          String value = mDataUtil.validifyValue(mTableProperties.getColumns()[colIndex],
              cev.getValue());
          if (value == null) {
            // TODO: alert the user
            return;
          }
          Map<String, String> values = new HashMap<String, String>();
          values.put(mTableProperties.getColumns()[colIndex].getElementKey(), value);
          // TODO: Update these nulls.
          mDbTable.updateRow(rowId, values, null, null, null, null, null);
          dismiss();
        }
      });
      Button cancelButton = new Button(context);
      cancelButton.setText(getResources().getString(R.string.cancel));
      cancelButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          dismiss();
        }
      });
      LinearLayout buttonWrapper = new LinearLayout(context);
      buttonWrapper.addView(setButton);
      buttonWrapper.addView(cancelButton);
      LinearLayout wrapper = new LinearLayout(context);
      wrapper.setOrientation(LinearLayout.VERTICAL);
      wrapper.addView(cev);
      wrapper.addView(buttonWrapper);
      setView(wrapper);
    }
  }
}