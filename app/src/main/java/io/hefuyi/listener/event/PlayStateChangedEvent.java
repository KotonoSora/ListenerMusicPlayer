package io.hefuyi.listener.event;

/**
 * Created by hefuyi on 2016/11/7.
 */

public class PlayStateChangedEvent {

    private final boolean mPlaying;

    public PlayStateChangedEvent(boolean playing) {
        mPlaying = playing;
    }

    public boolean isPlaying() {
        return mPlaying;
    }
}
