package it.unica.enrico.interfaces;

import it.unica.enrico.models.Data;
import java.util.Queue;

public interface Sender {

    Queue<Data> getQueue();

    boolean isReady();
}