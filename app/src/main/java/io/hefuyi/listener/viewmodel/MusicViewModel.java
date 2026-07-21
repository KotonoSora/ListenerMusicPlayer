package io.hefuyi.listener.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.event.PlayStateChangedEvent;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MusicViewModel extends ViewModel {

    private final MutableLiveData<String> songTitle = new MutableLiveData<>();
    private final MutableLiveData<String> songArtist = new MutableLiveData<>();
    private final MutableLiveData<Long> songId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();

    private Subscription metaSubscription;
    private Subscription playStateSubscription;

    public MusicViewModel() {
        // Initialize with current state
        updateFromPlayer();
        subscribeToEvents();
    }

    public LiveData<String> getSongTitle() {
        return songTitle;
    }

    public void setSongTitle(String title) {
        if (title != null && !title.equals(songTitle.getValue())) {
            songTitle.setValue(title);
        }
    }

    public LiveData<String> getSongArtist() {
        return songArtist;
    }

    public void setSongArtist(String artist) {
        if (artist != null && !artist.equals(songArtist.getValue())) {
            songArtist.setValue(artist);
        }
    }

    public LiveData<Long> getSongId() {
        return songId;
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean playing) {
        if (isPlaying.getValue() == null || playing != isPlaying.getValue()) {
            isPlaying.setValue(playing);
        }
    }

    private void updateFromPlayer() {
        songTitle.setValue(MusicPlayer.getTrackName());
        songArtist.setValue(MusicPlayer.getArtistName());
        songId.setValue(MusicPlayer.getCurrentAudioId());
        isPlaying.setValue(MusicPlayer.isPlaying());
    }

    private void subscribeToEvents() {
        metaSubscription = RxBus.getInstance()
                .toObservable(MetaChangedEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    songTitle.setValue(event.getSongName());
                    songArtist.setValue(event.getArtistName());
                    songId.setValue(event.getSongId());
                }, throwable -> {
                });

        playStateSubscription = RxBus.getInstance()
                .toObservable(PlayStateChangedEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    isPlaying.setValue(event.isPlaying());
                }, throwable -> {
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (metaSubscription != null && !metaSubscription.isUnsubscribed()) {
            metaSubscription.unsubscribe();
        }
        if (playStateSubscription != null && !playStateSubscription.isUnsubscribed()) {
            playStateSubscription.unsubscribe();
        }
    }
}
