package client.codes;

public enum CommandCode {
    CIPHER_FILE_BY_CARD((byte) 0x01, "Cipher file by card"),
    UNCIPHER_FILE_BY_CARD((byte) 0x02, "Uncipher file by card"),
    COMPARE_FILES_FROM_CARD((byte) 0x03, "Compare files from card"),
    WRITE_FILE_TO_CARD((byte) 0x04, "Write file to card"),
    LIST_FILES_FROM_CARD((byte) 0x05, "List files from card"),
    READ_FILE_FROM_CARD((byte) 0x06, "Read file from card");

    private byte code;
    private String codeName;

    CommandCode(final byte code, final String codeName) {
        this.code = code;
        this.codeName = codeName;
    }

    public byte getCode() {
        return this.code;
    }

    @Override
    public String toString() {
        return this.codeName;
    }
}