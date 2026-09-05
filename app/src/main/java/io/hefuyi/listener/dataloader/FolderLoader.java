package io.hefuyi.listener.dataloader;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.hefuyi.listener.mvp.model.FolderInfo;
import rx.Observable;

/**
 * Created by hefuyi on 2016/12/11.
 */

public class FolderLoader {

    /**
     * 检索包含音频文件的文件夹, 并统计该文件夹下的歌曲数目
     *
     * @return Observable containing list of folder information
     */
    @SuppressWarnings("deprecation")
    public static Observable<List<FolderInfo>> getFoldersWithSong(final Context context) {
        return Observable.create(subscriber -> {
            final List<FolderInfo> folderInfos = new ArrayList<>();
            final String[] projection = new String[]{MediaStore.Audio.Media.DATA};
            final String selection = "is_music=1 AND title != ''";

            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);

            if (cursor != null) {
                Map<String, Integer> folderCountMap = new HashMap<>();
                int index_data = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                while (cursor.moveToNext()) {
                    String filePath = cursor.getString(index_data);
                    if (filePath != null) {
                        int lastSeparatorIndex = filePath.lastIndexOf(File.separator);
                        if (lastSeparatorIndex > 0) {
                            String folderPath = filePath.substring(0, lastSeparatorIndex);
                            folderCountMap.merge(folderPath, 1, Integer::sum);
                        }
                    }
                }
                cursor.close();

                for (Map.Entry<String, Integer> entry : folderCountMap.entrySet()) {
                    String folderPath = entry.getKey();
                    int songCount = entry.getValue();
                    String folderName = folderPath.substring(folderPath.lastIndexOf(File.separator) + 1);
                    folderInfos.add(new FolderInfo(folderName, folderPath, songCount));
                }
            }
            subscriber.onNext(folderInfos);
            subscriber.onCompleted();
        });
    }
}
