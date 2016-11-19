package ar.com.mtraverso.observable;

import java.awt.*;

/**
 * Created by mtraverso on 11/15/16.
 */
public interface Observable {
    void registerObserver(Observer observer);
    void notifyObservers(Image i);
}
