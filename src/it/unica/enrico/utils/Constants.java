package it.unica.enrico.utils;

public abstract class Constants {

        public enum  Status {
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
