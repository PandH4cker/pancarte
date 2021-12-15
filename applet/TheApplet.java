package applet;

import javacard.framework.*;

public class TheApplet extends Applet {
	private static final byte UPDATECARDKEY						= (byte)0x14;
	private static final byte UNCIPHERFILEBYCARD				= (byte)0x13;
	private static final byte CIPHERFILEBYCARD					= (byte)0x12;
	private static final byte CIPHERANDUNCIPHERNAMEBYCARD		= (byte)0x11;
	private static final byte READFILEFROMCARD					= (byte)0x10;
	private static final byte WRITEFILETOCARD					= (byte)0x09;
	private static final byte UPDATEWRITEPIN					= (byte)0x08;
	private static final byte UPDATEREADPIN						= (byte)0x07;
	private static final byte DISPLAYPINSECURITY				= (byte)0x06;
	private static final byte DESACTIVATEACTIVATEPINSECURITY	= (byte)0x05;
	private static final byte ENTERREADPIN						= (byte)0x04;
	private static final byte ENTERWRITEPIN						= (byte)0x03;
	private static final byte READNAMEFROMCARD					= (byte)0x02;
	private static final byte WRITENAMETOCARD					= (byte)0x01;

	private final static short SW_VERIFICATION_FAILED       = (short) 0x6300; 
    private final static short SW_PIN_VERIFICATION_REQUIRED = (short) 0x6301;
	private final static short SW_NAME_REQUIRED 			= (short) 0x6302;


	private final static short NVRSIZE      = (short) 16384;
	private static byte[] NVR               = new byte[NVRSIZE];
	

	private OwnerPIN readPin;
	private OwnerPIN writePin;

	private boolean pinSecurity;



	protected TheApplet() {
		byte[] writePinCode = {(byte)0x31,(byte)0x31,(byte)0x31,(byte)0x31};
		byte[] readPinCode = {(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30};


		this.writePin = new OwnerPIN((byte) 3, (byte) 8);
		this.readPin = new OwnerPIN((byte) 3, (byte) 8);

		this.writePin.update(writePinCode, (short) 0, (byte) 4);
		this.readPin.update(readPinCode, (short) 0, (byte) 4);

		this.pinSecurity = false;

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
			case UPDATECARDKEY: 
				updateCardKey(apdu); 
				break;
			case UNCIPHERFILEBYCARD: 
				uncipherFileByCard(apdu); 
				break;
			case CIPHERFILEBYCARD: 
				cipherFileByCard(apdu); 
				break;
			case CIPHERANDUNCIPHERNAMEBYCARD: 
				cipherAndUncipherNameByCard(apdu); 
				break;
			case READFILEFROMCARD: 
				readFileFromCard(apdu); 
				break;
			case WRITEFILETOCARD: 
				writeFileToCard(apdu); 
				break;
			case UPDATEWRITEPIN: 
				updateWritePIN(apdu); 
				break;
			case UPDATEREADPIN: 
				updateReadPIN(apdu); 
				break;
			case DISPLAYPINSECURITY: 
				displayPINSecurity(apdu); 
				break;
			case DESACTIVATEACTIVATEPINSECURITY: 
				desactivateActivatePINSecurity(apdu); 
				break;
			case ENTERREADPIN: 
				enterReadPIN(apdu); 
				break;
			case ENTERWRITEPIN: 
				enterWritePIN(apdu); 
				break;
			case READNAMEFROMCARD: 
				readNameFromCard(apdu); 
				break;
			case WRITENAMETOCARD: 
				writeNameToCard(apdu); 
				break;
			default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	void verify(byte[] buffer, OwnerPIN pin) {
		if(!pin.check(buffer, (byte)5, buffer[4])) 
			ISOException.throwIt(SW_VERIFICATION_FAILED);
	}


	void updateCardKey(APDU apdu) {
	}


	void uncipherFileByCard(APDU apdu) {
	}


	void cipherFileByCard(APDU apdu) {
	}


	void cipherAndUncipherNameByCard(APDU apdu) {
	}


	void readFileFromCard(APDU apdu) {
		if (!this.writePin.isValidated() && pinSecurity)
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else if (NVR[0] == 0)
			ISOException.throwIt(SW_NAME_REQUIRED);

	}


	void writeFileToCard(APDU apdu) {
		if (!this.writePin.isValidated() && pinSecurity)
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else if (NVR[0] == 0)
			ISOException.throwIt(SW_NAME_REQUIRED);

		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();

		byte nameLength = NVR[0];
		byte P1 = buffer[2];
		byte P2 = buffer[3];
		byte dataLength = buffer[4] + 1;

		short fileOffset = nameLength + 1;
		short chunkOffset = fileOffset + NVR[fileOffset];
		short lastChunkSize = chunkOffset + 1;
		short currentChunkOffset = (lastChunkSize + 1) * P2;

		if (P1 == 1)
			Util.arrayCopy(buffer, (short) 4, NVR, fileOffset, (short) dataLength);
		else {
			Util.arrayCopy(buffer, (short) 4, NVR, currentChunkOffset, (short) dataLength);
			++NVR[chunkOffset];
			if (P1 == 2) 
				NVR[lastChunkSize] = dataLength;
		}
	}


	void updateWritePIN(APDU apdu) {
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
	}
}