package yoonsung.odk.spreadsheet.Activity;

import java.util.List;

import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.Data;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * An activity for setting display preferences.
 * 
 * TODO: have options for each table
 */
public class DisplayPrefsActivity extends Activity {
	
	/** the data */
	private Data data;
	/** the shared preferences manager */
	private SharedPreferences settings;
	/** the table name spinner */
	private Spinner tnSpinner;
	/** the table of options */
	private TableLayout opts;
	
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = new Data();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        tnSpinner = new Spinner(this);
        List<String> tableNames = data.getTables();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item,
				tableNames.toArray(new String[0]));
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		tnSpinner.setAdapter(adapter);
		tnSpinner.setSelection(0);
		tnSpinner.setOnItemSelectedListener(new TableSpinListener());
    	LinearLayout v = new LinearLayout(this);
		v.setOrientation(LinearLayout.VERTICAL);
		v.addView(tnSpinner);
		opts = new TableLayout(this);
		v.addView(opts);
		setContentView(v);
		prepTableOpts();
    }
    
    /**
     * Prepares the table of options for the selected table.
     */
    private void prepTableOpts() {
    	String tableName = tnSpinner.getSelectedItem().toString();
    	Table table = data.getTable();
    	opts.removeAllViews();
    	for(int i=0; i<table.getWidth(); i++) {
    		TableRow row = new TableRow(this);
    		String colName = table.getColName(i);
    		TextView label = new TextView(this);
    		label.setText(colName);
    		row.addView(label);
    		EditText et = new EditText(this);
    		int width = settings.getInt("tablewidths-" + tableName + "-" +
    				colName, 125);
    		et.setText(new Integer(width).toString());
    		et.addTextChangedListener(new ETListener(colName));
    		row.addView(et);
    		opts.addView(row);
    	}
    }
    
    /**
     * A listener for the table name spinner. Calls prepTableOpts() on change.
     */
    private class TableSpinListener
    		implements AdapterView.OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			prepTableOpts();
		}
		@Override
		public void onNothingSelected(AdapterView<?> parent) {}
    }
    
    /**
     * A listener for changes to the width fields. Updates the preferences on
     * change.
     */
    private class ETListener implements TextWatcher {
    	private String colName;
    	/**
    	 * Constructs a new ETListener.
    	 * @param colName the name of the column whose field it will listen to
    	 */
    	ETListener(String colName) {
    		this.colName = colName;
    	}
		@Override
		public void afterTextChanged(Editable s) {
			try {
		    	String tableName = tnSpinner.getSelectedItem().toString();
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("tablewidths-" + tableName + "-" + colName,
						new Integer(s.toString()));
				editor.commit();
			} catch(NumberFormatException e) {
				// TODO: something here
			}
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {}
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {}
    }
	
}
