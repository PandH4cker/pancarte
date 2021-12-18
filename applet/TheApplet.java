package applet;

import javacard.framework.*;

public class TheApplet extends Applet {
	private static final byte READ_FILE_FROM_CARD							= (byte)0x06;
	private static final byte LIST_FILES_FROM_CARD							= (byte)0x05;
	private static final byte WRITE_FILE_TO_CARD							= (byte)0x04;
	private static final byte COMPARE_FILES_FROM_CARD						= (byte)0x03;
	private static final byte UNCIPHER_FILE_BY_CARD							= (byte)0x02;
	private static final byte CIPHER_FILE_BY_CARD							= (byte)0x01;

	private final static short SW_NO_MORE_MEMORY_AVAILABLE 	= (short) 0x6300;
	private final static short SW_FILE_NUMBER_ERROR 		= (short) 0x6301;



	private final static short NVRSIZE      = (short) 16384;
	private static byte[] NVR               = new byte[NVRSIZE];

	private static final byte DMS = (byte) 0x80;
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

	private short getWritableOffset(byte[] buffer) {
		for (short i = 0; i < buffer.length; ++i)
			if (buffer[i] == 0)
				return i;
		return (short) -1;
	}

	private byte[] getNthFile(short nth) {
		short currOffset = 0, dataSize = 0;
		while (NVR[currOffset] != 0) {
			short chunkOffset = (short) (currOffset + (NVR[currOffset] + 1));
			dataSize = (short) ((NVR[currOffset] + 1) + ((NVR[chunkOffset] - 1) * (DMS & 0xFF)) + NVR[(short) (chunkOffset + 1)] + 2);

			if (--nth == 0) {
				byte[] file = new byte[dataSize];
				Util.arrayCopy(NVR, (short) currOffset, file, (short) 0, (short) dataSize);
				return file;
			}

			currOffset += dataSize;
			if (currOffset >= NVR.length)
				ISOException.throwIt(SW_FILE_NUMBER_ERROR);
		}
		ISOException.throwIt(SW_FILE_NUMBER_ERROR);
		return null; // UNREACHABLE
	}

	private short getNumberOfFile() {
		short currOffset = 0, dataSize = 0, fileCounter = 0;
		while (NVR[currOffset] != 0) {
			short chunkOffset = (short) (currOffset + (NVR[currOffset] + 1));
			dataSize = (short) ((NVR[currOffset] + 1) + ((NVR[chunkOffset] - 1) * (DMS & 0xFF)) + NVR[(short) (chunkOffset + 1)] + 2);

			currOffset += dataSize;
			++fileCounter;
			if (currOffset >= NVR.length)
				return fileCounter;
		}
		return fileCounter;
	}

	/*private short getChunkOffset(byte[] buffer) {
		short i = 0;
		while(i < buffer.length) {
			i += buffer[i] + 1;
			if (buffer[i] == 0)
				return i;
			else {
				short lastDataSize = buffer[i + 1];
				i += (buffer[i] - 1) * DMS + lastDataSize;
			}
		}
	}*/

	void uncipherFileByCard(APDU apdu) {

	}


	void cipherFileByCard(APDU apdu) {

	}

	void compareFilesFromCard(APDU apdu) {

	}

	void writeFileToCard(APDU apdu) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();

		byte P1 = buffer[2];
		byte P2 = buffer[3];

		if (P1 == 0) {
			short filenameOffset = (short) 4;
			short filenameSize = buffer[filenameOffset];

			short writableOffsetInNVR = getWritableOffset(NVR);
			if (writableOffsetInNVR == -1)
				ISOException.throwIt(SW_NO_MORE_MEMORY_AVAILABLE);

			Util.arrayCopy(buffer, (short) filenameOffset, NVR, (short) writableOffsetInNVR, (short) (filenameSize + 1));

			nbChunksOffsetInNVR = (short) (writableOffsetInNVR + filenameSize + 1);
			NVR[nbChunksOffsetInNVR] = (byte) 0;
		} else if (P1 == 1 || P1 == 2) {
			short dataSizeOffset = (short) 4;
			short dataSize = (short) (buffer[dataSizeOffset] & 0xFF);

			short offsetInNVR = (short) ((nbChunksOffsetInNVR + 2) + ((DMS & 0xFF) * (P2 - 1)));

			Util.arrayCopy(buffer, (short) (dataSizeOffset + 1), NVR, offsetInNVR, (short) dataSize);

			++NVR[nbChunksOffsetInNVR];

			if (P1 == 2)
				NVR[(short) (nbChunksOffsetInNVR + 1)] = (byte) dataSize;
		}		
	}

	void listFilesFromCard(APDU apdu) {

	}

	void readFileFromCard(APDU apdu) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();

		byte[] file = getNthFile(buffer[5]);
		Util.arrayCopy(file, (short) 0, buffer, (short) 0, (short) file.length);

		apdu.setOutgoingAndSend((short) 0, (short) file.length);
	}
}