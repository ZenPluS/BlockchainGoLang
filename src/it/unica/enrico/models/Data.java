package it.unica.enrico.models;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Data {

    private static final int LENGTH_LENGTH = 4;
    private static final int FROM_LENGTH = 4;
    private static final int TO_LENGTH = 4;

    private String mittente;
    private String destinatario;
    private InetAddress indirizzoSorgente;
    private int portaSorgente;
    private InetAddress indirizzoDestinzazione;
    private int portaDestinzazione;

    public String getMittente () {
        return mittente;
    }

    public void setMittente (String mittente) {
        this.mittente = mittente;
    }

    public String getDestinatario () {
        return destinatario;
    }

    public void setDestinatario (String destinatario) {
        this.destinatario = destinatario;
    }

    public InetAddress getIndirizzoSorgente () {
        return indirizzoSorgente;
    }

    public void setIndirizzoSorgente (InetAddress indirizzoSorgente) {
        this.indirizzoSorgente = indirizzoSorgente;
    }

    public int getPortaSorgente () {
        return portaSorgente;
    }

    public void setPortaSorgente (int portaSorgente) {
        this.portaSorgente = portaSorgente;
    }

    public InetAddress getIndirizzoDestinzazione () {
        return indirizzoDestinzazione;
    }

    public void setIndirizzoDestinzazione (InetAddress indirizzoDestinzazione) {
        this.indirizzoDestinzazione = indirizzoDestinzazione;
    }

    public int getPortaDestinzazione () {
        return portaDestinzazione;
    }

    public void setPortaDestinzazione (int portaDestinzazione) {
        this.portaDestinzazione = portaDestinzazione;
    }

    public ByteBuffer getFirma () {
        return firma;
    }

    public void setFirma (ByteBuffer firma) {
        this.firma = firma;
    }

    public ByteBuffer getMessaggio () {
        return messaggio;
    }

    public void setMessaggio (ByteBuffer messaggio) {
        this.messaggio = messaggio;
    }

    private ByteBuffer firma;
    private ByteBuffer messaggio;

    public Data() { }

    public Data (String mittente, String indirizzoSorgente, int portaSorgente,
                 String destinatario, String indirizzoDestinzazione,
                 int portaDestinzazione, byte[] firma, byte[] messaggio) {
        this.mittente = mittente;
        this.destinatario = destinatario;
        try {
            this.indirizzoSorgente = InetAddress.getByName(indirizzoSorgente);
            this.indirizzoDestinzazione = InetAddress.getByName(indirizzoDestinzazione);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.portaSorgente = portaSorgente;
        this.portaDestinzazione = portaDestinzazione;

        this.firma = ByteBuffer.allocate(firma.length);
        this.firma.put(firma);
        this.firma.flip();

        this.messaggio = ByteBuffer.allocate(messaggio.length);
        this.messaggio.put(messaggio);
        this.messaggio.flip();
    }

    public int getBufferLength() {
        return  FROM_LENGTH + mittente.getBytes().length +
                TO_LENGTH + destinatario.getBytes().length +
                LENGTH_LENGTH + indirizzoSorgente.getHostAddress().getBytes().length +
                LENGTH_LENGTH + String.valueOf(portaSorgente).getBytes().length +
                LENGTH_LENGTH + indirizzoDestinzazione.getHostAddress().getBytes().length +
                LENGTH_LENGTH + String.valueOf(portaDestinzazione).getBytes().length +
                LENGTH_LENGTH + firma.limit() +
                LENGTH_LENGTH + messaggio.limit();
    }

    public void toBuffer(ByteBuffer buffer) {
        final byte[] fBytes = mittente.getBytes();
        buffer.putInt(fBytes.length);
        buffer.put(fBytes);

        final byte[] oBytes = destinatario.getBytes();
        buffer.putInt(oBytes.length);
        buffer.put(oBytes);

        { // Source
            final byte[] hBytes = indirizzoSorgente.getHostAddress().getBytes();
            final int hLength = hBytes.length;
            buffer.putInt(hLength);
            buffer.put(hBytes);

            final byte[] pBytes = String.valueOf(portaSorgente).getBytes();
            final int pLength = pBytes.length;
            buffer.putInt(pLength);
            buffer.put(pBytes);
        }

        { // Destination
            final byte[] hBytes = indirizzoDestinzazione.getHostAddress().getBytes();
            final int hLength = hBytes.length;
            buffer.putInt(hLength);
            buffer.put(hBytes);

            final byte[] pBytes = String.valueOf(portaDestinzazione).getBytes();
            final int pLength = pBytes.length;
            buffer.putInt(pLength);
            buffer.put(pBytes);
        }

        // Sig
        buffer.putInt(firma.limit());
        buffer.put(firma);

        // Data
        buffer.putInt(messaggio.limit());
        buffer.put(messaggio);
    }

    public void fromBuffer(ByteBuffer buffer) {
        final int fLength = buffer.getInt();
        final byte[] fBytes = new byte[fLength];
        buffer.get(fBytes, 0, fLength);
        mittente = new String(fBytes);

        final int oLength = buffer.getInt();
        final byte[] oBytes = new byte[oLength];
        buffer.get(oBytes, 0, oLength);
        destinatario = new String(oBytes);

        { // Source
            final int hLength = buffer.getInt();
            final byte[] hostBytes = new byte[hLength];
            buffer.get(hostBytes);
            final String sHost = new String(hostBytes);
            try {
                this.indirizzoSorgente = InetAddress.getByName(sHost);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            final int pLength = buffer.getInt();
            final byte[] portBytes = new byte[pLength];
            buffer.get(portBytes);
            final String sPort = new String(portBytes);
            this.portaSorgente = Integer.parseInt(sPort);
        }

        { // Destination
            final int hLength = buffer.getInt();
            final byte[] hostBytes = new byte[hLength];
            buffer.get(hostBytes);
            final String sHost = new String(hostBytes);
            try {
                this.indirizzoDestinzazione = InetAddress.getByName(sHost);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            final int pLength = buffer.getInt();
            final byte[] portBytes = new byte[pLength];
            buffer.get(portBytes);
            final String sPort = new String(portBytes);
            this.portaDestinzazione = Integer.parseInt(sPort);
        }

        // Sig
        final int sLength = buffer.getInt();
        final byte[] sBytes = new byte[sLength];
        buffer.get(sBytes, 0, sLength);
        this.firma = ByteBuffer.allocate(sBytes.length);
        this.firma.put(sBytes);

        // Data
        final int dLength = buffer.getInt();
        final byte[] dBytes = new byte[dLength];
        buffer.get(dBytes, 0, dLength);
        this.messaggio = ByteBuffer.allocate(dBytes.length);
        this.messaggio.put(dBytes);

        this.messaggio.flip();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Data))
            return false;
        Data d = (Data) o;
        if (!(d.mittente.equals(this.mittente)))
            return false;
        if (!(d.destinatario.equals(this.destinatario)))
            return false;
        if (!(indirizzoSorgente.equals(d.indirizzoSorgente)))
            return false;
        if (portaSorgente != d.portaSorgente)
            return false;
        if (!(indirizzoDestinzazione.equals(d.indirizzoDestinzazione)))
            return false;
        if (portaDestinzazione != d.portaDestinzazione)
            return false;
        if (!(Arrays.equals(this.firma.array(), d.firma.array())))
            return false;
        if (!(Arrays.equals(this.messaggio.array(), d.messaggio.array())))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Mittente: ").append(mittente).append("\n");
        builder.append("Sorgente: ").append(indirizzoSorgente.getHostAddress()).append(":").append(portaSorgente).append("\n\n");
        builder.append("Destinatario: ").append(destinatario).append("\n");
        builder.append("Destinazione: ").append(indirizzoDestinzazione.getHostAddress()).append(":").append(portaDestinzazione).append("\n\n");
        builder.append("Messaggio: ").append(new String(messaggio.array()));
        return builder.toString();
    }
}
