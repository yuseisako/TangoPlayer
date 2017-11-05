package me.yusei.tangoplayer;

import android.support.annotation.NonNull;
import android.util.Log;

import com.nononsenseapps.filepicker.FilePickerFragment;

import java.io.File;

public class FilteredFilePickerFragment extends FilePickerFragment {
    /**
     * @param file
     * @return The file extension. If file has no extension, it returns null.
     */
    private String getExtension(@NonNull File file) {
        String path = file.getPath();
        int i = path.lastIndexOf(".");
        if (i < 0) {
            return null;
        } else {
            return path.substring(i);
        }
    }

    @Override
    protected boolean isItemVisible(final File file) {
        Log.d("debug", "isitemvisible");
        boolean ret = super.isItemVisible(file);
        if (ret && !isDir(file) && (mode == MODE_FILE || mode == MODE_FILE_AND_DIR)) {
            String ext = getExtension(file);
            return ext != null && ((".mp4").equalsIgnoreCase(ext) || (".3gp").equalsIgnoreCase(ext) || (".webm").equalsIgnoreCase(ext) || (".mkv").equalsIgnoreCase(ext) || (".avi").equalsIgnoreCase(ext));
        }
        return ret;
    }
}