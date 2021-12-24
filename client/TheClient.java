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
	private static final byte DMS 								= (byte) 0xF0;
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
		int apduLength = 5;
		byte[] apdu = new byte[apduLength];
		apdu[0] = CLA;
		apdu[1] = CommandCode.LIST_FILES_FROM_CARD.getCode();
		apdu[2] = P1 = 0;
		apdu[3] = P2 = 0;

		this.cmd = new CommandAPDU(apdu);
		this.resp = this.sendAPDU(cmd, DISPLAY);

		String responseString = HexString.hexify(
									Arrays.copyOfRange(
										resp.getBytes(), 
										resp.getBytes().length - 2, 
										resp.getBytes().length
									)
								);
		ResponseCode responseCode = ResponseCode.fromString(responseString);
		if (responseCode == ResponseCode.OK) {
			short numberOfFiles = (short) (this.resp.getBytes()[0] & 0xFF);
			if (numberOfFiles == 0)
				System.out.println("[+] The card is empty");
			else {
				System.out.println("[+] There is " + numberOfFiles + " files");
				System.out.println("ID\tFilename\t\tSize (Chunks, Last Data Size)");
				System.out.println("-------------------------------------------------------------");
				apdu[2] = P1 = (byte) 1;
				for (short i = 0; i < numberOfFiles; ++i) {
					apdu[3] = P2 = (byte) (i + 1);

					this.cmd = new CommandAPDU(apdu);
					this.resp = this.sendAPDU(cmd, false);

					responseString = HexString.hexify(
									Arrays.copyOfRange(
										resp.getBytes(), 
										resp.getBytes().length - 2, 
										resp.getBytes().length
									)
								);
					responseCode = ResponseCode.fromString(responseString);
					if (responseCode == ResponseCode.OK) {
						byte[] metadata = this.resp.getBytes();
						String filename = "";

						int index = 1;
						for (; index <= metadata[0]; ++index)
							filename += (char) metadata[index];
						
						int chunkNumberOffset = index++;
						int lastDataSizeOffset = index++;
						int dataSize = (metadata[chunkNumberOffset] - 1) * (DMS & 0xFF) + (metadata[lastDataSizeOffset] & 0xFF);

						String dataSizeStr = "";

						if (dataSize < 1000)
							dataSizeStr = "" + dataSize + " bytes";
						else if (dataSize < 10000)
							dataSizeStr = "" + dataSize/1000 + " Mb";

						// Improve here
						System.out.println(
							"" + (i + 1) + "\t" + 
							filename + (dataSize >= 100 ? "\t\t\t" : "\t\t  ") + 
							dataSizeStr + " (" + metadata[chunkNumberOffset] + ", " + (metadata[lastDataSizeOffset] & 0xFF) + ")"
						);
					} else {
						printError(responseCode);
						return;
					}
				}
			}
		} else printError(responseCode);
	}

	void compareFilesFromCard() {
		String[] userInput = readKeyboard().split(" ");
		if (userInput.length < 2)
			System.err.println("You must provide two filepaths");
		else if (isSameFiles(new File(userInput[0]), new File(userInput[1])))
			System.out.println("Contents of those files are equal");
		else
			System.out.println("Contents of those files differ");
	}


	void uncipherFileByCard() {
		String filePath = readKeyboard();
		try {
			File f = new File(filePath);
			InputStream inputStream = new FileInputStream(f);
			DataInputStream dataInputStream = new DataInputStream(inputStream);

			File outputFile = new File("Decrypted/" + f.getName() + ".dec");
			truncateFile(outputFile);

			FileOutputStream outputStream = new FileOutputStream(outputFile, true);
			while(dataInputStream.available() > 0) {
				int bufferLength = (DMS & 0xFF);
				byte[] buffer = new byte[bufferLength];

				dataInputStream.readFully(buffer, 0, bufferLength);

				int apduLength = 6 + bufferLength;
				
				byte[] apdu = new byte[apduLength];
				apdu[0] = CLA;
				apdu[1] = CommandCode.UNCIPHER_FILE_BY_CARD.getCode();
				apdu[2] = P1 = 0;
				apdu[3] = P2 = 0;
				apdu[4] = LC = (byte) bufferLength;

				System.out.println("Buffer length: " + bufferLength);

				System.arraycopy(buffer, 0, apdu, 5, LC & 0xFF);
				
				this.cmd = new CommandAPDU(apdu);
				this.resp = this.sendAPDU(cmd, DISPLAY);

				byte[] respBytes = this.resp.getBytes();

				String responseString = HexString.hexify(
					Arrays.copyOfRange(
						respBytes, 
						respBytes.length - 2, 
						respBytes.length
					)
				);
				ResponseCode responseCode = ResponseCode.fromString(responseString);
				if (responseCode == ResponseCode.OK) {
					byte[] data = Arrays.copyOfRange(respBytes, 0, respBytes.length - 2);

					if (!(dataInputStream.available() > 0))
						data = trimPKCS5Padding(data);

					outputStream.write(data);
				} else printError(responseCode);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}


	void cipherFileByCard() {
		String filePath = readKeyboard();
		try {
			File f = new File(filePath);
			InputStream inputStream = new FileInputStream(f);
			DataInputStream dataInputStream = new DataInputStream(inputStream);

			byte[] clearContent = new byte[(int) f.length()];
			dataInputStream.readFully(clearContent);

			byte[] paddedText = addPKCS5Padding(clearContent, (short) (DMS & 0xFF));
			dataInputStream = new DataInputStream(
				new ByteArrayInputStream(paddedText)
			);

			File outputFile = new File("Encrypted/" + f.getName() + ".enc");
			truncateFile(outputFile);

			FileOutputStream outputStream = new FileOutputStream(outputFile, true);
			while(dataInputStream.available() > 0) {
				int bufferLength = (DMS & 0xFF);
				byte[] buffer = new byte[bufferLength];

				dataInputStream.readFully(buffer, 0, bufferLength);
								
				int apduLength = 6 + bufferLength;
				
				byte[] apdu = new byte[apduLength];
				apdu[0] = CLA;
				apdu[1] = CommandCode.CIPHER_FILE_BY_CARD.getCode();
				apdu[2] = P1 = 0;
				apdu[3] = P2 = 0;
				apdu[4] = LC = (byte) bufferLength;

				System.out.println("Buffer length: " + bufferLength);

				System.arraycopy(buffer, 0, apdu, 5, LC & 0xFF);

				this.cmd = new CommandAPDU(apdu);
				this.resp = this.sendAPDU(cmd, DISPLAY);

				byte[] respBytes = this.resp.getBytes();

				String responseString = HexString.hexify(
					Arrays.copyOfRange(
						respBytes, 
						respBytes.length - 2, 
						respBytes.length
					)
				);
				ResponseCode responseCode = ResponseCode.fromString(responseString);
				if (responseCode == ResponseCode.OK) {
					byte[] data = Arrays.copyOfRange(respBytes, 0, respBytes.length - 2);
					outputStream.write(data);
				} else printError(responseCode);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
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
			byte[] metadata = this.resp.getBytes();
			String filename = "";
			String data = "";

			int i = 1;
			for (; i <= metadata[0]; ++i)
				filename += (char) metadata[i];

			int chunkNumberOffset = i++;
			System.out.println("Chunks number: " + metadata[chunkNumberOffset]);

			int lastDataSizeOffset = i++;
			System.out.println("Last Data Size: " + (metadata[lastDataSizeOffset] & 0xFF));


			byte[] fileData = new byte[(metadata[chunkNumberOffset] - 1) * (DMS & 0xFF) + (metadata[lastDataSizeOffset] & 0xFF)];

			i = 0;
			apdu[2] = P1 = 1;
			for (; i < metadata[chunkNumberOffset] - 1; ++i) {
				apdu[3] = P2 = (byte) (i + 1);

				this.cmd = new CommandAPDU(apdu);
				this.resp = this.sendAPDU(cmd, DISPLAY);

				responseString = HexString.hexify(
					Arrays.copyOfRange(
						resp.getBytes(), 
						resp.getBytes().length - 2, 
						resp.getBytes().length
					)
				);
				responseCode = ResponseCode.fromString(responseString);
				if (responseCode == ResponseCode.OK) {
					System.arraycopy(Arrays.copyOfRange(
						resp.getBytes(), 
						0, 
						resp.getBytes().length - 2
					), 0, fileData, (DMS & 0xFF) * i, (DMS & 0xFF));
				} else {
					printError(responseCode);
					return;
				}
			}

			apdu[2] = P1 = 2;
			apdu[3] = P2 = (byte) (i + 1);

			this.cmd = new CommandAPDU(apdu);
			this.resp = this.sendAPDU(cmd, DISPLAY);

			responseString = HexString.hexify(
				Arrays.copyOfRange(
					resp.getBytes(), 
					resp.getBytes().length - 2, 
					resp.getBytes().length
				)
			);
			responseCode = ResponseCode.fromString(responseString);
			if (responseCode == ResponseCode.OK) {
				System.arraycopy(Arrays.copyOfRange(
					resp.getBytes(), 
					0, 
					resp.getBytes().length - 2
				), 0, fileData, (DMS & 0xFF) * i, (metadata[lastDataSizeOffset] & 0xFF));
			} else {
				printError(responseCode);
				return;
			}

			i = 0;
			for (; i < fileData.length; ++i)
				data += (char) fileData[i];

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

	private byte[] addPKCS5Padding(byte[] clearContent, short blockSize) {
		byte padding = (byte) (blockSize - clearContent.length % blockSize);

		byte[] padText = new byte[(short) (padding & 0xFF)];
		Arrays.fill(padText, padding);

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			byteArrayOutputStream.write(clearContent);
			byteArrayOutputStream.write(padText);
		} catch (Exception ignored) {}

		return byteArrayOutputStream.toByteArray();
	}

	private byte[] trimPKCS5Padding(byte[] decrypted) {
		byte padding = decrypted[decrypted.length - 1];
		return Arrays.copyOfRange(
			decrypted,
			0,
			decrypted.length - (short) (padding & 0xFF)
		);
	}


	private void exit() {
		loop = false;
	}


	private void runAction(int choice) {
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

	private String readKeyboard() {
		String result = null;

		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
			result = input.readLine();
		} catch(Exception ignored) {}

		return result;
	}

	private int readMenuChoice() {
		int result = 0;

		try {
			String choice = readKeyboard();
			result = Integer.parseInt(choice);
		} catch(Exception ignored) {}

		System.out.println("");

		return result;
	}

	private void printMenu() {
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

	private void mainLoop() {
		while(loop) {
			printMenu();
			runAction(readMenuChoice());
		}
	}

	private void printError(ResponseCode responseCode) {
		System.out.println("[!] ERROR: " + responseCode);
	}

	private void truncateFile(File f) {
		try {
			new PrintWriter(f).close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean isSameFiles(File f1, File f2) {
		if (f1.length() != f2.length())
			return false;

		try {
			BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream(f1));
			BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(f2));

			int ch = 0;
			while ((ch = bis1.read()) != -1)
				if (ch != bis2.read())
					return false;
		} catch(IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public static void main(String[] args) throws InterruptedException {
		new TheClient();
	}
}
