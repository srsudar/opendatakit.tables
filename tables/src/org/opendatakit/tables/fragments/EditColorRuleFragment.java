package org.opendatakit.tables.fragments;

import java.util.List;

import org.opendatakit.common.android.data.ColorRule;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.preferences.EditColorPreference;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.views.ColorPickerDialog.OnColorChangedListener;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class EditColorRuleFragment extends AbsTableLevelPreferenceFragment 
    implements OnColorChangedListener {
  
  private static final String TAG =
      EditColorRuleFragment.class.getSimpleName();
  
  public static class IntentKeys {
    public static final String RULE_POSITION = "rulePosition";
  }
  
  // The keys for communicating with EditColorPreference.
  public static final String COLOR_PREF_KEY_TEXT = "textKey";
  public static final String COLOR_PREF_KEY_BACKGROUND = "backgroundKey";
  
  // These are the fields that define the rule.
  private String mElementKey;
  private String mRuleValue;
  private ColorRule.RuleType mRuleOperator;
  private Integer mTextColor;
  private Integer mBackgroundColor;
  
  private CharSequence[] mOperatorHumanFriendlyValues;
  private CharSequence[] mOperatorEntryValues;
  private CharSequence[] mColumnDisplayNames;
  private CharSequence[] mColumnElementKeys;
  private ColorRuleGroup mColorRuleGroup;
  private List<ColorRule> mColorRuleList;
  /** The position in the rule list that we are editing. */
  private int mRulePosition;
  private ColorRuleGroup.Type mColorRuleGroupType;
  
  /** A value signifying the fragment is being used to add a new rule. */
  public static final int INVALID_RULE_POSITION = -1;
  
  public EditColorRuleFragment() {
    // Required by fragments.
  }
  
  /**
   * 
   * @param colorRuleGroupType
   * @param elementKey the elementKey the rule is for, if this is in a group
   * of type {@link ColorRuleGroup.Type#COLUMN}. If it is not, pass null.
   * @return
   */
  public static EditColorRuleFragment newInstanceForNewRule(
      ColorRuleGroup.Type colorRuleGroupType,
      String elementKey) {
    Bundle arguments = new Bundle();
    arguments.putInt(IntentKeys.RULE_POSITION, INVALID_RULE_POSITION);
    IntentUtil.addColorRuleGroupTypeToBundle(arguments, colorRuleGroupType);
    IntentUtil.addElementKeyToBundle(arguments, elementKey);
    EditColorRuleFragment result = new EditColorRuleFragment();
    result.setArguments(arguments);
    return result;
  }
  
  /**
   * 
   * @param colorRuleGroupType
   * @param elementKey the element key the rule is for if this is in a group
   * of type {@link ColorRuleGroup.Type#COLUMN}. If it is not, pass null.
   * @param rulePosition the position of the rule in the group you're editing
   * @return
   */
  public static EditColorRuleFragment newInstanceForExistingRule(
      ColorRuleGroup.Type colorRuleGroupType,
      String elementKey,
      int rulePosition) {
    Bundle arguments = new Bundle();
    IntentUtil.addColorRuleGroupTypeToBundle(arguments, colorRuleGroupType);
    IntentUtil.addElementKeyToBundle(arguments, elementKey);
    arguments.putInt(IntentKeys.RULE_POSITION, rulePosition);
    EditColorRuleFragment result = new EditColorRuleFragment();
    result.setArguments(arguments);
    return result;
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.addPreferencesFromResource(R.xml.preference_color_rule_entry);
    String elementKey =
        IntentUtil.retrieveElementKeyFromBundle(savedInstanceState);
    if (elementKey == null) {
      elementKey = IntentUtil.retrieveElementKeyFromBundle(getArguments());
    }
    this.mElementKey = elementKey;
  }
 
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Bundle arguments = this.getArguments();
    ColorRuleGroup.Type colorRuleType =
        IntentUtil.retrieveColorRuleTypeFromBundle(arguments);
    int rulePosition = arguments.getInt(IntentKeys.RULE_POSITION);
    this.mColorRuleGroupType = colorRuleType;
    this.mRulePosition = rulePosition;
    if (this.mColorRuleGroupType == ColorRuleGroup.Type.COLUMN) {
      // then we also need to pull out the element key.
      String elementKey = IntentUtil.retrieveElementKeyFromBundle(arguments);
      this.mElementKey = elementKey;
    }
  }
  
  @Override
  public void onResume() {
    super.onResume();
    this.initializeStateRequiringContext();
    this.initializeAllPreferences();
  }
  
  /**
   * Set up the objects that require a context.
   */
  private void initializeStateRequiringContext() {
    TableProperties tableProperties = this.retrieveTableProperties();
    // 1) First fill in the color rule group and list.
    switch (this.mColorRuleGroupType) {
    case COLUMN:
      this.mColorRuleGroup = ColorRuleGroup.getColumnColorRuleGroup(
          tableProperties,
          this.mElementKey);
      break;
    case TABLE:
      this.mColorRuleGroup =
        ColorRuleGroup.getStatusColumnRuleGroup(tableProperties);
      break;
    case STATUS_COLUMN:
      this.mColorRuleGroup =
      ColorRuleGroup.getTableColorRuleGroup(tableProperties);
      break;
    default:
      throw new IllegalArgumentException(
          "unrecognized color rule group type: " + this.mColorRuleGroupType);
    }
    this.mColorRuleList = this.mColorRuleGroup.getColorRules();
    // 2) then fill in the starting state for the rule.
    if (this.isUnpersistedNewRule()) {
      // fill in dummy holder values
      this.mRuleValue =
          this.getActivity().getString(R.string.compared_to_value);
      this.mRuleOperator = null;
      this.mTextColor = Constants.DEFAULT_TEXT_COLOR;
      this.mBackgroundColor = Constants.DEFAULT_BACKGROUND_COLOR;
    } else {
      // fill in the values for the rule.
      // Get the rule we're editing.
      ColorRule startingRule = this.mColorRuleList.get(this.mRulePosition);
      this.mRuleValue = startingRule.getVal();
      this.mRuleOperator = startingRule.getOperator();
      this.mTextColor = startingRule.getForeground();
      this.mBackgroundColor = startingRule.getBackground();
      this.mElementKey = startingRule.getColumnElementKey();
    }
    // 3) then fill in the static things backing the dialogs.
    this.mOperatorHumanFriendlyValues = ColorRule.RuleType.getValues();
    this.mOperatorEntryValues = ColorRule.RuleType.getValues();
    List<String> elementKeys = tableProperties.getPersistedColumns();
    this.mColumnDisplayNames = new CharSequence[elementKeys.size()];
    this.mColumnElementKeys = new CharSequence[elementKeys.size()];
    for (int i = 0; i < elementKeys.size(); i++) {
      String elementKey = elementKeys.get(i);
      ColumnProperties cp = tableProperties.getColumnByElementKey(elementKey);
      this.mColumnDisplayNames[i] = cp.getLocalizedDisplayName();
      this.mColumnElementKeys[i] = elementKey;
    }
  }
  
  private void initializeAllPreferences() {
    // We have several things to do here. First we'll initialize all the
    // individual preferences.
    this.initializeColumns();
    this.initializeComparisonType();
    this.initializeRuleValue();
    this.initializeTextColor();
    this.initializeBackgroundColor();
    this.initializeSave();
  }
  
  /**
   * Return true if we are currently displaying a new rule that has not yet
   * been saved.
   * @return
   */
  boolean isUnpersistedNewRule() {
    return this.mRulePosition == INVALID_RULE_POSITION;
  }
  
  private void initializeColumns() {
    final TableProperties tableProperties = this.retrieveTableProperties();
    final ListPreference pref = this.findListPreference(
        Constants.PreferenceKeys.ColorRule.ELEMENT_KEY);
    pref.setEntries(this.mColumnDisplayNames);
    pref.setEntryValues(mColumnElementKeys);
    if (!isUnpersistedNewRule()) {
      String displayName = tableProperties.getColumnByElementKey(
          this.mColorRuleList
              .get(mRulePosition)
              .getColumnElementKey())
            .getLocalizedDisplayName();
      pref.setSummary(displayName);
      pref.setValueIndex(pref.findIndexOfValue(mElementKey));
    }
    pref.setOnPreferenceChangeListener(
        new Preference.OnPreferenceChangeListener() {
      
      @Override
      public boolean onPreferenceChange(
          Preference preference,
          Object newValue) {
        Log.d(TAG, "onPreferenceChance callback invoked for value: "
            + newValue);
        mElementKey = (String) newValue;
        String displayName =
            tableProperties
              .getColumnByElementKey(mElementKey)
              .getLocalizedDisplayName();
        pref.setSummary(displayName);
        pref.setValueIndex(
            pref.findIndexOfValue(mElementKey));
        updateStateOfSaveButton();
        // false so we don't persist the value and pass it b/w rules
        return false;
      }
    });
    // And now we have to consider that we don't display this at all.
    if (this.mColorRuleGroupType == ColorRuleGroup.Type.COLUMN) {
      this.getPreferenceScreen().removePreference(pref);
    }
  }
  
  private void initializeComparisonType() {
    final ListPreference pref = (ListPreference) this.findListPreference(
        Constants.PreferenceKeys.ColorRule.COMPARISON_TYPE);
    pref.setEntries(this.mOperatorHumanFriendlyValues);
    pref.setEntryValues(this.mOperatorEntryValues);
    // now set the correct one as checked
    if (this.mRuleOperator != null) {
      pref.setSummary(this.mRuleOperator.getSymbol());
      pref.setValueIndex(
          pref.findIndexOfValue(mRuleOperator.getSymbol()));
    }
    pref.setOnPreferenceChangeListener(
        new Preference.OnPreferenceChangeListener() {
      
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Here we want to update the rule and also persist it.
        Log.d(TAG, "onPreferenceChange callback invoked for value: " +
            (String) newValue);
        ColorRule.RuleType newOperator =
            ColorRule.RuleType.getEnumFromString((String) newValue);
        mRuleOperator = newOperator;
        preference.setSummary(mRuleOperator.getSymbol());
        pref.setValueIndex(
            pref.findIndexOfValue(mRuleOperator.getSymbol()));
        updateStateOfSaveButton();
        // false so we don't persist the value and pass it b/w rules
        return false;
      }
    });
  }
  
  private void initializeRuleValue() {
    final EditTextPreference pref = this.findEditTextPreference(
        Constants.PreferenceKeys.ColorRule.RULE_VALUE);
    if (!isUnpersistedNewRule()) {
      pref.setSummary(this.mRuleValue);
    }
    pref.setOnPreferenceChangeListener(
        new Preference.OnPreferenceChangeListener() {
      
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        String newValueStr = (String) newValue;
        pref.setSummary(newValueStr);
        mRuleValue = newValueStr;
        updateStateOfSaveButton();
        return false;
      }
    });
  }
  
  private void initializeTextColor() {
    EditColorPreference pref = (EditColorPreference)
        this.findPreference(Constants.PreferenceKeys.ColorRule.TEXT_COLOR);
    pref.initColorPickerListener(
        this,
        COLOR_PREF_KEY_TEXT,
        getActivity().getString(R.string.text_color),
        this.mTextColor);
  }
  
  private void initializeBackgroundColor() {
    EditColorPreference pref = (EditColorPreference)
        this.findPreference(
            Constants.PreferenceKeys.ColorRule.BACKGROUND_COLOR);
    pref.initColorPickerListener(
        this,
        COLOR_PREF_KEY_BACKGROUND,
        getActivity().getString(R.string.background_color),
        this.mBackgroundColor);
  }
  
  private void initializeSave() {
    Preference pref =
        this.findPreference(Constants.PreferenceKeys.ColorRule.SAVE);
    pref.setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
      
      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (preferencesDefineValidRule()) {
          saveRule();
        }
        updateStateOfSaveButton();
        return false;
      }
    });
  }
  
  private void saveRule() {
    ColorRule newRule = constructColorRuleFromState();
    if (this.isUnpersistedNewRule()) {
      this.mColorRuleList.add(newRule);
      // We just added the rule to the end of the existing list, so the new
      // position is the last item in the list.
      mRulePosition = mColorRuleList.size() - 1;
    } else {
      mColorRuleList.set(mRulePosition, newRule);
    }
    mColorRuleGroup.replaceColorRuleList(this.mColorRuleList);
    mColorRuleGroup.saveRuleList();
    updateStateOfSaveButton();  }
  
  @Override
  public void colorChanged(String key, int color) {
    if (key.equals(COLOR_PREF_KEY_TEXT)) {
      this.mTextColor = color;
    } else if (key.equals(COLOR_PREF_KEY_BACKGROUND)) {
      this.mBackgroundColor = color;
    } else {
      Log.e(TAG, "unrecognized key: " + key);
    }
    updateStateOfSaveButton();
  }
  
  /**
   * Return a new {@link ColorRule} based on the state. If the rule is not
   * valid, returns null
   * @return the rule, or null if it is not valid
   */
  ColorRule constructColorRuleFromState() {
    if (preferencesDefineValidRule()) {
      return new ColorRule(
          this.mElementKey,
          this.mRuleOperator,
          this.mRuleValue,
          this.mTextColor.intValue(),
          this.mBackgroundColor.intValue());
    } else {
      return null;
    }
  }
  
  void updateStateOfSaveButton() {
    Preference savePref =
        this.findPreference(Constants.PreferenceKeys.ColorRule.SAVE);
    boolean enableButton = this.saveButtonShouldBeEnabled();
    savePref.setEnabled(enableButton);
  }
  
  /**
   * Return true if the save button should be enabled. This depends on if the
   * rule is valid as well as if it differs from the saved version.
   * @return
   */
  boolean saveButtonShouldBeEnabled() {
    if (preferencesDefineValidRule()) {
      // A valid rule can only be saved if it differs from the previous rule.
      ColorRule userDefinedRule = constructColorRuleFromState();
      ColorRule existingRule = this.mColorRuleList.get(this.mRulePosition);
      if (userDefinedRule.equalsWithoutId(existingRule)) {
        return false;
      } else {
        return true;
      }
    } else {
      return false;
    }
  }

  
  /**
   * 
   * @return true if the current preference fields in the UI define a valid 
   * rule
   */
  boolean preferencesDefineValidRule() {
    return
        this.mElementKey != null &&
        this.mRuleValue != null &&
        this.mRuleOperator != null &&
        this.mTextColor != null &&
        this.mBackgroundColor != null;
  }
  
  /**
   * Retrieve the {@link TableLevelPreferencesActivity} associated with this
   * fragment.
   * @return
   */
  TableLevelPreferencesActivity retrieveTableLevelPreferenceActivity() {
    TableLevelPreferencesActivity result = 
        (TableLevelPreferencesActivity) this.getActivity();
    return result;
  }
  
  TableProperties retrieveTableProperties() {
    return this.retrieveTableLevelPreferenceActivity().getTableProperties();
  }
  
}
