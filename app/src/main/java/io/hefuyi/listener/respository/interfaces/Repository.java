package io.hefuyi.listener.respository.interfaces;

import java.util.List;

import io.hefuyi.listener.mvp.model.Album;
import io.hefuyi.listener.mvp.model.Artist;
import io.hefuyi.listener.mvp.model.FolderInfo;
import io.hefuyi.listener.mvp.model.Playlist;
import io.hefuyi.listener.mvp.model.Song;
import rx.Observable;

/**
 * Created by hefuyi on 2016/11/3.
 */

@SuppressWarnings("unused")
public interface Repository {

    //from network


    //form local

    Observable<List<Album>> getAllAlbums();

    Observable<Album> getAlbum(long id);

    Observable<List<Album>> getAlbums(String paramString);

    Observable<List<Song>> getSongsForAlbum(long albumID);

    Observable<List<Album>> getAlbumsForArtist(long artistID);

    Observable<List<Artist>> getAllArtists();

    Observable<Artist> getArtist(long artistID);

    Observable<List<Artist>> getArtists(String paramString);

    Observable<List<Song>> getSongsForArtist(long artistID);

    Observable<List<Song>> getRecentlyAddedSongs();

    Observable<List<Album>> getRecentlyAddedAlbums();

    Observable<List<Artist>> getRecentlyAddedArtists();

    Observable<List<Song>> getRecentlyPlayedSongs();

    Observable<List<Album>> getRecentlyPlayedAlbums();

    Observable<List<Artist>> getRecentlyPlayedArtist();

    Observable<List<Playlist>> getPlaylists(boolean defaultIncluded);

    Observable<List<Song>> getSongsInPlaylist(long playlistID);

    Observable<List<Song>> getQueueSongs();

    Observable<List<Song>> getFavoriteSongs();

    Observable<List<Album>> getFavoriteAlbums();

    Observable<List<Artist>> getFavoriteArtist();

    Observable<List<Song>> getAllSongs();

    Observable<List<Song>> searchSongs(String searchString);

    Observable<List<Song>> getTopPlaySongs();

    Observable<List<FolderInfo>> getFoldersWithSong();

    Observable<List<Song>> getSongsInFolder(String path);

    Observable<List<Object>> getSearchResult(String queryString);
}
