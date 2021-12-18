package client;

import java.io.*;
import java.util.Arrays;
import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;

import client.codes.CommandCode;
import client.codes.ResponseCode;

public class TheClient {
	private PassThruCardService servClient;
	private boolean loop;
	private CommandAPDU cmd;
	private ResponseAPDU resp;

	private static final boolean DISPLAY = true;

	private static final byte CLA								= (byte) 0x00;
	private static byte P1										= (byte) 0x00;
	private static byte P2										= (byte) 0x00;
	private static final byte DMS 								= (byte) 0x90;
	private static 		 byte LC								= (byte) 0x00;

	public TheClient() {
		this.loop = true;

		try {
			SmartCard.start();
			System.out.print( "Smartcard inserted?... " ); 

			CardRequest cr = new CardRequest(CardRequest.ANYCARD, null, null); 

			SmartCard sm = SmartCard.waitForCard (cr);

			if (sm != null)
				System.out.println ("got a SmartCard object!\n");
			else
				System.out.println("did not get a SmartCard object!\n");

			this.initNewCard(sm); 

			SmartCard.shutdown();
		} catch(Exception e) {
			System.out.println("TheClient error: " + e.getMessage());
		}
		System.exit(0);
	}

	private ResponseAPDU sendAPDU(CommandAPDU cmd) {
		return sendAPDU(cmd, true);
	}

	private ResponseAPDU sendAPDU(CommandAPDU cmd, boolean display) {
		ResponseAPDU result = null;
		try {
			result = this.servClient.sendCommandAPDU(cmd);
			if(display)
				displayAPDU(cmd, result);
		} catch(Exception e) {
			System.out.println("Exception caught in sendAPDU: " + e.getMessage());
			System.exit(-1);
		}
		return result;
	}


	/************************************************
	 * *********** BEGINNING OF TOOLS ***************
	 * **********************************************/


	private String apdu2string(APDU apdu) {
		return removeCR(HexString.hexify(apdu.getBytes()));
	}


	public void displayAPDU(APDU apdu) {
		System.out.println(removeCR(HexString.hexify(apdu.getBytes())) + "\n");
	}


	public void displayAPDU(CommandAPDU termCmd, ResponseAPDU cardResp) {
		System.out.println("--> Term: " + removeCR(HexString.hexify(termCmd.getBytes())));
		System.out.println("<-- Card: " + removeCR(HexString.hexify(cardResp.getBytes())));
	}


	private String removeCR(String string) {
		return string.replace('\n', ' ');
	}


	/******************************************
	 * *********** END OF TOOLS ***************
	 * ****************************************/


