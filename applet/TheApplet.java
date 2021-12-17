package applet;

import javacard.framework.*;

public class TheApplet extends Applet {
	private static final byte READ_FILE_FROM_CARD							= (byte)0x06;
	private static final byte LIST_FILES_FROM_CARD							= (byte)0x05;
	private static final byte WRITE_FILE_TO_CARD							= (byte)0x04;
	private static final byte COMPARE_FILES_FROM_CARD						= (byte)0x03;
	private static final byte UNCIPHER_FILE_BY_CARD							= (byte)0x02;
	private static final byte CIPHER_FILE_BY_CARD							= (byte)0x01;

	private final static short SW_VERIFICATION_FAILED       = (short) 0x6300; 
    private final static short SW_PIN_VERIFICATION_REQUIRED = (short) 0x6301;
	private final static short SW_NAME_REQUIRED 			= (short) 0x6302;


	private final static short NVRSIZE      = (short) 16384;
	private static byte[] NVR               = new byte[NVRSIZE];

	private static final byte DMS = (byte) 0x08;
	private static short nbChunksOffsetInNVR;
	
	private OwnerPIN readPin;
	private OwnerPIN writePin;

	private boolean pinSecurity;



	protected TheApplet() {
		/*byte[] writePinCode = {(byte)0x31,(byte)0x31,(byte)0x31,(byte)0x31};
		byte[] readPinCode = {(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30};


		this.writePin = new OwnerPIN((byte) 3, (byte) 8);
		this.readPin = new OwnerPIN((byte) 3, (byte) 8);

		this.writePin.update(writePinCode, (short) 0, (byte) 4);
		this.readPin.update(readPinCode, (short) 0, (byte) 4);

		this.pinSecurity = false;*/

		this.register();
	}


