package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.tables.activities.TableActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * The LocationDialogFragment is used when asking the user if they would like to
 * add a location to the map.
 * 
 * @author Chris Gelon (cgelon)
 */
public class LocationDialogFragment extends DialogFragment {

  /**
   * The key in the argument bundle to grab the location that the row will be
   * added at.
   */
  public static final String LOCATION_KEY = "locationkey";
  /**
   * The key in the argument bundle to grab the mapping from element names to
   * values.
   */
  public static final String ELEMENT_NAME_TO_VALUE_KEY = "elementnametovaluekey";
  
  private String _location;
  private ArrayList<String> _mappingList;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle bundle = getArguments();
    String location = bundle.getString(LOCATION_KEY);
    final Map<String, String> mapping = getElementNameToValueMap(bundle.getStringArrayList(ELEMENT_NAME_TO_VALUE_KEY));
    if (location != null) {
      // Use the Builder class for convenient dialog construction
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage("Would you like to add a row at: " + location + "?")
          .setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              ((TableActivity) getActivity()).addRow(mapping);
            }
          }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              // User cancelled the dialog
            }
          });
      return builder.create();
    }
    return null;
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(LOCATION_KEY, _location);
    outState.putStringArrayList(ELEMENT_NAME_TO_VALUE_KEY, _mappingList);
  }

  /**
   * There is no way to store a map in a bundle, so I had to store it as a list,
   * alternating the key and the value. This recreates the map from the bundle.
   */
  private Map<String, String> getElementNameToValueMap(List<String> strings) {
    Map<String, String> elementNameToValue = new HashMap<String, String>();
    for (int i = 0; i < strings.size(); i += 2) {
      elementNameToValue.put(strings.get(i), strings.get(i + 1));
    }
    return elementNameToValue;
  }
}
