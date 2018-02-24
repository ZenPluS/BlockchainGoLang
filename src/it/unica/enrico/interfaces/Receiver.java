package it.unica.enrico.interfaces;

import it.unica.enrico.models.Data;
import java.util.Queue;

public interface Receiver {

    public Queue<Data> getQueue();

    public boolean isReady();

    public String getHost();

    public int getPort();
}