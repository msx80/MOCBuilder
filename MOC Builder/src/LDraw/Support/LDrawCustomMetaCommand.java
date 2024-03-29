package LDraw.Support;

import java.util.StringTokenizer;


public class LDrawCustomMetaCommand extends LDrawMetaCommand{
	String commandProgram;
	String commandName;
	String commandValue;
	
	public LDrawCustomMetaCommand (){
		super();
		commandProgram = LDrawKeywords.MOCBUILDER_CUSTOM_META;
	}

	@Override
	public void setStringValue(String newString) {
		StringTokenizer strTokenizer = new StringTokenizer(newString);
		if (strTokenizer.hasMoreTokens()) {
			strTokenizer.nextToken();
		}
		if (strTokenizer.hasMoreTokens()) {
			commandProgram = strTokenizer.nextToken();		
		}
		if (strTokenizer.hasMoreTokens()) {
			commandName = strTokenizer.nextToken();		
		}
		if (strTokenizer.hasMoreTokens()) {
			commandValue = strTokenizer.nextToken();		
		}
		commandString = newString;
	}
	@Override
	public String stringValue() {
		commandString = String.format("%s %s %s", commandProgram , commandName , commandValue);
		return super.stringValue();
	}

	public String getCommandName() {
		return commandName;
	}

	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}

	public String getCommandValue() {
		return commandValue;
	}

	public void setCommandValue(String commandValue) {
		this.commandValue = commandValue;
	}
}
