package it.unica.enrico.models;

import it.unica.enrico.utils.HashUtils;
import it.unica.enrico.utils.KeyUtils;
import java.nio.ByteBuffer;
import java.security.Signature;
import java.util.Arrays;

/* questa classe rappresenta una transazione generica
   ogni blocco conterrà una lista/array di transazioni */
public class Transaction {

    private static final int FROM_LENGTH = 4;
    private static final int TO_LENGTH = 4;
    private static final int TIMESTAMP_LENGTH = 8;
    private static final int HEADER_LENGTH  = 4;
    private static final int VALUE_LENGTH = 4;
    private static final int LENGTH_LENGTH = 4;

    String mittente;                    //chi effettua la transazione
    String destinatario;                //chi riceve la transazione
    long timestamp;                     //timestamp della transazione espresso in unix(millisecondi)
    String header;                      //riassunto della transazione;
    int valore;                         //valore della transazizone
    ByteBuffer firma;                   //firma della transazione

    Transaction[] inputs;               //ToDo
    Transaction[]  outputs;             //ToDo

    public Transaction() { }

    public Transaction(String mittente, String destinatario, String header, int valore, byte[] firma, Transaction[] inputs, Transaction[] outputs) {
        this.mittente = mittente;
        this.destinatario = destinatario;
        this.timestamp = 0;
        this.header = header;
        this.valore = valore;
        this.firma = ByteBuffer.wrap(getSig(firma));

        this.inputs = new Transaction[inputs.length];
        for (int i=0; i<inputs.length; i++)
            this.inputs[i] = inputs[i];

        this.outputs = new Transaction[outputs.length];
        for (int i=0; i<outputs.length; i++)
            this.outputs[i] = outputs[i];
    }

    private byte[] getSig(byte[] firma){
        final byte[] sig = new byte[firma.length];
        System.arraycopy(firma, 0, sig, 0, firma.length);
        return sig;
    }


    /* Crea la transazione aggregata e la firma*/
    public static Transaction newSignedTransaction(Signature firma,
                                                   String mittente, String destinatario,
                                                   String header, int valore,
                                                   Transaction[] inputs, Transaction[] outputs)
    {
        final byte[] sig = KeyUtils.signMsg(firma, header.getBytes());
        return new Transaction(mittente, destinatario, header, valore, sig, inputs, outputs);
    }

    public void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    public int getBufferLength() {
        int iLength = 0;
        for (Transaction t : inputs)
            iLength += LENGTH_LENGTH + t.getBufferLength();

        int oLength = 0;
        for (Transaction t : outputs)
            oLength += LENGTH_LENGTH + t.getBufferLength();

        int length = LENGTH_LENGTH + firma.limit() +
                     LENGTH_LENGTH + iLength +
                     LENGTH_LENGTH + oLength +
                     TIMESTAMP_LENGTH +
                     VALUE_LENGTH +
                     HEADER_LENGTH + header.getBytes().length +
                     FROM_LENGTH + mittente.getBytes().length +
                     TO_LENGTH + destinatario.getBytes().length;
        return length;
    }

    public void toBuffer(ByteBuffer buffer) {
        { // signature
            buffer.putInt(firma.limit());
            buffer.put(firma);
            firma.flip();
        }

        { // inputs
            buffer.putInt(inputs.length);
            for (Transaction t : inputs) {
                buffer.putInt(t.getBufferLength());
                t.toBuffer(buffer);
                // do not flip buffer here
            }
        }

        { // outputs
            buffer.putInt(outputs.length);
            for (Transaction t : outputs) {
                buffer.putInt(t.getBufferLength());
                t.toBuffer(buffer);
                // do not flip buffer here
            }
        }

        buffer.putLong(timestamp);
        buffer.putInt(valore);

        final int mLength = header.length();
        buffer.putInt(mLength);
        final byte[] mBytes = header.getBytes();
        buffer.put(mBytes);

        final byte[] fBytes = mittente.getBytes();
        buffer.putInt(fBytes.length);
        buffer.put(fBytes);

        final byte[] oBytes = destinatario.getBytes();
        buffer.putInt(oBytes.length);
        buffer.put(oBytes);
    }

    public void fromBuffer(ByteBuffer buffer) {
        { // signature
            int sLength = buffer.getInt();
            byte[] bSignature = new byte[sLength];
            buffer.get(bSignature);
            this.firma = ByteBuffer.wrap(bSignature);
        }

        { // inputs
            int iLength = buffer.getInt();
            this.inputs = new Transaction[iLength];
            for (int i=0; i<iLength; i++) {
                int tLength = buffer.getInt();
                Transaction t = new Transaction();
                final byte[] bytes = new byte[tLength];
                buffer.get(bytes);
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                t.fromBuffer(bb);
                this.inputs[i] = t;
            }
        }

        { // ouputs
            int oLength = buffer.getInt();
            this.outputs = new Transaction[oLength];
            for (int i=0; i<oLength; i++) {
                int tLength = buffer.getInt();
                Transaction t = new Transaction();
                final byte[] bytes = new byte[tLength];
                buffer.get(bytes);
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                t.fromBuffer(bb);
                this.outputs[i] = t;
            }
        }

        timestamp = buffer.getLong();
        valore = buffer.getInt();

        final int mLength = buffer.getInt();
        final byte[] mBytes = new byte[mLength];
        buffer.get(mBytes, 0, mLength);
        header = new String(mBytes);

        final int fLength = buffer.getInt();
        final byte[] fBytes = new byte[fLength];
        buffer.get(fBytes, 0, fLength);
        mittente = new String(fBytes);

        final int tLength = buffer.getInt();
        final byte[] tBytes = new byte[tLength];
        buffer.get(tBytes, 0, tLength);
        destinatario = new String(tBytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode += mittente.length();
        hashCode += destinatario.length();
        hashCode += timestamp;
        hashCode += header.length();
        hashCode += valore;
        for (byte b :firma.array())
            hashCode += b;
        for (Transaction t : inputs)
            hashCode += t.hashCode();
        for (Transaction t : outputs)
            hashCode += t.hashCode();
        return 31 * hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Transaction))
            return false;
        Transaction c = (Transaction) o;
        { // signature
            if (c.firma.limit() != this.firma.limit())
                return false;
            if (!(Arrays.equals(c.firma.array(), this.firma.array())))
                return false;
        }
        { // inputs
            if (c.inputs.length != this.inputs.length)
                return false;
            for (int i=0; i<c.inputs.length; i++)
                if (!(c.inputs[i].equals(inputs[i])))
                    return false;
        }
        { // ouputs
            if (c.outputs.length != this.outputs.length)
                return false;
            for (int i=0; i<c.outputs.length; i++)
                if (!(c.outputs[i].equals(outputs[i])))
                    return false;
        }
        if (c.timestamp != this.timestamp)
            return false;
        if (c.valore != this.valore)
            return false;
        if (!(c.mittente.equals(this.mittente)))
            return false;
        if (!(c.destinatario.equals(this.destinatario)))
            return false;
        if (!(c.header.equals(this.header)))
            return false;
        else
            return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Firma: ").append(HashUtils.bytesToHex(this.firma.array())).append("\n");
        builder.append("Inputs: ").append(inputs.length).append("\n");
        builder.append("Outputs: ").append(outputs.length).append("\n");
        builder.append("Timestamp: ").append(timestamp).append("\n");
        builder.append("Valore: ").append(valore).append("\n");
        builder.append("Mittente: ").append(mittente).append("\n");
        builder.append("Destinatario: ").append(destinatario).append("\n");
        builder.append("Header: ").append(header).append("\n");
        return builder.toString();
    }
}
