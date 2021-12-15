package client.codes;

public enum CommandCode {
    WRITE_NAME_TO_CARD((byte) 0x01, "Write name to card"),
    READ_NAME_FROM_CARD((byte) 0x02, "Read name from card"),
    ENTER_WRITE_PIN((byte) 0x03, "Enter write pin"),
    ENTER_READ_PIN((byte) 0x04, "Enter read pin"),
    DESACTIVATE_ACTIVATE_PIN_SECURITY((byte) 0x05, "Desactivate/Activate pin security"),
    DISPLAY_PIN_SECURITY((byte) 0x06, "Display pin security"),
    UPDATE_READ_PIN((byte) 0x07, "Update read pin"),
    UPDATE_WRITE_PIN((byte) 0x08, "Update write pin"),
    WRITE_FILE_TO_CARD((byte) 0x09, "Write file to card"),
    READ_FILE_FROM_CARD((byte) 0x10, "Read file from card"),
    CIPHER_AND_UNCIPHER_NAME_BY_CARD((byte) 0x11, "Cipher/Uncipher name by card"),
    CIPHER_FILE_BY_CARD((byte) 0x12, "Cipher file by card"),
    UNCIPHER_FILE_BY_CARD((byte) 0x13, "Uncipher file by card"),
    UPDATE_CARD_KEY((byte) 0x14, "Update card key");

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