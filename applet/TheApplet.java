package applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

public class TheApplet extends Applet {
	private static final byte READ_FILE_FROM_CARD							= (byte)0x06;
	private static final byte LIST_FILES_FROM_CARD							= (byte)0x05;
	private static final byte WRITE_FILE_TO_CARD							= (byte)0x04;
	private static final byte UNCIPHER_FILE_BY_CARD							= (byte)0x02;
	private static final byte CIPHER_FILE_BY_CARD							= (byte)0x01;

	private final static short SW_NO_MORE_MEMORY_AVAILABLE 	= (short) 0x6300;
	private final static short SW_FILE_NUMBER_ERROR 		= (short) 0x6301;

	private final static byte[] KEY = new byte[] {
		(byte) 0x13,  (byte) 0x37, 
		(byte) 0xBA,  (byte) 0xBE, 
		(byte) 0xCA,  (byte) 0xFE, 
		(byte) 0xDE,  (byte) 0xAD
	};

	private final static short NVRSIZE      = (short) 16384;
	private static byte[] NVR               = new byte[NVRSIZE];

	private static final byte DMS = (byte) 0xF0;
	private static short nbChunksOffsetInNVR;
	
	private Cipher desECBNoPadEncrypt, desECBNoPadDecrypt;

	private Key secretDESKey;

	protected TheApplet() {
		initKeyDES();
		initCiphers();

		this.register();
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
		new TheApplet();
	} 

	public boolean select() {
		return true;
	} 

	public void deselect() {}

	private void initKeyDES() {
	    try {
		    secretDESKey = KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES, false);
		    ((DESKey) secretDESKey).setKey(KEY, (short) 0);
	    } catch(Exception ignored) {}
    }

    private void initCiphers() {
	    if(secretDESKey != null) 
			try {
				desECBNoPadEncrypt = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
				desECBNoPadEncrypt.init(secretDESKey, Cipher.MODE_ENCRYPT);

				desECBNoPadDecrypt = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
				desECBNoPadDecrypt.init(secretDESKey, Cipher.MODE_DECRYPT);
			} catch(Exception ignored) {}
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
			dataSize = (short) (
				(NVR[currOffset] + 1) + 
				((NVR[chunkOffset] - 1) * (DMS & 0xFF)) + 
				(NVR[(short) (chunkOffset + 1)] & 0xFF) + 
				2
			);

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

	private byte[] getMetadata(byte[] file) {
		short filenameSize = (short) (file[0] & 0xFF);
		byte[] newBuffer = new byte[(short) ((filenameSize + 1) + 2)];

		Util.arrayCopy(file, (short) 0, newBuffer, (short) 0, (short) newBuffer.length);

		return newBuffer;
	}

	private byte[] getData(byte[] file) {
		short filenameSize = (short) (file[0] & 0xFF);
		short chunkOffset = (short) (filenameSize + 1);
		short dataSize = (short) (
			((file[chunkOffset] - 1) * (DMS & 0xFF)) +
			(file[(short) (chunkOffset + 1)] & 0xFF)
		);

		byte[] newBuffer = new byte[dataSize];
		Util.arrayCopy(file, (short) (chunkOffset + 2), newBuffer, (short) 0, (short) newBuffer.length);

		return newBuffer;
	}

	private short getNumberOfFiles() {
		short currOffset = 0, dataSize = 0, fileCounter = 0;
		while (NVR[currOffset] != 0) {
			short chunkOffset = (short) (currOffset + (NVR[currOffset] + 1));
			dataSize = (short) (
				(NVR[currOffset] + 1) + 
				((NVR[chunkOffset] - 1) * (DMS & 0xFF)) + 
				(NVR[(short) (chunkOffset + 1)] & 0xFF) + 
				2
			);

			currOffset += dataSize;
			++fileCounter;
			if (currOffset >= NVR.length)
				return fileCounter;
		}
		return fileCounter;
	}

	void uncipherFileByCard(APDU apdu) {
		apdu.setIncomingAndReceive();

        byte[] buffer = apdu.getBuffer();
        short length = (short) (buffer[4] & 0xFF);

        desECBNoPadDecrypt.doFinal(buffer, (short) 5, (short) length, buffer, (short) 0);
        apdu.setOutgoingAndSend((short) 0, length);
	}

	void cipherFileByCard(APDU apdu) {
		apdu.setIncomingAndReceive();

        byte[] buffer = apdu.getBuffer();
        short length = (short) (buffer[4] & 0xFF);

        desECBNoPadEncrypt.doFinal(buffer, (short) 5, (short) length, buffer, (short) 0);
        apdu.setOutgoingAndSend((short) 0, length);
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
				NVR[(short) (nbChunksOffsetInNVR + 1)] = buffer[dataSizeOffset];
		}		
	}

	void listFilesFromCard(APDU apdu) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();

		byte P1 = buffer[2];
		byte P2 = buffer[3];

		if (P1 == 0) {
			buffer[0] = (byte) getNumberOfFiles();
			apdu.setOutgoingAndSend((short) 0, (short) 1);
		} else if (P1 == 1) {
			byte[] metadata = getMetadata(getNthFile((short) (P2 & 0xFF)));
			Util.arrayCopy(metadata, (short) 0, buffer, (short) 0, (short) metadata.length);
			apdu.setOutgoingAndSend((short) 0, (short) metadata.length);
		}
	}

	void readFileFromCard(APDU apdu) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();

		byte P1 = buffer[2];
		byte P2 = buffer[3];

		byte[] file = getNthFile(buffer[5]);

		if (P1 == 0) {
			byte[] metadata = getMetadata(file);
			Util.arrayCopy(metadata, (short) 0, buffer, (short) 0, (short) metadata.length);
			apdu.setOutgoingAndSend((short) 0, (short) metadata.length);
		} else if (P1 == 1 || P1 == 2) {
			byte[] metadata = getMetadata(file);
			byte[] fileData = getData(file);
			Util.arrayCopy(
				fileData, 
				(short) ((DMS & 0xFF) * (P2 - 1)), 
				buffer, 
				(short) 0, 
				(short) (P1 == 2 ? metadata[(short) (metadata.length - 1)] & 0xFF : DMS & 0xFF)
			);
			apdu.setOutgoingAndSend((short) 0, (short) (P1 == 2 ? metadata[(short) (metadata.length - 1)] & 0xFF : DMS & 0xFF));
		}
	}
}