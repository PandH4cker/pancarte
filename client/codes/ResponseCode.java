package client.codes;

public enum ResponseCode {
    OK("90 00", "Ok"),
    SW_VERIFICATION_FAILED("63 00", "Verification Failed"),
    SW_PIN_VERIFICATION_REQUIRED("63 01", "PIN Verification Required"),
    SW_EXCEEDED_MAX_SIZE("6F 00", "Exceeded Max Size"),
    UNKNOWN("", "Unknown Response");

    private String responseCode;
    private String response;

    ResponseCode(final String responseCode, final String response) {
        this.responseCode = responseCode;
        this.response = response;
    }

    public static ResponseCode fromString(final String responseCode) {
        for (ResponseCode rc : ResponseCode.values())
            if (responseCode.equals(rc.responseCode))
                return rc;
        return UNKNOWN;
    }

    public String getResponseCode() {
        return this.responseCode;
    }

    @Override
    public String toString() {
        return this.response;
    }
}