package org.opendatakit.tables.views.components;

import java.util.List;

import org.opendatakit.aggregate.odktables.rest.SyncState;
import org.opendatakit.common.android.data.ColorRule;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

public class ColorRuleAdapter extends ArrayAdapter<ColorRule> {
  
  private static final String TAG = ColorRuleAdapter.class.getSimpleName();
  
  private Context mContext;
  private List<ColorRule> mColorRules;
  private int mResourceId;
  private ColorRuleGroup.Type mType;
  private TableProperties mTableProperties;
  
  public ColorRuleAdapter(
      Context context,
      int resource,
      List<ColorRule> colorRules,
      TableProperties tableProperties,
      ColorRuleGroup.Type colorRuleType) {
    super(context, resource, colorRules);
    this.mContext = context;
    this.mResourceId = resource;
    this.mColorRules = colorRules;
    this.mType = colorRuleType;
    this.mTableProperties = tableProperties;
  }
  
  private View createView(ViewGroup parent) {
    LayoutInflater layoutInflater = (LayoutInflater)
        parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    return layoutInflater.inflate(
        this.mResourceId,
        parent,
        false);
  }
  
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View row = convertView;
    if (row == null) {
      row = this.createView(parent);
    }
    final int currentPosition = position;
    // We'll need to display the display name if this is an editable field.
    // (ie if a status column or table rule)
    String description = "";
    boolean isMetadataRule = false;
    if (mType == ColorRuleGroup.Type.STATUS_COLUMN ||
        mType == ColorRuleGroup.Type.TABLE) {
      ColorRule colorRule = mColorRules.get(currentPosition);
      String elementKey = colorRule.getColumnElementKey();
      if (DbTable.getAdminColumns().contains(elementKey)) {
        isMetadataRule = true;
        // We know it must be a String rep of an int.
        SyncState targetState = SyncState.valueOf(colorRule.getVal());
        // For now we need to handle the special cases of the sync state.
        if (targetState == SyncState.inserting) {
          description = this.mContext.getString(
              R.string.sync_state_equals_inserting_message);
        } else if (targetState == SyncState.updating) {
          description = this.mContext.getString(
              R.string.sync_state_equals_updating_message);
        } else if (targetState == SyncState.rest) {
          description = this.mContext.getString(
              R.string.sync_state_equals_rest_message);
        } else if (targetState == SyncState.rest_pending_files) {
          description = this.mContext.getString(
              R.string.sync_state_equals_rest_pending_files_message);
        } else if (targetState == SyncState.deleting) {
          description = this.mContext.getString(
              R.string.sync_state_equals_deleting_message);
        } else if (targetState == SyncState.conflicting) {
          description = this.mContext.getString(
              R.string.sync_state_equals_conflicting_message);
        } else {
          Log.e(TAG, "unrecognized sync state: " + targetState);
          description = "unknown";
        }
      } else {
        description = this.mTableProperties.getColumnByElementKey(elementKey)
            .getLocalizedDisplayName();
      }
    }
    if (!isMetadataRule) {
      description += " " +
          mColorRules.get(currentPosition).getOperator().getSymbol() + " " +
          mColorRules.get(currentPosition).getVal();
    }
    TextView label =
        (TextView) row.findViewById(R.id.row_label);
    label.setText(description);
    final int backgroundColor =
        mColorRules.get(currentPosition).getBackground();
    final int textColor =
        mColorRules.get(currentPosition).getForeground();
    // Will demo the color rule.
    TextView exampleView =
        (TextView) row.findViewById(R.id.row_ext);
    exampleView.setText(this.mContext.getString(R.string.status_column));
    exampleView.setTextColor(textColor);
    exampleView.setBackgroundColor(backgroundColor);
    exampleView.setVisibility(View.VISIBLE);
    // The radio button is meaningless here, so get it off the screen.
    final RadioButton radioButton = (RadioButton)
        row.findViewById(R.id.radio_button);
    radioButton.setVisibility(View.GONE);
    // And now the settings icon.
    final ImageView editView = (ImageView)
        row.findViewById(R.id.row_options);
    final View holderView = row;
    editView.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        holderView.showContextMenu();
      }
    });
    return row;
  }
}
