package client.utils;

import opencard.core.terminal.*;
import opencard.core.util.*;

public final class APDUtils {
    public static String apdu2string(APDU apdu) {
		return removeCR(HexString.hexify(apdu.getBytes()));
	}

	public static void displayAPDU(APDU apdu) {
		System.out.println(removeCR(HexString.hexify(apdu.getBytes())) + "\n");
	}

	public static void displayAPDU(CommandAPDU termCmd, ResponseAPDU cardResp) {
		System.out.println("--> Term: " + removeCR(HexString.hexify(termCmd.getBytes())));
		System.out.println("<-- Card: " + removeCR(HexString.hexify(cardResp.getBytes())));
	}

	public static String removeCR(String string) {
		return string.replace('\n', ' ');
	}
}