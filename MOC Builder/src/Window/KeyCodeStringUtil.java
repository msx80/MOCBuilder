package Window;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

public class KeyCodeStringUtil {
	public static String getKeyCodeString(KeyEvent arg0){
		String str = "";
		if ((arg0.stateMask & SWT.CTRL) != 0)
			if (str.length() == 0)
				str += "Ctrl";
			else
				str += "+Ctrl";
		if ((arg0.stateMask & SWT.ALT) != 0)
			if (str.length() == 0)
				str += "Alt";
			else
				str += "+Alt";
		if ((arg0.stateMask & SWT.SHIFT) != 0)
			if (str.length() == 0)
				str += "Shift";
			else
				str += "+Shift";

		if (str.length() == 0) {
			str += keyCode(arg0.keyCode);
		} else
			str += "+" + keyCode(arg0.keyCode);
		return str;
	}
	private static String keyCode(int keyCode) {
		switch (keyCode) {
		/* Keyboard and Mouse Masks */
		case SWT.ALT:
			return "Alt";
		case SWT.SHIFT:
			return "Shift";
		case SWT.CONTROL:
			return "Ctrl";
		case SWT.COMMAND:
			return "COMMAND";

			/* Non-Numeric Keypad Keys */
		case SWT.ARROW_UP:
			return "ARROW_UP";
		case SWT.ARROW_DOWN:
			return "ARROW_DOWN";
		case SWT.ARROW_LEFT:
			return "ARROW_LEFT";
		case SWT.ARROW_RIGHT:
			return "ARROW_RIGHT";
		case SWT.PAGE_UP:
			return "PAGE_UP";
		case SWT.PAGE_DOWN:
			return "PAGE_DOWN";
		case SWT.HOME:
			return "HOME";
		case SWT.END:
			return "END";
		case SWT.INSERT:
			return "INSERT";

			/* Virtual and Ascii Keys */
		case SWT.BS:
			return "BS";
		case SWT.CR:
			return "CR";
		case SWT.DEL:
			return "DEL";
		case SWT.ESC:
			return "ESC";
		case SWT.LF:
			return "LF";
		case SWT.TAB:
			return "TAB";
		case SWT.SPACE:
			return "SPACE";

			/* Functions Keys */
		case SWT.F1:
			return "F1";
		case SWT.F2:
			return "F2";
		case SWT.F3:
			return "F3";
		case SWT.F4:
			return "F4";
		case SWT.F5:
			return "F5";
		case SWT.F6:
			return "F6";
		case SWT.F7:
			return "F7";
		case SWT.F8:
			return "F8";
		case SWT.F9:
			return "F9";
		case SWT.F10:
			return "F10";
		case SWT.F11:
			return "F11";
		case SWT.F12:
			return "F12";
		case SWT.F13:
			return "F13";
		case SWT.F14:
			return "F14";
		case SWT.F15:
			return "F15";

			/* Numeric Keypad Keys */
		case SWT.KEYPAD_ADD:
			return "KEYPAD_ADD";
		case SWT.KEYPAD_SUBTRACT:
			return "KEYPAD_SUBTRACT";
		case SWT.KEYPAD_MULTIPLY:
			return "KEYPAD_MULTIPLY";
		case SWT.KEYPAD_DIVIDE:
			return "KEYPAD_DIVIDE";
		case SWT.KEYPAD_DECIMAL:
			return "KEYPAD_DECIMAL";
		case SWT.KEYPAD_CR:
			return "KEYPAD_CR";
		case SWT.KEYPAD_0:
			return "KEYPAD_0";
		case SWT.KEYPAD_1:
			return "KEYPAD_1";
		case SWT.KEYPAD_2:
			return "KEYPAD_2";
		case SWT.KEYPAD_3:
			return "KEYPAD_3";
		case SWT.KEYPAD_4:
			return "KEYPAD_4";
		case SWT.KEYPAD_5:
			return "KEYPAD_5";
		case SWT.KEYPAD_6:
			return "KEYPAD_6";
		case SWT.KEYPAD_7:
			return "KEYPAD_7";
		case SWT.KEYPAD_8:
			return "KEYPAD_8";
		case SWT.KEYPAD_9:
			return "KEYPAD_9";
		case SWT.KEYPAD_EQUAL:
			return "KEYPAD_EQUAL";

			/* Other keys */
		case SWT.CAPS_LOCK:
			return "CAPS_LOCK";
		case SWT.NUM_LOCK:
			return "NUM_LOCK";
		case SWT.SCROLL_LOCK:
			return "SCROLL_LOCK";
		case SWT.PAUSE:
			return "PAUSE";
		case SWT.BREAK:
			return "BREAK";
		case SWT.PRINT_SCREEN:
			return "PRINT_SCREEN";
		case SWT.HELP:
			return "HELP";
		}
		return character((char) keyCode);
	}

	private static String character(char character) {
		switch (character) {
		case 0:
			return "\\0";
		case SWT.BS:
			return "\\b";
		case SWT.CR:
			return "\\r";
		case SWT.DEL:
			return "DEL";
		case SWT.ESC:
			return "ESC";
		case SWT.LF:
			return "\\n";
		case SWT.TAB:
			return "\\t";
		}
		return "" + character;
	}
}
