package it.unica.enrico.utilis;

public abstract class Constants {

        public static enum  Status {
            OWN_TRANSACTION,
            NO_PUBLIC_KEY,
            INCORRECT_NONCE,
            FUTURE_BLOCK,
            BAD_HASH,
            BAD_SIGNATURE,
            BAD_INPUTS,
            DUPLICATE,
            SUCCESS,
            UNKNOWN
        }

}
