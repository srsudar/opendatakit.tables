package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.defaultopts.SmsOutFormatSetActivity;
import yoonsung.odk.spreadsheet.Activity.importexport.ImportExportActivity;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.DBIO;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.SecurityTables;
import yoonsung.odk.spreadsheet.Database.TableList;
import yoonsung.odk.spreadsheet.Database.TableList.TableInfo;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class TableManager extends ListActivity {

	public static final int ADD_NEW_TABLE     		= 0;
	public static final int ADD_NEW_SECURITY_TABLE 	= 1;
	public static final int IMPORT_EXPORT			= 2;
	public static final int SET_DEFAULT_TABLE 		= 3;
	public static final int SET_SECURITY_TABLE      = 4;
	public static final int SET_SHORTCUT_TABLE      = 5;
	public static final int CHANGE_TABLE_NAME 		= 6;
	public static final int REMOVE_TABLE      		= 7;
	public static final int ADD_NEW_SHORTCUT_TABLE  = 8;
	public static final int UNSET_DEFAULT_TABLE     = 9;
	public static final int UNSET_SECURITY_TABLE    = 10;
	public static final int UNSET_SHORTCUT_TABLE    = 11;
	public static final int AGGREGATE               = 12;
	
	private static String[] from = new String[] {"label", "ext"};
	private static int[] to = new int[] { android.R.id.text1, android.R.id.text2 };
	 	
	private DBIO db;
	private TableList tl;
	private List<TableInfo> tiList;
	
	private SimpleAdapter arrayAdapter;
	
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 this.db = new DBIO();
		 this.tl = new TableList();
		 
		 // Set title of activity
		 setTitle("ODK Tables > Table Manager");
		 
		 // Set Content View
		 setContentView(R.layout.white_list);
		 
		 HashMap<String, String> tableListTmp = tl.getAllTableList();
		 boolean loadError = getIntent().getBooleanExtra("loadError", false);
		 if (loadError && tableListTmp.size() < 1) {
			makeNoTableNotice();
		 } else {
			 refreshList();
		 }
	 }
	 
	 @Override
	 public void onResume() {
		 super.onResume();
		 HashMap<String, String> tableListTmp = tl.getAllTableList();
		 if (tableListTmp.size() < 1) {
			 makeNoTableNotice();
		 } else {
			 refreshList();
		 }
	 }
	 
	 private void makeNoTableNotice() {
		 List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
		 HashMap<String, String> temp = new HashMap<String, String>();
		 temp.put("label", "Click menu to add new table");
		 fillMaps.add(temp);
		 arrayAdapter = new SimpleAdapter(this, fillMaps, R.layout.white_list_row, from, to);
		 setListAdapter(arrayAdapter);
	 }
	 
	 public void refreshList() {
		 registerForContextMenu(getListView());
		 
		 tiList = tl.getTableList();
		 String defTableID = getDefaultTableID();
		 List<Map<String, String>> fMaps =
		     new ArrayList<Map<String, String>>();
		 for(TableInfo ti : tiList) {
		     Map<String, String> map = new HashMap<String, String>();
		     map.put("label", ti.getTableName());
		     if(ti.getTableType() == 2) {
		         map.put("ext", "Access Control Table");
		     } else if(ti.getTableType() == 3) {
		         map.put("ext", "Shortcut Table");
		     }
		     if(ti.getTableID().equals(defTableID)) {
		         if(map.get("ext") == null) {
		             map.put("ext", "Default Table");
		         } else {
		             map.put("ext", map.get("ext") + "; Default Table");
		         }
		     }
		     fMaps.add(map);
		 }
		 	 
		 // fill in the grid_item layout
		 arrayAdapter = new SimpleAdapter(this, fMaps, R.layout.white_list_row, from, to); 
		 setListAdapter(arrayAdapter);
		
		 ListView lv = getListView();
		 lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				
			 	@Override
				public void onItemClick(AdapterView adView, View view,
											int position, long id) {
					
			 		HashMap<String, String> current = (HashMap)arrayAdapter.getItem(position);
			 		String tableName = current.get("label");
			 		
					// Load Selected Table
					TableList tl = new TableList();
					int tableID = tl.getTableID(tableName);
					Log.e("Selected Table", tableName + " " + tableID);
					loadSelectedTable(position);
				}
		 });
	 } 
	 
	 private void loadSelectedTable(int index) {
	     TableInfo ti = tiList.get(index);
	     Intent i;
	     Log.d("TM", "TableManager:" + ti.getTableType());
	     switch(ti.getTableType()) {
	     case TableList.TABLETYPE_DATA:
	         i = new Intent(this, SpreadSheet.class);
	         break;
	     case TableList.TABLETYPE_SECURITY:
	         i = new Intent(this, SpreadSheet.class);
	         break;
	     case TableList.TABLETYPE_SHORTCUT:
	         i = new Intent(this, ShortcutTableActivity.class);
	         break;
         default:
             i = new Intent(this, SpreadSheet.class);
             break;
	     }
	     i.putExtra("tableID", ti.getTableID());
		 startActivity(i);
	 }
	 
	 @Override
	 public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		 super.onCreateContextMenu(menu, v, menuInfo);
		 AdapterView.AdapterContextMenuInfo acmi =
		     (AdapterView.AdapterContextMenuInfo) menuInfo;
		 TableInfo ti = tiList.get(acmi.position);
		 if(ti.getTableID().equals(getDefaultTableID())) {
	         menu.add(0, UNSET_DEFAULT_TABLE, 0, "Unset as Default Table");
		 } else {
	         menu.add(0, SET_DEFAULT_TABLE, 0, "Set as Default Table");
		 }
		 int tableType = ti.getTableType();
		 if(tableType == TableList.TABLETYPE_DATA) {
		     if (couldBeSecurityTable(ti.getTableID())) {
	             menu.add(0, SET_SECURITY_TABLE, 0, "Set as Access Control Table");
		     }
		     if (couldBeShortcutTable(ti.getTableID())) {
		         menu.add(0, SET_SHORTCUT_TABLE, 0, "Set as Shortcut Table");
		     }
		 } else if(tableType == TableList.TABLETYPE_SECURITY) {
	         menu.add(0, UNSET_SECURITY_TABLE, 0, "Unset as Access Control Table");
		 } else if(tableType == TableList.TABLETYPE_SHORTCUT) {
		     menu.add(0, UNSET_SHORTCUT_TABLE, 0, "Unset as Shortcut Table");
		 }
		 menu.add(0, CHANGE_TABLE_NAME, 1, "Change Table Name");
		 menu.add(0, REMOVE_TABLE, 2, "Delete the Table");
	 }
	 
	 private boolean couldBeSecurityTable(String tableId) {
	     String[] expected = { "phone_number", "id", "password" };
	     return checkTable(expected, tableId);
	 }
	 
	 private boolean couldBeShortcutTable(String tableId) {
         String[] expected = { "name", "input_format", "output_format" };
         return checkTable(expected, tableId);
	 }
	 
	 private boolean checkTable(String[] expectedCols, String tableId) {
         TableProperty tp = new TableProperty(tableId);
         List<String> cols = tp.getColOrderArrayList();
         if (cols.size() < expectedCols.length) {
             return false;
         }
         for (int i = 0; i < 3; i++) {
             if (!expectedCols[i].equals(cols.get(i))) {
                 return false;
             }
         }
         return true;
	 }
	 
	 @Override
	 public boolean onContextItemSelected(MenuItem item) {
	 
		 AdapterView.AdapterContextMenuInfo info= (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		 HashMap<String, String> sel = (HashMap)arrayAdapter.getItem(info.position);
		 
		 String tableName =  sel.get("label");
		 int tableID   = tl.getTableID(tableName);
		 
		 switch (item.getItemId()) {
		 case SET_DEFAULT_TABLE:
			 setDefaultTable(Integer.toString(tableID));
			 refreshList();
			 return true;
		 case SET_SECURITY_TABLE:
			 tl.setAsSecurityTable(Integer.toString(tableID));
             refreshList();
			 return true;
		 case UNSET_SECURITY_TABLE:
		     tl.unsetAsSecurityTable(Integer.toString(tableID));
		     refreshList();
		     return true;
		 case SET_SHORTCUT_TABLE:
		     tl.setAsShortcutTable(Integer.toString(tableID));
             refreshList();
		     return true;
         case UNSET_SHORTCUT_TABLE:
             tl.setAsDataTable(Integer.toString(tableID));
             refreshList();
             return true;
		 case CHANGE_TABLE_NAME:
			 // TO be Done
			 alertForNewTableName(false, -1, Integer.toString(tableID), null);
			 return true; 
		 case REMOVE_TABLE:
			 // To be Done
			 removeTable(Integer.toString(tableID));
			 refreshList();
			 return true;
		 }
		 return(super.onOptionsItemSelected(item));
	 }
	 
	 // CREATE OPTION MENU
	 @Override
	 public boolean onCreateOptionsMenu(Menu menu) {
		 super.onCreateOptionsMenu(menu);
		 menu.add(0, ADD_NEW_TABLE, 0, "Add New Data Table");
		 menu.add(0, ADD_NEW_SECURITY_TABLE, 0, "Add New Access Control Table");
		 menu.add(0, ADD_NEW_SHORTCUT_TABLE, 0, "Add New Shortcut Table");
		 menu.add(0, IMPORT_EXPORT, 0, "File Import/Export");
		 menu.add(0, AGGREGATE, 0, "ODK Aggregate Import/Export");
		 return true;
	 }
    
	 // HANDLE OPTION MENU
	 @Override
	 public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	
		 Log.d("timing", "menu item selected");
        
		 // HANDLES DIFFERENT MENU OPTIONS
		 switch(item.getItemId()) {
		 case ADD_NEW_TABLE:
			 alertForNewTableName(true, TableList.TABLETYPE_DATA, null, null);
			 return true;
		 case ADD_NEW_SECURITY_TABLE:
			 alertForNewTableName(true, TableList.TABLETYPE_SECURITY, null, null);
			 return true;
		 case ADD_NEW_SHORTCUT_TABLE:
             alertForNewTableName(true, TableList.TABLETYPE_SHORTCUT, null, null);
             return true;
		 case IMPORT_EXPORT:
			 Intent i = new Intent(this, ImportExportActivity.class);
			 startActivity(i);
			 return true;
		 case AGGREGATE:
			 Intent j = new Intent(this, Aggregation.class);
			 startActivity(j);
			 return true;
		 }
    	
		 return super.onMenuItemSelected(featureId, item);
	 }
	 
	 // Ask for a new column name.
	 private void alertForNewTableName(final boolean isNewTable, 
			 final int tableType, final String tableID, String givenTableName) {
		
		 // Prompt an alert box
		 AlertDialog.Builder alert = new AlertDialog.Builder(this);
		 alert.setTitle("Name of New Table");
	
		 // Set an EditText view to get user input 
		 final EditText input = new EditText(this);
		 alert.setView(input);
		 if (givenTableName != null) 
			 input.setText(givenTableName);
		 
		 // OK Action => Create new Column
		 alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int whichButton) {
				 String newTableName = input.getText().toString().trim();
				
				 if (newTableName == null || newTableName.equals("")) {
					// Table name is empty string
					toastTableNameError("Table name cannot be empty!");
					alertForNewTableName(isNewTable, tableType, tableID, null);
			 	 } else if (newTableName.contains(" ")) {
					// Check for space in-between
					toastTableNameError("Table name cannot contain spaces!");
					alertForNewTableName(isNewTable, tableType, tableID, newTableName.replace(' ', '_'));
			 	 } else {
			 		 if (isNewTable) 
			 			 addTable(newTableName, tableType);
			 		 else 
			 			 changeTableName(tableID, newTableName);
			 		 refreshList();
				 }
			 }
		 });

		 // Cancel Action
		 alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int whichButton) {
				 // Canceled.
			 }
		 });

		 alert.show();
	 }
	 
	 private void toastTableNameError(String msg) {
		 Context context = getApplicationContext();
		 Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
		 toast.show();
	 }
	 
	 private void setDefaultTable(String tableID) {
		// Share preference editor
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString("ODKTables:tableID", tableID);
	    editor.commit();
	    refreshList();
	 }
	 
	 private void addTable(String tableName, int tableType) { 
		// Register new table in TableList
	    try {
	         tl.registerNewTable(tableName, tableType);
	    } catch(Exception e) {
             Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG);
             return;
	     }
		 // Create a new table in the database
		 createNewDataTable(tableName);
		 if (tableType == TableList.TABLETYPE_SECURITY) {
			 int tableID = tl.getTableID(tableName);
			 // Add required columns for a security table
			 DataTable dt = new DataTable(Integer.toString(tableID));
			 dt.addNewColumn(SecurityTables.COL_1_PHONE_NUM);
			 dt.addNewColumn(SecurityTables.COL_2_PASSWORD);
			 dt.addNewColumn(SecurityTables.COL_3_ID);
			 // Register column order in table property
			 TableProperty tp = new TableProperty(Integer.toString(tableID));
			 ArrayList<String> colOrder = new ArrayList<String>();
			 colOrder.add("phone_number");
			 colOrder.add("id");
			 colOrder.add("password");
			 tp.setColOrder(colOrder);
			 // Set it as Security Table
			 tl.setAsSecurityTable(Integer.toString(tableID));
		 } else if(tableType == TableList.TABLETYPE_SHORTCUT){
		     DataTable dt = new DataTable(tl.getTableID(tableName) + "");
		     dt.addNewColumn("name");
		     dt.addNewColumn("input_format");
		     dt.addNewColumn("output_format");
		 }
		  
	 }
	 
	 private void createNewDataTable(String tableName) {
		 SQLiteDatabase con = db.getConn();
		 con.execSQL("CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
	                + DataTable.DATA_ROWID + " INTEGER PRIMARY KEY,"
	                + DataTable.DATA_PHONE_NUMBER_IN + " TEXT,"
	                + DataTable.DATA_TIMESTAMP + " TEXT"
	                + ");");
		 con.close();
	 }
	 
	 private void createNewSecurityTable(String tableName) {
		 int tableID = tl.getTableID(tableName);
		 SQLiteDatabase con = db.getConn();
		 con.execSQL("CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
	                + DataTable.DATA_ROWID + " INTEGER PRIMARY KEY,"
	                + "`" + SecurityTables.COL_1_PHONE_NUM + "` TEXT,"
	                + "`" + SecurityTables.COL_2_PASSWORD + "` TEXT,"
	                + "`" + SecurityTables.COL_3_ID + "` TEXT"
	                + ");");
		 con.close();
		 // Register column order in table property
		 TableProperty tp = new TableProperty(Integer.toString(tableID));
		 ArrayList<String> colOrder = new ArrayList<String>();
		 colOrder.add("phone_number");
		 colOrder.add("id");
		 colOrder.add("password");
		 tp.setColOrder(colOrder);
	 }
	 
	 private void removeTable(String tableID) {
		 String tableName = tl.getTableName(tableID);
		 
		 // Remove Actual Table
		 SQLiteDatabase con = db.getConn();
		 try {
			 con.execSQL("DROP TABLE `" + tableName + "`;");
			 con.close();
		 } catch (Exception e) {
			 Log.e("TableManager", "Error While Drop a Table");
		 }
		 
		 // Unregister Table from TableList
		 tl.unregisterTable(tableID);
		 
		 // Clean up Table Property
		 TableProperty tp = new TableProperty(tableID);
		 tp.removeAll();
		 
		 // Clean up Column Property
		 ColumnProperty cp = new ColumnProperty(tableID);
		 cp.removeAll();
	 }
	 
	 private void changeTableName(String tableID, String newTableName) {
		 String tableName = tl.getTableName(tableID);
		 
		 // Change actual table
		 SQLiteDatabase con = db.getConn();
		 con.execSQL("ALTER TABLE `"
				  	+ tableName
				  	+ "` RENAME TO `"
				  	+ newTableName
				  	+ "`;");
		 
		 // Change on TableList
		 ContentValues cv = new ContentValues();
		 cv.put(TableList.TABLE_NAME, newTableName);
		 con.update(TableList.TABLE_LIST, cv, TableList.TABLE_ID+" = "+tableID, null);
		
		 con.close();
	 }
	 
	 private String getDefaultTableID() {SharedPreferences settings =
         PreferenceManager.getDefaultSharedPreferences(this);
         return settings.getString("ODKTables:tableID", null);
	 }
	 
 }
