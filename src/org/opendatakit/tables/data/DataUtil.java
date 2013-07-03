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
package org.opendatakit.tables.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.utils.UTMConverter;

import android.content.Context;


public class DataUtil {

    private static final DateTimeFormatter DB_DATETIME_FORMATTER =
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZoneUTC();

    private static final String[] USER_FULL_DATETIME_PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ", // ODK Collect format
        "M/d/yy h:mm:ssa",
        "M/d/yy HH:mm:ss",
        "M/d/yyyy h:mm:ssa",
        "M/d/yyyy HH:mm:ss",
        "M/d h:mm:ssa",
        "M/d HH:mm:ss",
        "d h:mm:ssa",
        "d HH:mm:ss",
        "E h:mm:ssa",
        "E HH:mm:ss",
        "HH:mm:ss.SSSZ" // ODK Collect format for time
    };
    private static final String[][] USER_PARTIAL_DATETIME_PATTERNS = {
        {
            // minute
            "M/d/yy h:mma",
            "M/d/yy HH:mm",
            "M/d/yyyy h:mma",
            "M/d/yyyy HH:mm",
            "M/d h:mma",
            "M/d HH:mm",
            "d h:mma",
            "d HH:mm",
            "E h:mma",
            "E HH:mm"
        },
        {
            // hour
            "M/d/yy ha",
            "M/d/yy HH",
            "M/d/yyyy ha",
            "M/d/yyyy HH",
            "M/d ha",
            "M/d HH",
            "d ha",
            "d HH",
            "E ha",
            "E HH"
        },
        {
            // day
            "yyyy-MM-dd", // ODK Collect format for date
            "M/d/yy",
            "M/d/yyyy",
            "M/d",
            "d",
            "E"
        }
    };
    private static final int[] USER_INTERVAL_DURATIONS = {60, 3600, 86400};

    private static final Pattern USER_DURATION_FORMAT =
        Pattern.compile("(\\d+)(s|m|h|d)");

    private static final Pattern USER_NOW_RELATIVE_FORMAT =
        Pattern.compile("^now\\s*(-|\\+)\\s*(\\d+(s|m|h|d))$");

    private static final Pattern USER_LOCATION_LAT_LON_FORMAT =
        Pattern.compile("^\\s*\\(?(-?(\\d+|\\.\\d+|\\d+\\.\\d+))," +
                "\\s*(-?(\\d+|\\.\\d+|\\d+\\.\\d+))\\)?\\s*$");
    private static final Pattern USER_LOCATION_LAT_LON_ALT_ACC_FORMAT =
        Pattern.compile("^\\s*\\(?(-?(\\d+|\\.\\d+|\\d+\\.\\d+))" +
                "\\s+(-?(\\d+|\\.\\d+|\\d+\\.\\d+))" +
                "(\\s+(-?(\\d+|\\.\\d+|\\d+\\.\\d+))" +
                "\\s+(-?(\\d+|\\.\\d+|\\d+\\.\\d+)))?" + "\\)?\\s*$");
    private static final Pattern USER_LOCATION_UTM_COMMA_FORMAT =
        Pattern.compile("(-?(\\d+|\\.\\d+|\\d+\\.\\d+)),\\s*" +
                "(-?(\\d+|\\.\\d+|\\d+\\.\\d+)),\\s*(\\d+),\\s*(n|N|s|S)");
    private static final Pattern USER_LOCATION_UTM_SPACE_FORMAT =
        Pattern.compile("(\\d+)(n|N|s|S)\\s+(-?(\\d+|\\.\\d+|\\d+\\.\\d+))" +
                "\\s+(-?(\\d+|\\.\\d+|\\d+\\.\\d+))");

    private static final String USER_SHORT_FORMAT = "M/d h:mma";
    private static final String USER_LONG_FORMAT = "M/d/yyyy h:mm:ssa";

    private static DataUtil du;

    private final Locale locale;
    private final DateTimeZone tz;
    private final DateTimeFormatter userFullParser;
    private final DateTimeFormatter[] userPartialParsers;
    private final DateTimeFormatter userShortFormatter;
    private final DateTimeFormatter userLongFormatter;

    public static DataUtil getDefaultDataUtil() {
        if (du == null) {
            du = new DataUtil();
        }
        return du;
    }

    private DataUtil() {
        this(Locale.ENGLISH, TimeZone.getDefault());
    }

    public DataUtil(Locale locale, TimeZone tz) {
        this.locale = locale;
        this.tz = DateTimeZone.forTimeZone(tz);
        DateTimeFormatterBuilder fpBuilder = new DateTimeFormatterBuilder();
        for (String pattern : USER_FULL_DATETIME_PATTERNS) {
            DateTimeFormatter f = DateTimeFormat.forPattern(pattern);
            fpBuilder.appendOptional(f.getParser());
        }
        userFullParser = fpBuilder.toFormatter()
                .withLocale(locale).withZone(this.tz);
        userPartialParsers =
            new DateTimeFormatter[USER_PARTIAL_DATETIME_PATTERNS.length];
        for (int i = 0; i < USER_PARTIAL_DATETIME_PATTERNS.length; i++) {
            DateTimeFormatterBuilder dtfb = new DateTimeFormatterBuilder();
            for (String pattern : USER_PARTIAL_DATETIME_PATTERNS[i]) {
                DateTimeFormatter f = DateTimeFormat.forPattern(pattern);
                dtfb.appendOptional(f.getParser());
            }
            userPartialParsers[i] = dtfb.toFormatter()
                    .withLocale(locale).withZone(this.tz);
        }
        userShortFormatter = DateTimeFormat.forPattern(USER_SHORT_FORMAT);
        userLongFormatter = DateTimeFormat.forPattern(USER_LONG_FORMAT);
    }

    /**
     * Helper function to construct the value string for mimeUri types.
     *
     * @param context
     * @param tp
     * @param formValues
     * @param mimeTypeBase
     * @param filename
     * @return
     */
    public String serializeAsMimeUri(Context context, TableProperties tp,
        String instanceID, String mimeTypeBase, String filename) {
      if ( filename == null ) {
        return null;
      }
      filename = filename.trim();
      if ( filename.length() == 0 ) {
        return null;
      }

      if ( filename.startsWith("{") ) {
        @SuppressWarnings("unchecked")
        Map<String, String> ref;
        try {
          ref = ODKFileUtils.mapper.readValue(filename, Map.class);
        } catch (JsonParseException e) {
          e.printStackTrace();
          throw new IllegalStateException("Unable to serialize mimeUri", e);
        } catch (JsonMappingException e) {
          e.printStackTrace();
          throw new IllegalStateException("Unable to serialize mimeUri", e);
        } catch (IOException e) {
          e.printStackTrace();
          throw new IllegalStateException("Unable to serialize mimeUri", e);
        }
        if ( ref != null && ref.containsKey("uri") && ref.containsKey("contentType")) {
          // value looks good!
          return filename; // actually a mimeUri...
        } else {
          return null;
        }
      } else {
        // should wrap this into a mimeUri
        Map<String,String> mimeuri = new HashMap<String,String>();
        int dotIdx = filename.lastIndexOf(".");
        String ext = (dotIdx == -1) ? "*" : filename.substring(dotIdx+1);
        if ( ext.length() == 0 ) {
          ext = "*";
        }
        mimeuri.put("contentType", mimeTypeBase + "/" + ext);
        if ( filename.indexOf("/") != -1) {
          // contains a path -- assume it is a filepath?
          mimeuri.put("uri", FileProvider.getAsUrl(context, new File(filename)));
        } else {
          File ifolder = new File(ODKFileUtils.getInstanceFolder(
              TableFileUtils.ODK_TABLES_APP_NAME, tp.getTableId(), instanceID));
          mimeuri.put("uri", FileProvider.getAsUrl(context, new File(ifolder, filename)));
        }
        try {
          String serializedValue = ODKFileUtils.mapper.writeValueAsString(mimeuri);
          return serializedValue;
        } catch (JsonGenerationException e) {
          e.printStackTrace();
          throw new IllegalStateException("Unable to serialize mimeUri", e);
        } catch (JsonMappingException e) {
          e.printStackTrace();
          throw new IllegalStateException("Unable to serialize mimeUri", e);
        } catch (IOException e) {
          e.printStackTrace();
          throw new IllegalStateException("Unable to serialize mimeUri", e);
        }
      }
    }

    public String validifyValue(ColumnProperties cp, String input) {
        if ( input == null ) {
          // TODO: should we check for required values?
          // null values are always accepted (???)
          return input;
        }
        if ( cp.getColumnType() == ColumnType.DATE ) {
            return validifyDateValue(input);
        } else if ( cp.getColumnType() == ColumnType.DATETIME ) {
            return validifyDateTimeValue(input);
        } else if ( cp.getColumnType() == ColumnType.TIME ) {
            return validifyTimeValue(input);
        } else if ( cp.getColumnType() == ColumnType.DATE_RANGE ) {
            return validifyDateRangeValue(input);
        } else if ( cp.getColumnType() == ColumnType.NUMBER ) {
            return validifyNumberValue(input);
        } else if ( cp.getColumnType() == ColumnType.INTEGER ) {
            return validifyIntegerValue(input);
        } else if ( cp.getColumnType() == ColumnType.MC_OPTIONS ) {
            return validifyMultipleChoiceValue(cp, input);
        } else if ( cp.getColumnType() == ColumnType.GEOPOINT ) {
            return validifyLocationValue(input);
        } else {
            return input;
        }
    }

    private String validifyDateValue(String input) {
        DateTime instant = tryParseInstant(input);
        if (instant != null) {
            return formatDateTimeForDb(instant);
        }
        Interval interval = tryParseInterval(input);
        if (interval != null) {
            return formatDateTimeForDb(interval.getStart());
        }
        return null;
    }

    private String validifyDateTimeValue(String input) {
        DateTime instant = tryParseInstant(input);
        if (instant != null) {
            return formatDateTimeForDb(instant);
        }
        Interval interval = tryParseInterval(input);
        if (interval != null) {
            return formatDateTimeForDb(interval.getStart());
        }
        return null;
    }

    private String validifyTimeValue(String input) {
    	// TODO: does this need to be different?
    	// Need to respect TimeZone. What should Date be?
        DateTime instant = tryParseInstant(input);
        if (instant != null) {
            return formatDateTimeForDb(instant);
        }
        Interval interval = tryParseInterval(input);
        if (interval != null) {
            return formatDateTimeForDb(interval.getStart());
        }
        return null;
    }

    private String validifyDateRangeValue(String input) {
        Interval interval = tryParseInterval(input);
        if (interval != null) {
            return formatIntervalForDb(interval);
        }
        return null;
    }

    private String validifyNumberValue(String input) {
        try {
            Double.parseDouble(input);
            return input;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String validifyIntegerValue(String input) {
        try {
            Integer.parseInt(input);
            return input;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String validifyMultipleChoiceValue(ColumnProperties cp,
            String input) {
        for (String opt : cp.getDisplayChoicesMap()) {
            if (opt.equalsIgnoreCase(input)) {
                return opt;
            }
        }
        return null;
    }

    private String validifyLocationValue(String input) {
      if (input == null) {
        return null;
      }
      // free-form "lat, long" string entered by user
        Matcher matcher = USER_LOCATION_LAT_LON_FORMAT.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1) + "," + matcher.group(3);
        }
        // "lat long alt acc" from ODK Collect
        matcher = USER_LOCATION_LAT_LON_ALT_ACC_FORMAT.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1) + "," + matcher.group(3);
        }
        // UTM coordinates "630084,4833438,17,N"
        matcher = USER_LOCATION_UTM_COMMA_FORMAT.matcher(input);
        if (matcher.matches()) {
            double x = Double.parseDouble(matcher.group(1));
            double y = Double.parseDouble(matcher.group(3));
            int zone = Integer.parseInt(matcher.group(5));
            String hemi = matcher.group(6);
            boolean isSouthHemi = hemi.equalsIgnoreCase("s");
            double[] latLon = UTMConverter.parseUTM(x, y, zone, isSouthHemi);
            if (latLon == null) {
                return null;
            }
            String latStr = String.format("%.5g", latLon[0]);
            String lonStr = String.format("%.5g", latLon[1]);
            return latStr + "," + lonStr;
        }
     // UTM coordinates "17N 630084 4833438"
        matcher = USER_LOCATION_UTM_SPACE_FORMAT.matcher(input);
        if (matcher.matches()) {
            double x = Double.parseDouble(matcher.group(3));
            double y = Double.parseDouble(matcher.group(5));
            int zone = Integer.parseInt(matcher.group(1));
            String hemi = matcher.group(2);
            boolean isSouthHemi = hemi.equalsIgnoreCase("s");
            double[] latLon = UTMConverter.parseUTM(x, y, zone, isSouthHemi);
            if (latLon == null) {
                return null;
            }
            String latStr = String.format("%.5g", latLon[0]);
            String lonStr = String.format("%.5g", latLon[1]);
            return latStr + "," + lonStr;
        }
        return null;
    }

    public DateTime tryParseInstant(String input) {
        input = input.trim();
        if (input.equalsIgnoreCase("now")) {
          return new DateTime();
        }
        Matcher matcher = USER_NOW_RELATIVE_FORMAT.matcher(input);
        if (matcher.matches()) {
          int delta = tryParseDuration(matcher.group(2));
          if (delta < 0) {
              return null;
          } else if (matcher.group(1).equals("-")) {
              return new DateTime().minusSeconds(delta);
          } else {
              return new DateTime().plusSeconds(delta);
          }
        }
        try {
            return userFullParser.parseDateTime(input);
        } catch (IllegalArgumentException e) {}
//        if (!locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
//            return null;
//        }
        return null;
    }

    public Interval tryParseInterval(String input) {
        for (int i = 0; i < userPartialParsers.length; i++) {
            try {
                DateTime start = userPartialParsers[i].parseDateTime(input);
                DateTime end = start.plusSeconds(USER_INTERVAL_DURATIONS[i]);
                return new Interval(start, end);
            } catch (IllegalArgumentException e) {}
        }
        if (!locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            return null;
        }
        DateTime start = new DateMidnight().toDateTime();
        boolean match = false;
        if (input.equalsIgnoreCase("today")) {
            match = true;
        } else if (input.equalsIgnoreCase("yesterday")) {
            start = start.minusDays(1);
            match = true;
        } else if (input.equalsIgnoreCase("tomorrow") ||
                input.equalsIgnoreCase("tmw")) {
            start = start.plusDays(1);
            match = true;
        }
        if (match) {
            DateTime end = start.plusDays(1);
            return new Interval(start, end);
        }
        return null;
    }

    public int tryParseDuration(String input) {
        Matcher matcher = USER_DURATION_FORMAT.matcher(input);
        if (!matcher.matches()) {
            return -1;
        }
        int quant = Integer.parseInt(matcher.group(1));
        char unit = matcher.group(2).charAt(0);
        switch (unit) {
        case 's':
            return quant;
        case 'm':
            return quant * 60;
        case 'h':
            return quant * 3600;
        case 'd':
            return quant * 86400;
        default:
            return -1;
        }
    }

    public String formatDateTimeForDb(DateTime dt) {
        return DB_DATETIME_FORMATTER.print(dt);
    }

    public String formatIntervalForDb(Interval interval) {
        return formatDateTimeForDb(interval.getStart()) + "/" +
                formatDateTimeForDb(interval.getEnd());
    }

    public String formatNowForDb() {
        return formatDateTimeForDb(new DateTime());
    }

    public DateTime parseDateTimeFromDb(String dbString) {
        return DB_DATETIME_FORMATTER.parseDateTime(dbString);
    }

    public Interval parseIntervalFromDb(String dbString) {
    	// TODO: range should not be slash-separated but stored as two columns OR json in db...
        String[] split = dbString.split("/");
        return new Interval(DB_DATETIME_FORMATTER.parseDateTime(split[0]),
                DB_DATETIME_FORMATTER.parseDateTime(split[1]));
    }

    public String formatForUserDisplay(ColumnProperties cp, String value) {
        if ( cp.getColumnType() == ColumnType.DATE ) {
            return formatLongDateTimeForUser(parseDateTimeFromDb(value));
        } else if ( cp.getColumnType() == ColumnType.DATETIME ) {
        	// TODO: do we need special conversion
            return formatLongDateTimeForUser(parseDateTimeFromDb(value));
        } else if ( cp.getColumnType() == ColumnType.TIME ) {
        	// TODO: do we need special conversion
            return formatLongDateTimeForUser(parseDateTimeFromDb(value));
        } else if ( cp.getColumnType() == ColumnType.DATE_RANGE ) {
            return formatLongIntervalForUser(parseIntervalFromDb(value));
        } else {
            return value;
        }
    }

    public String formatShortDateTimeForUser(DateTime dt) {
        return userShortFormatter.print(dt);
    }

    public String formatLongDateTimeForUser(DateTime dt) {
        return userLongFormatter.print(dt);
    }

    public String formatShortIntervalForUser(Interval interval) {
        return formatShortDateTimeForUser(interval.getStart()) + "-" +
                formatShortDateTimeForUser(interval.getEnd());
    }

    public String formatLongIntervalForUser(Interval interval) {
        return formatLongDateTimeForUser(interval.getStart()) + " - " +
                formatLongDateTimeForUser(interval.getEnd());
    }

    public double[] parseLocationFromDb(String dbString) {
    	// TODO: geopoint should not be comma-separated but stored as four columns OR json in db...
    	// TODO: note that this expects only 2 coordinates (x,y) ???
        String[] split = dbString.split(",");
        return new double[] {Double.parseDouble(split[0]),
                Double.parseDouble(split[1])};
    }
}