	private boolean selectApplet() {
		boolean cardOk = false;
		try {
			CommandAPDU cmd = new CommandAPDU(new byte[]{
					(byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x0A,
				    (byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x62, 
				    (byte)0x03, (byte)0x01, (byte)0x0C, (byte)0x06, (byte)0x01
			});
			ResponseAPDU resp = this.sendAPDU(cmd);
			if(this.apdu2string(resp).equals("90 00"))
				cardOk = true;
		} catch(Exception e) {
			System.out.println("Exception caught in selectApplet: " + e.getMessage());
			System.exit(-1);
		}
		return cardOk;
	}


	private void initNewCard(SmartCard card) {
		if(card != null)
			System.out.println("Smartcard inserted\n");
		else {
			System.out.println("Did not get a smartcard");
			System.exit(-1);
		}

		System.out.println("ATR: " + HexString.hexify(card.getCardID().getATR()) + "\n");


		try {
			this.servClient = (PassThruCardService) card.getCardService(PassThruCardService.class, true);
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}

		System.out.println("Applet selecting...");
		if(!this.selectApplet()) {
			System.out.println("Wrong card, no applet to select!\n");
			System.exit(1);
			return;
		} else 
			System.out.println("Applet selected");

		mainLoop();
	}

	void listFilesFromCard() {

	}

	void compareFilesFromCard() {

	}


	void uncipherFileByCard() {
	}


	void cipherFileByCard() {
	}

	void readFileFromCard() {
		String nthFile = readKeyboard();

		int apduLength = 6 + nthFile.length();
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.READ_FILE_FROM_CARD.getCode();
		apdu[2] = P1 = 0;
		apdu[3] = P2 = 0;
		apdu[4] = LC = (byte) 1;
		apdu[5] = (byte) (nthFile.getBytes()[0] - 0x30);

		this.cmd = new CommandAPDU(apdu);
		this.resp = this.sendAPDU(cmd, DISPLAY);

		String responseString = this.apdu2string(resp);
		ResponseCode responseCode = ResponseCode.fromString(responseString.substring(responseString.length() - 5));		
		if (responseCode == ResponseCode.OK) {
			byte[] buffer = this.resp.getBytes();
			String filename = "";
			String data = "";

			int i = 1;
			for (; i <= buffer[0]; ++i)
				filename += (char) buffer[i];

			int chunkNumberOffset = i++;
			System.out.println("Chunks number: " + buffer[chunkNumberOffset]);

			int lastDataSizeOffset = i++;
			System.out.println("Last Data Size: " + (buffer[lastDataSizeOffset] & 0xFF));

			for(; 
				i < lastDataSizeOffset + 
					1 + 
					(DMS & 0xFF) * (buffer[chunkNumberOffset] - 1) + 
					(buffer[lastDataSizeOffset] & 0xFF); 
				++i)
				data += (char) buffer[i];

			System.out.println("[+] File #"+ nthFile + " [" + filename + "]:");
			System.out.println(data);
			System.out.println("[+] File successfully retrieved !");
		} else printError(responseCode);
	}


	void writeFileToCard() {
		String filePath = readKeyboard();
		try {
			File f = new File(filePath);
			InputStream inputStream = new FileInputStream(f);
			DataInputStream dataInputStream = new DataInputStream(inputStream);

			String basename = f.getName();
			int apduLength = 5 + basename.length();
			
			byte[] apdu = new byte[apduLength];
			apdu[0] = CLA;
			apdu[1] = CommandCode.WRITE_FILE_TO_CARD.getCode();
			apdu[2] = P1 = 0;
			apdu[3] = P2 = 0;
			apdu[4] = LC = (byte) basename.length();

			System.arraycopy(basename.getBytes(), 0, apdu, 5, LC);

			this.cmd = new CommandAPDU(apdu);
			resp = this.sendAPDU(cmd, DISPLAY);

			ResponseCode responseCode = ResponseCode.fromString(this.apdu2string(resp));
			if (responseCode == ResponseCode.OK)
				System.out.println("[+] Filename " + basename + " written");
			else printError(responseCode);

			P1 = 1;
			while(dataInputStream.available() > 0) {
				int bufferLength = (DMS & 0xFF) < dataInputStream.available() ? (DMS & 0xFF) : dataInputStream.available();
				byte[] buffer = new byte[bufferLength];

				dataInputStream.readFully(buffer, 0, bufferLength);
								
				apduLength = 5 + bufferLength;
				if (!(dataInputStream.available() > 0)) 
					P1 = 2;
				++P2;
				
				apdu = new byte[apduLength];
				apdu[0] = CLA;
				apdu[1] = CommandCode.WRITE_FILE_TO_CARD.getCode();
				apdu[2] = P1;
				apdu[3] = P2;
				apdu[4] = LC = (byte) bufferLength;

				System.out.println("Buffer length: " + bufferLength);

				System.arraycopy(buffer, 0, apdu, 5, LC & 0xFF);

				this.cmd = new CommandAPDU(apdu);
				resp = this.sendAPDU(cmd, DISPLAY);

				responseCode = ResponseCode.fromString(this.apdu2string(resp));
				if (responseCode == ResponseCode.OK)
					System.out.println("[+] Added Data: " + Arrays.toString(buffer) + " to " + basename);
				else printError(responseCode);
			}

			System.out.println("[+] File written successfully !");
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}


	/*void updateWritePIN() {
		String pin = readKeyboard();
		int apduLength = pin.length() + 5;
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.UPDATE_WRITE_PIN.getCode();
		apdu[2] = P1;
		apdu[3] = P2;
		apdu[4] = LC = (byte) pin.length();

		System.arraycopy(pin.getBytes(), 0, apdu, 5, LC);

		this.cmd = new CommandAPDU(apdu);
		resp = this.sendAPDU(cmd, DISPLAY);

		ResponseCode responseCode = ResponseCode.fromString(this.apdu2string(resp));
		if (responseCode == ResponseCode.OK)
			System.out.println("[+] Write PIN Updated With " + pin);
		else printError(responseCode);
	}*/


	/*void updateReadPIN() {
		String pin = readKeyboard();
		int apduLength = pin.length() + 5;
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.UPDATE_READ_PIN.getCode();
		apdu[2] = P1;
		apdu[3] = P2;
		apdu[4] = LC = (byte) pin.length();

		System.arraycopy(pin.getBytes(), 0, apdu, 5, LC);

		this.cmd = new CommandAPDU(apdu);
		resp = this.sendAPDU(cmd, DISPLAY);

		ResponseCode responseCode = ResponseCode.fromString(this.apdu2string(resp));
		if (responseCode == ResponseCode.OK)
			System.out.println("[+] Read PIN Updated With " + pin);
		else printError(responseCode);
	}*/


	/*void displayPINSecurity() {
		int apduLength = 5;
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.DISPLAY_PIN_SECURITY.getCode();
		apdu[2] = P1;
		apdu[3] = P2;
		apdu[4] = LC = 0x00;

		this.cmd = new CommandAPDU(apdu);
		resp = this.sendAPDU(cmd, DISPLAY);

		String responseString = this.apdu2string(resp);
		ResponseCode responseCode = ResponseCode.fromString(responseString.substring(responseString.length() - 5));
		if (responseCode == ResponseCode.OK) {
			byte[] bytes = resp.getBytes();
	    	System.out.println("[+] PINSecurity: " + (bytes[0] == 1 ? "activated" : "deactivated"));
		} else printError(responseCode);
	}*/


	/*void desactivateActivatePINSecurity() {
		int apduLength = 5;
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.DESACTIVATE_ACTIVATE_PIN_SECURITY.getCode();
		apdu[2] = P1;
		apdu[3] = P2;
		apdu[4] = LC = 0x00;

		this.cmd = new CommandAPDU(apdu);
		resp = this.sendAPDU(cmd, DISPLAY);

		ResponseCode responseCode = ResponseCode.fromString(this.apdu2string(resp));
		if (responseCode == ResponseCode.OK)
			System.out.println("[+] Toggled PIN Security");
		else printError(responseCode);
	}*/


	/*void enterReadPIN() {
		String pin = readKeyboard();
		int apduLength = pin.length() + 5;
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.ENTER_READ_PIN.getCode();
		apdu[2] = P1;
		apdu[3] = P2;
		apdu[4] = LC = (byte) pin.length();

		System.arraycopy(pin.getBytes(), 0, apdu, 5, LC);

		this.cmd = new CommandAPDU(apdu);
		resp = this.sendAPDU(cmd, DISPLAY);


		ResponseCode responseCode = ResponseCode.fromString(this.apdu2string(resp));
		if (responseCode == ResponseCode.OK)
			System.out.println("[+] Read PIN Enabled");
		else printError(responseCode);
	}*/


	/*void enterWritePIN() {
		String pin = readKeyboard();
		int apduLength = pin.length() + 5;
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.ENTER_WRITE_PIN.getCode();
		apdu[2] = P1;
		apdu[3] = P2;
		apdu[4] = LC = (byte) pin.length();

		System.arraycopy(pin.getBytes(), 0, apdu, 5, LC);

		this.cmd = new CommandAPDU(apdu);
		resp = this.sendAPDU(cmd, DISPLAY);

		ResponseCode responseCode = ResponseCode.fromString(this.apdu2string(resp));
		if (responseCode == ResponseCode.OK)
			System.out.println("[+] Write PIN Enabled");
		else printError(responseCode);
	}*/


	/*void readNameFromCard() {
		int apduLength = 5;
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.READ_NAME_FROM_CARD.getCode();
		apdu[2] = P1;
		apdu[3] = P2;
		apdu[4] = LC = 0x00;

		this.cmd = new CommandAPDU(apdu);
		resp = this.sendAPDU(cmd, DISPLAY);

		String responseString = this.apdu2string(resp);
		ResponseCode responseCode = ResponseCode.fromString(responseString.substring(responseString.length() - 5));
		if (responseCode == ResponseCode.OK) {
			byte[] bytes = resp.getBytes();
			String msg = "";

			for(int i = 0; i < bytes.length - 2; ++i)
		    	msg += new StringBuffer("").append((char) bytes[i]);

	    	System.out.println("[+] " + msg + " name retrieved from the card");
		} else printError(responseCode);
	}*/


	/*void writeNameToCard() {
		String name = readKeyboard();
		int apduLength = name.length() + 5;
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.WRITE_NAME_TO_CARD.getCode();
		apdu[2] = P1;
		apdu[3] = P2;
		apdu[4] = LC = (byte) name.length();

		System.arraycopy(name.getBytes(), 0, apdu, 5, LC);

		this.cmd = new CommandAPDU(apdu);
		resp = this.sendAPDU(cmd, DISPLAY);

		ResponseCode responseCode = ResponseCode.fromString(this.apdu2string(resp));
		if (responseCode == ResponseCode.OK) 
			System.out.println("[+] " + name + " added to the name of the card");
		else printError(responseCode);
	}*/


	void exit() {
		loop = false;
	}


	void runAction(int choice) {
		switch(choice) {
			case 6: readFileFromCard(); break;
			case 5: listFilesFromCard(); break;
			case 4: writeFileToCard(); break;
			case 3: compareFilesFromCard(); break;
			case 2: uncipherFileByCard(); break;
			case 1: cipherFileByCard(); break;
			case 0: exit(); break;
			default: System.out.println("unknown choice!");
		}
	}


	String readKeyboard() {
		String result = null;

		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
			result = input.readLine();
		} catch(Exception ignored) {}

		return result;
	}


	int readMenuChoice() {
		int result = 0;

		try {
			String choice = readKeyboard();
			result = Integer.parseInt(choice);
		} catch(Exception ignored) {}

		System.out.println("");

		return result;
	}


	void printMenu() {
		System.out.println("");
		System.out.println("6: read file from card");
		System.out.println("5: list files from card");
		System.out.println("4: write file to card");
		System.out.println("3: compare files from card");
		System.out.println("2: uncipher file by card");
		System.out.println("1: cipher file by card");
		System.out.println("0: exit");
		System.out.print("--> ");
	}


	void mainLoop() {
		while(loop) {
			printMenu();
			runAction(readMenuChoice());
		}
	}

	void printError(ResponseCode responseCode) {
		System.out.println("[!] ERROR: " + responseCode);
	}


	public static void main(String[] args) throws InterruptedException {
		new TheClient();
	}
}