	public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
		new TheApplet();
	} 


	public boolean select() {
		return true;
		//return !(pin.getTriesRemaining() == 0);
	} 


	public void deselect() {
		//pin.reset();
	}


	public void process(APDU apdu) throws ISOException {
		if(selectingApplet() == true)
			return;

		byte[] buffer = apdu.getBuffer();

		switch(buffer[1]) {
			case CIPHER_FILE_BY_CARD:
				cipherFileByCard(apdu);
			break;

			case UNCIPHER_FILE_BY_CARD:
				uncipherFileByCard(apdu);
			break;

			case COMPARE_FILES_FROM_CARD:
				compareFilesFromCard(apdu);
			break;

			case WRITE_FILE_TO_CARD:
				writeFileToCard(apdu);
			break;

			case LIST_FILES_FROM_CARD:
				listFilesFromCard(apdu);
			break;

			case READ_FILE_FROM_CARD:
				readFileFromCard(apdu);
			break;

			default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	void verify(byte[] buffer, OwnerPIN pin) {
		if(!pin.check(buffer, (byte)5, buffer[4])) 
			ISOException.throwIt(SW_VERIFICATION_FAILED);
	}

	void uncipherFileByCard(APDU apdu) {

	}


	void cipherFileByCard(APDU apdu) {

	}

	void compareFilesFromCard(APDU apdu) {

	}

	void writeFileToCard(APDU apdu) {
		/*if (!this.writePin.isValidated() && pinSecurity)
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else if (NVR[0] == 0)
			ISOException.throwIt(SW_NAME_REQUIRED);*/

		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();

		//byte firstFileSize = NVR[0];

		byte P1 = buffer[2];
		byte P2 = buffer[3];

		if (P1 == 0) {
			short filenameOffset = (short) 4;
			short filenameSize = buffer[filenameOffset];
			Util.arrayCopy(buffer, (short) filenameOffset, NVR, (short) 0, (short) (filenameSize + 1));

			nbChunksOffsetInNVR = (short) (filenameSize + 1);
			NVR[nbChunksOffsetInNVR] = (byte) 0;
		} else if (P1 == 1) {
			short dataSizeOffset = (short) 4;
			short dataSize = buffer[dataSizeOffset];

			short offsetInNVR = (short) ((nbChunksOffsetInNVR + 2) + (DMS * P2));

			Util.arrayCopy(buffer, (short) (dataSizeOffset + 1), NVR, offsetInNVR, (short) dataSize);

			++NVR[nbChunksOffsetInNVR];
		} else if (P1 == 2) {
			short dataSizeOffset = (short) 4;
			short dataSize = buffer[dataSizeOffset];

			short offsetInNVR = (short) ((nbChunksOffsetInNVR + 2) + (DMS * P2));

			Util.arrayCopy(buffer, (short) (dataSizeOffset + 1), NVR, offsetInNVR, (short) dataSize);

			++NVR[nbChunksOffsetInNVR];

			NVR[(short) (nbChunksOffsetInNVR + 1)] = (byte) dataSize;
		}
		
		/*apdu.setIncomingAndReceive();
		buffer = apdu.getBuffer();

		P1 = buffer[2];
		P2 = buffer[3];

		short lastDataSizeOffset = (short) 4;
		byte lastDataSize = buffer[lastDataSizeOffset];
		NVR[(short) (nbChunksOffsetInNVR + 1)] = lastDataSize;*/

		/*while(P1 != 2) {
			apdu.setIncomingAndReceive();
			buffer = apdu.getBuffer();

			P1 = buffer[2];
			P2 = buffer[3];
			short dataSizeOffset = (short) 4;
			short dataSize = buffer[dataSizeOffset];

			short offsetInNVR = (short) ((nbChunksOffsetInNVR + 2) + (DMS * P2));

			Util.arrayCopy(buffer, (short) (dataSizeOffset + 1), NVR, offsetInNVR, (short) dataSize);

			++NVR[nbChunksOffsetInNVR];

			if (P1 == 2)
				NVR[(short) (nbChunksOffsetInNVR + 1)] = (byte) dataSize;
		}*/
	}

	void listFilesFromCard(APDU apdu) {

	}

	void readFileFromCard(APDU apdu) {
		/*if (!this.writePin.isValidated() && pinSecurity)
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else if (NVR[0] == 0)
			ISOException.throwIt(SW_NAME_REQUIRED);*/

	}

	/*void updateWritePIN(APDU apdu) {
		if (!this.writePin.isValidated() && pinSecurity)
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);

		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();

		byte[] pinCode = new byte[4];
		Util.arrayCopy(buffer, (short) 5, pinCode, (short) 0, (short) buffer[4]);

		this.writePin.update(pinCode, (short) 0, (byte) buffer[4]);
	}


	void updateReadPIN(APDU apdu) {
		if (!this.writePin.isValidated() && pinSecurity)
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);

		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		
		byte[] pinCode = new byte[4];
		Util.arrayCopy(buffer, (short) 5, pinCode, (short) 0, (short) buffer[4]);

		this.readPin.update(pinCode, (short) 0, (byte) buffer[4]);
	}


	void displayPINSecurity(APDU apdu) {
		byte[] buffer = apdu.getBuffer();

		buffer[0] = (byte) (this.pinSecurity ? 1 : 0);
		buffer[1] = 0;

		apdu.setOutgoingAndSend((short) 0, (short) 2);
	}


	void desactivateActivatePINSecurity(APDU apdu) {
		this.pinSecurity = !this.pinSecurity;
	}


	void enterReadPIN(APDU apdu) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();

		verify(buffer, this.readPin);
	}


	void enterWritePIN(APDU apdu) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();

		verify(buffer, this.writePin);
	}


	void readNameFromCard(APDU apdu) {
		if (!this.readPin.isValidated() && pinSecurity)
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);

		byte[] buffer = apdu.getBuffer();

		Util.arrayCopy(NVR, (short) 1, buffer, (short) 0, NVR[0]);
		apdu.setOutgoingAndSend((short) 0, NVR[0]);
	}


	void writeNameToCard(APDU apdu) {
		if (!this.writePin.isValidated() && pinSecurity)
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);


		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		Util.arrayCopy(buffer, (short) 4, NVR, (short) 0, (short) (buffer[4] + 1));
	}*/
}