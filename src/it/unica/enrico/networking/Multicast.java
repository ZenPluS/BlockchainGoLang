package it.unica.enrico.networking;

import it.unica.enrico.interfaces.MessageListener;
import it.unica.enrico.interfaces.Receiver;
import it.unica.enrico.interfaces.Sender;
import it.unica.enrico.models.Data;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Multicast {

    private static final boolean DEBUG = Boolean.getBoolean("debug_all");
    static final int PORT = 5000;
    static final String GROUP = "225.4.5.6";

    public static MulticastSocket createReceiver() throws IOException {
        // Create the socket and bind it to port 'port'.
        final MulticastSocket s = new MulticastSocket(PORT);
        // join the multicast group
        s.joinGroup(InetAddress.getByName(GROUP));
        // Now the socket is set up and we are ready to receive packets
        return s;
    }

    public static void destoryReceiver(MulticastSocket s) throws UnknownHostException, IOException {
        if (s == null)
            return;

        // Leave the multicast group and close the socket
        s.leaveGroup(InetAddress.getByName(GROUP));
        s.close();
    }

    /**
     * Blocking call
     */
    public static boolean recvData(MulticastSocket s, byte[] buffer) throws IOException {
        s.setSoTimeout(100);
        // Create a DatagramPacket and do a receive
        final DatagramPacket pack = new DatagramPacket(buffer, buffer.length);
        try {
            s.receive(pack);
        } catch (SocketTimeoutException e) {
            return false;
        }
        // We have finished receiving data
        return true;
    }

    public static MulticastSocket createSender() throws IOException {
        // Create the socket but we don't bind it as we are only going to send data
        // Note that we don't have to join the multicast group if we are only
        // sending data and not receiving
        return new MulticastSocket();
    }

    public static void destroySender(MulticastSocket s) throws IOException {
        if (s == null)
            return;

        // When we have finished sending data close the socket
        s.close();
    }

    public static void sendData(MulticastSocket s, int ourTTL, byte[] buffer) throws IOException {
        // Create a DatagramPacket
        final DatagramPacket pack = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(GROUP), PORT);
        // Get the current TTL, set our TTL, do a send, reset the TTL
        final int ttl = s.getTimeToLive();
        s.setTimeToLive(ourTTL);
        s.send(pack);
        s.setTimeToLive(ttl);
    }

    public static final class Peer {

        private static final int BUFFER_SIZE = 100*1024;

        public static final class RunnableRecv implements Runnable, Receiver {

            static volatile boolean run = true;
            final ConcurrentLinkedQueue<Data> toRecv = new ConcurrentLinkedQueue<Data>();
            private final MessageListener listener;
            private volatile boolean isReady = false;

            public RunnableRecv(MessageListener listener) {
                run = true;
                this.listener = listener;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Queue<Data> getQueue() {
                return toRecv;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isReady() {
                return isReady;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getHost() {
                return GROUP;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int getPort() {
                return PORT;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void run() {
                MulticastSocket s = null;
                try {
                    if (DEBUG)
                        System.out.println("Creating receiver");
                    s = Multicast.createReceiver();
                    final byte[] array = new byte[BUFFER_SIZE];
                    final ByteBuffer bb = ByteBuffer.wrap(array);
                    isReady = true;
                    while (run) {
                        bb.clear();
                        final boolean p = Multicast.recvData(s, array);
                        if (!p) {
                            Thread.yield();
                            continue;
                        }

                        final Data data = new Data();
                        data.fromBuffer(bb);

                        if (DEBUG)
                            System.out.println("Il server ha ricevuto '"+
                                    new String(data.getMessaggio().array())+
                                    "' da "+data.getIndirizzoSorgente().getHostAddress()+":"+
                                    data.getPortaSorgente());

                        toRecv.add(data);
                        listener.onMessage(this);

                        Thread.yield();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        Multicast.destoryReceiver(s);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        public static final class RunnableSend implements Runnable, Sender {

            private static final int ttl = 1;
            static volatile boolean run = true;
            final ConcurrentLinkedQueue<Data> toSend = new ConcurrentLinkedQueue<Data>();
            private volatile boolean isReady = false;

            public RunnableSend() {
                run = true;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Queue<Data> getQueue() {
                return toSend;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isReady() {
                return isReady;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void run() {
                MulticastSocket s = null;
                try {
                    if (DEBUG)
                        System.out.println("Creating sender");
                    s = Multicast.createSender();
                    final byte[] buffer = new byte[BUFFER_SIZE];
                    final ByteBuffer bb = ByteBuffer.wrap(buffer);
                    isReady = true;
                    while (run) {
                        if (DEBUG && toSend.size()>1)
                            System.out.println("Client toSend size="+toSend.size());
                        final Data d = toSend.poll();
                        if (d != null) {
                            bb.clear();
                            d.toBuffer(bb);
                            bb.flip();

                            if (DEBUG)
                                System.out.println("Il client ("+d.getIndirizzoSorgente().getHostAddress()+":"+
                                        d.getPortaSorgente()+") " + "ha inviato '"+new String(d.getMessaggio()
                                        .array())+"'");

                            Multicast.sendData(s, ttl, buffer);
                        }

                        Thread.yield();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        Multicast.destroySender(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }
}
