package it.unica.enrico.models;

import it.unica.enrico.utils.HashUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

/* questa classe rappresenta il blocco generico della blockchain */
public class Block {

    //alcune costanti per il funzionamento
    private static final int FROM_LENGTH = 4;
    private static final int BOOLEAN_LENGTH = 2;
    private static final int NUM_OF_ZEROS_LENGTH = 4;
    private static final int NONCE_LENGTH = 4;
    private static final int BLOCK_LENGTH = 4;
    private static final int LENGTH_LENGTH = 4;

    private String provenienza;         //chi ha caricato il nodo sulla blockchain
    private Boolean bloccoConf=false;   //valore booleano che rapprensenta se il blocco è confermato o no, quindi valido o no
    private int numeroDiZeri;           //numero di zeri contenuti nell'hash del blocco quindi indica la difficoltà mining
    private int nonce;                  //numero random/pseudo random utilizzato per la generazione dell'hash del blocco
    private int dimensione;             //indica la dimensione del blocco
    int indice;                         //posizione del blocco nella blockchain
    private Transaction[] transazioni;  //Array delle transazioni contenute nel blocco
    private byte[] hashBloccoPrecedente;//hash del blocco precedente
    private byte[] hash;                //hash del blocco attuale

    public int getBufferLength() {
        int transactionsLength = 0;
        for (Transaction t : transazioni)
            transactionsLength += LENGTH_LENGTH + t.getBufferLength();
        return  FROM_LENGTH + provenienza.length() +
                BOOLEAN_LENGTH +
                NUM_OF_ZEROS_LENGTH +
                NONCE_LENGTH +
                BLOCK_LENGTH +
                LENGTH_LENGTH + hashBloccoPrecedente.length +
                LENGTH_LENGTH + hash.length +
                LENGTH_LENGTH + transactionsLength;
    }

    //crezione del buffer
    public void toBuffer(ByteBuffer buffer) {
        final byte[] fBytes = provenienza.getBytes();
        buffer.putInt(fBytes.length);
        buffer.put(fBytes);

        buffer.putChar(getBoolean(bloccoConf));
        buffer.putInt(numeroDiZeri);
        buffer.putInt(nonce);
        buffer.putInt(dimensione);

        buffer.putInt(hashBloccoPrecedente.length);
        buffer.put(hashBloccoPrecedente);

        buffer.putInt(hash.length);
        buffer.put(hash);

        buffer.putInt(transazioni.length);
        for (Transaction t : transazioni) {
            buffer.putInt(t.getBufferLength());
            t.toBuffer(buffer);
        }
    }

    public void fromBuffer(ByteBuffer buffer) {
        final int fLength = buffer.getInt();
        final byte[] fBytes = new byte[fLength];
        buffer.get(fBytes, 0, fLength);
        provenienza = new String(fBytes);

        bloccoConf = parseBoolean(buffer.getChar());
        numeroDiZeri = buffer.getInt();
        nonce = buffer.getInt();
        dimensione = buffer.getInt();

        { // previous hash
            final int length = buffer.getInt();
            hashBloccoPrecedente = new byte[length];
            buffer.get(hashBloccoPrecedente);
        }

        { // next hash
            final int length = buffer.getInt();
            hash = new byte[length];
            buffer.get(hash);
        }

        int tLength = buffer.getInt();
        transazioni =  new Transaction[tLength];
        for (int i=0; i < tLength; i++) {
            int length = buffer.getInt();
            final byte[] bytes = new byte[length];
            buffer.get(bytes);
            final ByteBuffer bb = ByteBuffer.wrap(bytes);
            final Transaction t = new Transaction();
            t.fromBuffer(bb);
            transazioni[i] = t;
        }
    }

    private static final char getBoolean(boolean bool) {
        return (bool?'T':'F');
    }

    private static final boolean parseBoolean(char bool) {
        return (bool=='T'?true:false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode += provenienza.length();
        if (bloccoConf)
            hashCode += 1;
        hashCode += nonce;
        hashCode += dimensione;
        hashCode += numeroDiZeri;
        hashCode += transazioni.length;
        for (Transaction t : transazioni)
            hashCode += t.hashCode();
        for (byte b : hashBloccoPrecedente)
            hashCode += b;
        for (byte b : hash)
            hashCode += b;
        return 31 * hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Block))
            return false;
        Block c = (Block) o;
        if (!(c.provenienza.equals(provenienza)))
            return false;
        if (bloccoConf != c.bloccoConf)
            return false;
        if (nonce != c.nonce)
            return false;
        if (dimensione != c.dimensione)
            return false;
        if (numeroDiZeri != c.numeroDiZeri)
            return false;
        if (c.transazioni.length != this.transazioni.length)
            return false;
        { // compare transactions
            for (int i=0; i<c.transazioni.length; i++) {
                if (!(c.transazioni[i].equals(this.transazioni[i])))
                    return false;
            }
        }
        if (!(Arrays.equals(c.hashBloccoPrecedente, hashBloccoPrecedente)))
            return false;
        if (!(Arrays.equals(c.hash, hash)))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Blocco valido: ").append(bloccoConf).append("\n");
        builder.append("Numero di Zeri: ").append(numeroDiZeri).append("\n");
        builder.append("Nonce utilizzato: ").append(nonce).append("\n");
        builder.append("Dimensione blocco: ").append(dimensione).append("\n");
        builder.append("Hash precedente: ").append(HashUtils.bytesToHex(hashBloccoPrecedente));
        builder.append("Hash: ").append(HashUtils.bytesToHex(hash));
        builder.append("Block-> [ ").append("\n");
        for (Transaction t : transazioni) {
            builder.append("Transazione: [").append("\n");
            builder.append(t.toString()).append("\n");
            builder.append("]").append("\n");
        }
        builder.append("]\n");
        return builder.toString();
    }
}
