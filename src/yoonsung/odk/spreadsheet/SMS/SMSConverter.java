package yoonsung.odk.spreadsheet.SMS;

import java.util.HashMap;

public class SMSConverter {
		
	public HashMap<String, String> parseSMS(String sms) {
		// <Column Name, Value>
		HashMap<String, String> result = new HashMap<String, String>();
		
		// Split the message
		String[] tokens = sms.split(" ");
		
		// Parse into column names and values
		int index = 0;
		while(index < tokens.length && tokens[index].startsWith("+")) { 
			String key = tokens[index];
			String val = "";
			int next = index + 1;
			while (next < tokens.length && !tokens[next].startsWith("+")) {
				val += tokens[next].trim() + " ";
				next++;
			}
			result.put(key.substring(1, key.length()), val);
			index = next;
		}
		
		return result;
	}
}
