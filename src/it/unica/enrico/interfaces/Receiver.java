package it.unica.enrico.interfaces;

import it.unica.enrico.models.Data;
import java.util.Queue;

public interface Receiver {

    Queue<Data> getQueue();

    boolean isReady();

    String getHost();

    int getPort();
}