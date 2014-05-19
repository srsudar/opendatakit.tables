package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter.ConcordantColumn;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter.ConflictColumn;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter.Resolution;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter.Section;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Activity for resolving the conflicts in a row. This is the native version,
 * which presents a UI and does not support HTML or js rules.
 * @author sudar.sam@gmail.com
 *
 */
public class CheckpointResolutionRowActivity extends ListActivity
    implements ConflictResolutionListAdapter.UICallbacks {

  private static final String TAG =
      CheckpointResolutionRowActivity.class.getSimpleName();

  public static final String INTENT_KEY_ROW_ID = "rowId";

  private static final String BUNDLE_KEY_SHOWING_LOCAL_DIALOG =
      "showingLocalDialog";
  private static final String BUNDLE_KEY_SHOWING_SERVER_DIALOG =
      "showingServerDialog";
  private static final String BUNDLE_KEY_VALUE_KEYS = "valueValueKeys";
  private static final String BUNDLE_KEY_CHOSEN_VALUES = "chosenValues";
  private static final String BUNDLE_KEY_RESOLUTION_KEYS = "resolutionKeys";
  private static final String BUNDLE_KEY_RESOLUTION_VALUES =
      "resolutionValues";

  private ConflictResolutionListAdapter mAdapter;
  private String mAppName;
  private String mTableId;
  private String mRowId;

  private Button mButtonTakeLocal;
  private Button mButtonTakeServer;
  private List<ConflictColumn> mConflictColumns;

  /**
   * The message to the user as to why they're getting extra options. Will be
   * either thing to the effect of "someone has deleted something you've
   * changed", or "you've deleted something someone has changed". They'll then
   * have to choose either to delete or to go ahead and actually restore and
   * then resolve it.
   */
  private TextView mTextViewDeletionMessage;
  /**
   * The option saying they're going to restore and then resolve the conflicts.
   */
  private RadioGroup mRadioGroupDeletion;
  private RadioButton mRadioButtonRestoreAndResolve;
  /**
   * The option saying they're going to delete it, possibly discarding any
   * changes they'd made.
   */
  private RadioButton mRadioButtonDelete;

  private boolean mIsShowingTakeLocalDialog;
  private boolean mIsShowingTakeServerDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mAppName = getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    if ( mAppName == null ) {
      mAppName = TableFileUtils.getDefaultAppName();
    }
    this.setContentView(R.layout.checkpoint_resolution_row_activity);
    this.mTextViewDeletionMessage = (TextView)
        findViewById(R.id.conflict_resolution_deletion_message);
    this.mRadioGroupDeletion = (RadioGroup)
        findViewById(R.id.conflict_resolution_deleted_states_radio_group);
    this.mRadioButtonRestoreAndResolve = (RadioButton)
        findViewById(R.id.conflict_resolution_radio_button_restore);
    this.mRadioButtonDelete = (RadioButton)
        findViewById(R.id.conflict_resolution_radio_button_delete);
    this.mButtonTakeLocal =
        (Button) findViewById(R.id.conflict_resolution_button_take_local);
    this.mButtonTakeServer =
        (Button) findViewById(R.id.conflict_resolution_button_take_server);

    mTableId =
        getIntent().getStringExtra(Constants.IntentKeys.TABLE_ID);
    this.mRowId = getIntent().getStringExtra(INTENT_KEY_ROW_ID);
    TableProperties tp =
        TableProperties.getTablePropertiesForTable(this, mAppName, mTableId);
    DbTable dbTable = DbTable.getDbTable(tp);

    UserTable conflictTable = dbTable.rawSqlQuery(DataTableColumns.ID + "=?", new String[] {mRowId}, null, null, DataTableColumns.SAVEPOINT_TIMESTAMP, "ASC");
    if ( conflictTable.getNumberOfRows() == 0 ) {
      // another process resolved this?
      finish();
    }
    // the first row is the oldest -- it should be a COMPLETE or INCOMPLETE row
    // if it isn't, then this is a new row and the option is to delete it or
    // save as incomplete. Otherwise, it is to roll back or update to incomplete.
    Row rowStarting = conflictTable.getRowAtIndex(0);
    String type = rowStarting.getDataOrMetadataByElementKey(DataTableColumns.SAVEPOINT_TYPE);
    boolean deleteEntirely = ( type == null || type.length() == 0 || conflictTable.getNumberOfRows() == 1 );
    Row rowEnding = conflictTable.getRowAtIndex(conflictTable.getNumberOfRows()-1);
    //
    // And now we need to construct up the adapter.

    // There are several things to do be aware of. We need to get all the
    // section headings, which will be the column names. We also need to get
    // all the values which are in conflict, as well as those that are not.
    // We'll present them in user-defined order, as they may have set up the
    // useful information together.
    List<String> columnOrder = tp.getPersistedColumns();
    // This will be the number of rows down we are in the adapter. Each
    // heading and each cell value gets its own row. Columns in conflict get
    // two, as we'll need to display each one to the user.
    int adapterOffset = 0;
    List<Section> sections = new ArrayList<Section>();
    this.mConflictColumns = new ArrayList<ConflictColumn>();
    List<ConcordantColumn> noConflictColumns =
        new ArrayList<ConcordantColumn>();
    for (int i = 0; i < columnOrder.size(); i++) {
      String elementKey = columnOrder.get(i);
      String columnDisplayName =
          tp.getColumnByElementKey(elementKey).getLocalizedDisplayName();
      Section newSection = new Section(adapterOffset, columnDisplayName);
      ++adapterOffset;
      sections.add(newSection);
      String localValue = rowEnding.getDataOrMetadataByElementKey(elementKey);
      String serverValue = rowStarting.getDataOrMetadataByElementKey(elementKey);
      if ((localValue == null && serverValue == null) ||
    	  (localValue != null && localValue.equals(serverValue))) {
        // TODO: this doesn't compare actual equality of blobs if their display
        // text is the same.
        // We only want to display a single row, b/c there are no choices to
        // be made by the user.
        ConcordantColumn concordance = new ConcordantColumn(adapterOffset,
            localValue);
        noConflictColumns.add(concordance);
        ++adapterOffset;
      } else {
        // We need to display both the server and local versions.
        ConflictColumn conflictColumn = new ConflictColumn(adapterOffset,
            elementKey, localValue, serverValue);
        ++adapterOffset;
        mConflictColumns.add(conflictColumn);
      }
    }
    // Now that we have the appropriate lists, we need to construct the
    // adapter that will display the information.
    this.mAdapter = new ConflictResolutionListAdapter(
        this.getActionBar().getThemedContext(), this, sections,
        noConflictColumns, mConflictColumns);
    this.setListAdapter(mAdapter);
    this.onDecisionMade();
    // Here we'll handle the cases of whether or not rows were deleted. There
    // are three cases to consider:
    // 1) both rows were updated, neither is deleted. This is the normal case
    // 2) the server row was deleted, the local was updated (thus a conflict)
    // 3) the local was deleted, the server was updated (thus a conflict)
    // To Figure this out we'll first need the state of each version.
    // Note that these calls should never return nulls, as whenever a row is in
    // conflict, there should be a conflict type. Therefore if we throw an
    // error that is fine, as we've violated an invariant.

    this.mButtonTakeLocal.setOnClickListener(new DiscardOlderValuesAndMarkNewestAsIncompleteRowClickListener());
    this.mButtonTakeServer.setOnClickListener(new DiscardNewerValuesAndRetainOldestInOriginalStateRowClickListener());
    if (!deleteEntirely) {
      // User chooses to take the older (server) or newest (local) set of values.
      // Hide the elements of the view relevant to deletion restoration.
      mTextViewDeletionMessage.setVisibility(View.GONE);
      mRadioGroupDeletion.setVisibility(View.GONE);
      mAdapter.setConflictColumnsEnabled(false);
      mAdapter.notifyDataSetChanged();
    } else {
      // User chooses to delete record (server) or take newest (local) set of values.
      this.mTextViewDeletionMessage.setVisibility(View.VISIBLE);
      this.mTextViewDeletionMessage.setText(
          getString(R.string.conflict_server_was_deleted_explanation));
      this.mRadioGroupDeletion.setVisibility(View.VISIBLE);
      this.mRadioButtonRestoreAndResolve.setText(
          getString(R.string.radio_button_message_restore_server_deleted));
      this.mRadioButtonRestoreAndResolve.setOnClickListener(
          new DiscardOlderValuesAndMarkNewestAsIncompleteRowClickListener());
      this.mRadioButtonDelete.setText(
          getString(R.string.radio_button_message_delete_server_deleted));
      this.mRadioButtonDelete.setOnClickListener(
          new DiscardAllValuesAndDeleteRowClickListener());
      // Disable these, because can't yet take action on row.
      mButtonTakeServer.setEnabled(false);
      mButtonTakeLocal.setEnabled(false);
      mAdapter.setConflictColumnsEnabled(false);
      mAdapter.notifyDataSetChanged();
    }
  }

  /*
   * (non-Javadoc)
   * @see org.opendatakit.tables.views.components.ConflictResolutionListAdapter.UICallbacks#onDecisionMade(boolean)
   */
  @Override
  public void onDecisionMade() {
    // set the listview enabled in case it'd been down due to deletion
    // resolution.
    mAdapter.setConflictColumnsEnabled(false);
    mAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(BUNDLE_KEY_SHOWING_LOCAL_DIALOG,
        mIsShowingTakeLocalDialog);
    outState.putBoolean(BUNDLE_KEY_SHOWING_SERVER_DIALOG,
        mIsShowingTakeServerDialog);
    // We also need to get the chosen values and decisions and save them so
    // that we don't lose information if they rotate the screen.
    Map<String, String> chosenValuesMap = mAdapter.getResolvedValues();
    Map<String, Resolution> userResolutions = mAdapter.getResolutions();
    if (chosenValuesMap.size() != userResolutions.size()) {
      Log.e(TAG, "[onSaveInstanceState] chosen values and user resolutions" +
      		" are not the same size. This should be impossible, so not " +
      		"saving state.");
      return;
    }
    String[] valueKeys = new String[chosenValuesMap.size()];
    String[] chosenValues = new String[chosenValuesMap.size()];
    String[] resolutionKeys = new String[userResolutions.size()];
    String[] resolutionValues = new String[userResolutions.size()];
    int i = 0;
    for (Map.Entry<String, String> valueEntry : chosenValuesMap.entrySet()) {
      valueKeys[i] = valueEntry.getKey();
      chosenValues[i] = valueEntry.getValue();
      ++i;;
    }
    i = 0;
    for (Map.Entry<String, Resolution> resolutionEntry :
        userResolutions.entrySet()) {
      resolutionKeys[i] = resolutionEntry.getKey();
      resolutionValues[i] = resolutionEntry.getValue().name();
      ++i;
    }
    outState.putStringArray(BUNDLE_KEY_VALUE_KEYS, valueKeys);
    outState.putStringArray(BUNDLE_KEY_CHOSEN_VALUES, chosenValues);
    outState.putStringArray(BUNDLE_KEY_RESOLUTION_KEYS, resolutionKeys);
    outState.putStringArray(BUNDLE_KEY_RESOLUTION_VALUES, resolutionValues);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    Log.e(TAG, "onRestoreInstanceState");
    if (savedInstanceState.containsKey(BUNDLE_KEY_SHOWING_LOCAL_DIALOG)) {
      boolean wasShowingLocal =
          savedInstanceState.getBoolean(BUNDLE_KEY_SHOWING_LOCAL_DIALOG);
      if (wasShowingLocal) this.mButtonTakeLocal.performClick();
    }
    if (savedInstanceState.containsKey(BUNDLE_KEY_SHOWING_SERVER_DIALOG)) {
      boolean wasShowingServer =
          savedInstanceState.getBoolean(BUNDLE_KEY_SHOWING_SERVER_DIALOG);
      if (wasShowingServer) this.mButtonTakeServer.performClick();
    }
    String[] valueKeys =
        savedInstanceState.getStringArray(BUNDLE_KEY_VALUE_KEYS);
    String[] chosenValues =
        savedInstanceState.getStringArray(BUNDLE_KEY_CHOSEN_VALUES);
    String[] resolutionKeys =
        savedInstanceState.getStringArray(BUNDLE_KEY_RESOLUTION_KEYS);
    String[] resolutionValues =
        savedInstanceState.getStringArray(BUNDLE_KEY_RESOLUTION_VALUES);
    if (valueKeys != null) {
      // Then we know that we should have the chosenValues as well, or else
      // there is trouble. We're not doing a null check here, but if we didn't
      // get it then we know there is an error. We'll throw a null pointer
      // exception, but that is better than restoring bad state.
      // Same thing goes for the resolution keys. Those and the map should
      // always go together.
      Map<String, String> chosenValuesMap = new HashMap<String, String>();
      for (int i = 0; i < valueKeys.length; i++) {
        chosenValuesMap.put(valueKeys[i], chosenValues[i]);
      }
      Map<String, Resolution> userResolutions =
          new HashMap<String, Resolution>();
      for (int i = 0; i < resolutionKeys.length; i++) {
        userResolutions.put(resolutionKeys[i],
            Resolution.valueOf(resolutionValues[i]));
      }
      mAdapter.setRestoredState(chosenValuesMap, userResolutions);
    }
    // And finally, call this to make sure we update the button as appropriate.
    Log.e(TAG, "going to call onDecisionMade");
    this.onDecisionMade();

  }

  private class DiscardAllValuesAndDeleteRowClickListener
      implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      // We should do a popup.
      AlertDialog.Builder builder = new AlertDialog.Builder(
          CheckpointResolutionRowActivity.this.getActionBar()
          .getThemedContext());
      builder.setMessage(
          getString(R.string.conflict_delete_local_confirmation_warning));
      builder.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              // TODO: delete the local version.
              // this will be a simple matter of deleting all the rows with the
              // same rowid on the local device.
              TableProperties tp =
                  TableProperties.getTablePropertiesForTable(
                      CheckpointResolutionRowActivity.this, mAppName, mTableId);
              DbTable dbTable = DbTable.getDbTable(tp);
              dbTable.deleteRowActual(mRowId);
              CheckpointResolutionRowActivity.this.finish();
              Log.d(TAG, "deleted local and server versions");
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
          // here we need to do nothing and UNCHECK the radiobutton.
          mRadioButtonDelete.setChecked(false);
          dialog.dismiss();
        }
      });
      builder.create().show();
    }
  }

  private class DiscardOlderValuesAndMarkNewestAsIncompleteRowClickListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(
          CheckpointResolutionRowActivity.this.getActionBar()
          .getThemedContext());
      builder.setMessage(getString(R.string.take_local_warning));
      builder.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              mIsShowingTakeLocalDialog = false;
              TableProperties tp =
                  TableProperties.getTablePropertiesForTable(
                      CheckpointResolutionRowActivity.this, mAppName, mTableId);
              DbTable dbTable = DbTable.getDbTable(tp);
              // TODO: discard all but newest and mark it as INCOMPLETE
              CheckpointResolutionRowActivity.this.finish();
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingTakeLocalDialog = false;
        }
      });
      mIsShowingTakeLocalDialog = true;
      builder.create().show();
    }

  }

  private class DiscardNewerValuesAndRetainOldestInOriginalStateRowClickListener implements View.OnClickListener {


    @Override
    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(
          CheckpointResolutionRowActivity.this.getActionBar()
          .getThemedContext());
      builder.setMessage(getString(R.string.take_server_warning));
      builder.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              mIsShowingTakeServerDialog = false;
              TableProperties tp =
                  TableProperties.getTablePropertiesForTable(
                      CheckpointResolutionRowActivity.this, mAppName, mTableId);
              DbTable dbTable = DbTable.getDbTable(tp);
              // TODO: delete all but oldest non-null savepoint...
              CheckpointResolutionRowActivity.this.finish();
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingTakeServerDialog = false;
        }
      });
      mIsShowingTakeServerDialog = true;
      builder.create().show();
    }

  }

}
