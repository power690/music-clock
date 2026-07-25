package com.xiaowei.music;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalMusicHelper {
    private static final String TAG = "LocalMusicHelper";

    private static LocalMusicHelper instance;

    public static class LocalSong {
        public String title;
        public String artist;
        public String path;
        public String album;
        public long duration;

        public LocalSong(String path) {
            this.path = path;
            this.title = getFileNameNoEx(new File(path).getName());
            this.artist = "";
        }

        private String getFileNameNoEx(String filename) {
            if ((filename != null) && (filename.length() > 0)) {
                int dot = filename.lastIndexOf('.');
                if ((dot > -1) && (dot < (filename.length()))) {
                    return filename.substring(0, dot);
                }
            }
            return filename;
        }
    }

    private Context context;
    private ExoPlayer mediaPlayer;
    private List<LocalSong> currentPlaylist = new ArrayList<>();
    private int currentIndex = -1;
    private OnMusicPlayerListener listener;
    private boolean isPlaying = false;
    private ExecutorService networkExecutor;
    private Handler mainHandler;

    public interface OnMusicPlayerListener {
        void onTrackStart(LocalSong song, Bitmap cover);
        void onPlaybackComplete();
        void onError(String message);
        void onPlayingStateChanged(boolean playing);
    }

    public static synchronized LocalMusicHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LocalMusicHelper(context);
        }
        return instance;
    }

    private LocalMusicHelper(Context context) {
        this.context = context.getApplicationContext();
        this.listener = null;
        this.networkExecutor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initMediaPlayer();
    }

    public void setListener(OnMusicPlayerListener listener) {
        this.listener = listener;
    }

    private void initMediaPlayer() {
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setEnableAudioTrackPlaybackParams(true);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();

        mediaPlayer = new ExoPlayer.Builder(context, renderersFactory)
                .setAudioAttributes(audioAttributes, true )
                .setHandleAudioBecomingNoisy(true)
                .build();

        mediaPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {

                if (playbackState == Player.STATE_READY && !mediaPlayer.getPlayWhenReady()) {
                    mediaPlayer.setPlayWhenReady(true);
                    mediaPlayer.play();
                }
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false;
                    playNext();
                    if (LocalMusicHelper.this.listener != null) {
                        LocalMusicHelper.this.listener.onPlaybackComplete();
                    }
                }
            }

            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying = playing;

                if (LocalMusicHelper.this.listener != null) {
                    if (!playing && mediaPlayer != null && mediaPlayer.getPlayWhenReady()) {

                        return;
                    }
                    LocalMusicHelper.this.listener.onPlayingStateChanged(playing);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "ExoPlayer Error: " + error.getMessage());
                if (LocalMusicHelper.this.listener != null) {
                    LocalMusicHelper.this.listener.onError(context.getString(R.string.error_play_format, error.getMessage() == null ? "" : error.getMessage()));
                }
            }
        });
    }

    public List<LocalSong> scanMusicFiles(String directoryPath) {
        currentPlaylist.clear();
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            return currentPlaylist;
        }

        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return false;
                String name = pathname.getName().toLowerCase();
                return name.endsWith(".mp3") || name.endsWith(".flac") ||
                       name.endsWith(".wav") || name.endsWith(".ogg") ||
                       name.endsWith(".m4a") || name.endsWith(".aac");
            }
        });

        if (files != null) {
            for (File file : files) {
                LocalSong song = new LocalSong(file.getAbsolutePath());
                currentPlaylist.add(song);
            }
            Collections.sort(currentPlaylist, new Comparator<LocalSong>() {
                @Override
                public int compare(LocalSong o1, LocalSong o2) {
                    return o1.title.compareToIgnoreCase(o2.title);
                }
            });
        }
        return currentPlaylist;
    }

    public void playSong(int index) {
        if (index < 0 || index >= currentPlaylist.size()) return;

        try {
            currentIndex = index;
            final LocalSong song = currentPlaylist.get(index);
            MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(new java.io.File(song.path)));
            mediaPlayer.setMediaItem(mediaItem);
            mediaPlayer.prepare();
            mediaPlayer.setPlayWhenReady(true);
            isPlaying = true;

            loadAndNotifySongInfo(song);

            if (listener != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.onPlayingStateChanged(true);
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Play failed", e);
            if (listener != null) listener.onError(context.getString(R.string.error_play_failed_format, e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    public void refreshCurrentSongInfo() {
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            final LocalSong song = currentPlaylist.get(currentIndex);

            loadAndNotifySongInfo(song);
        }
    }

    private void loadAndNotifySongInfo(final LocalSong song) {
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap cover = parseLocalMetadata(song);
                if (cover == null) {
                    cover = findCoverInDirectory(song.path);
                }
                final Bitmap finalCover = cover;

                boolean missingInfo = (finalCover == null) || TextUtils.isEmpty(song.artist);
                if (missingInfo) {
                    notifyUI(song, finalCover);
                    fetchMetadataFromiTunes(song, finalCover);
                } else {
                    notifyUI(song, finalCover);
                }
            }
        });
    }

    private void notifyUI(final LocalSong song, final Bitmap cover) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onTrackStart(song, cover);
                }
            }
        });
    }

    public String getLyricsContent(String audioPath) {

        String embeddedLyrics = getEmbeddedLyricsKt(audioPath);
        if (!TextUtils.isEmpty(embeddedLyrics)) {
            Log.d(TAG, "Found embedded lyrics for: " + audioPath);
            return embeddedLyrics;
        }

        try {
            File audioFile = new File(audioPath);
            String parentDir = audioFile.getParent();
            String fileName = audioFile.getName();
            String nameNoExt = fileName;
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                nameNoExt = fileName.substring(0, dotIndex);
            }

            File lrcFile = new File(parentDir, nameNoExt + ".lrc");
            if (lrcFile.exists() && lrcFile.isFile()) {
                StringBuilder sb = new StringBuilder();
                FileInputStream fis = new FileInputStream(lrcFile);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                fis.close();
                Log.d(TAG, "Found external lrc file");
                return sb.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "read lrc failed: " + e.getMessage());
        }

        return null;
    }

    private String getEmbeddedLyricsKt(String audioPath) {
        if (audioPath == null || audioPath.isEmpty()) return null;
        String lower = audioPath.toLowerCase();
        try {
            if (lower.endsWith(".flac")) {
                return FlacLyricsParser.INSTANCE.readLyrics(audioPath);
            }
            if (lower.endsWith(".mp3") && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    com.mpatric.mp3agic.Mp3File mp3 = new com.mpatric.mp3agic.Mp3File(audioPath);
                    if (mp3.hasId3v2Tag()) {
                        String l = mp3.getId3v2Tag().getLyrics();
                        if (l != null && !l.trim().isEmpty()) return l;
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "mp3agic parse failed: " + t.getMessage());
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "embedded lyrics parse failed: " + t.getMessage());
        }
        return null;
    }

    private static class AudioLyricsParser {
        public static String getEmbeddedLyrics(String path) {
            if (path.toLowerCase().endsWith(".mp3")) {
                return getMp3Lyrics(path);
            } else if (path.toLowerCase().endsWith(".flac")) {
                return getFlacLyrics(path);
            }
            return null;
        }

        private static String getFlacLyrics(String path) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(path, "r");
                byte[] header = new byte[4];
                raf.read(header);
                if (!new String(header).equals("fLaC")) return null;

                boolean lastBlock = false;
                while (!lastBlock) {
                    int blockHeader = raf.readUnsignedByte();
                    lastBlock = (blockHeader & 0x80) != 0;
                    int type = blockHeader & 0x7F;
                    int len1 = raf.readUnsignedByte();
                    int len2 = raf.readUnsignedByte();
                    int len3 = raf.readUnsignedByte();
                    int length = (len1 << 16) | (len2 << 8) | len3;

                    if (type == 4) {

                        int vendorLen = Integer.reverseBytes(raf.readInt());
                        raf.skipBytes(vendorLen);

                        int commentListLen = Integer.reverseBytes(raf.readInt());
                        for (int i = 0; i < commentListLen; i++) {
                            int commentLen = Integer.reverseBytes(raf.readInt());
                            byte[] commentBytes = new byte[commentLen];
                            raf.read(commentBytes);
                            String comment = new String(commentBytes, "UTF-8");
                            if (comment.toUpperCase().startsWith("LYRICS=") ||
                                comment.toUpperCase().startsWith("UNSYNCED LYRICS=")) {
                                int eqIndex = comment.indexOf('=');
                                return comment.substring(eqIndex + 1);
                            }
                        }
                        return null;
                    } else {
                        raf.skipBytes(length);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "FLAC parse error: " + e.getMessage());
            } finally {
                if (raf != null) try { raf.close(); } catch (IOException e) {}
            }
            return null;
        }

        private static String getMp3Lyrics(String path) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(path, "r");
                byte[] header = new byte[3];
                raf.read(header);
                if (!new String(header).equals("ID3")) return null;

                raf.skipBytes(2);
                byte flags = raf.readByte();
                int size = readSynchsafeInt(raf);
                long endOfTag = raf.getFilePointer() + size;

                while (raf.getFilePointer() < endOfTag) {
                    byte[] frameIdBytes = new byte[4];
                    if (raf.read(frameIdBytes) < 4) break;
                    String frameId = new String(frameIdBytes);

                    int frameSize = raf.readInt();
                    raf.skipBytes(2);

                    if (frameId.equals("USLT")) {
                        int encoding = raf.readByte();
                        raf.skipBytes(3);

                        int b;
                        while ((b = raf.readByte()) != 0) {

                        }

                        if (encoding == 1 || encoding == 2) {
                            long pos = raf.getFilePointer();
                            if (raf.readByte() != 0) {
                                raf.seek(pos);
                            }
                        }

                        int readSoFar = 1 + 3 + 1;
                        byte[] buffer = new byte[Math.min(frameSize - readSoFar, 50000)];
                        raf.read(buffer);

                        String charset = "ISO-8859-1";
                        if (encoding == 1) charset = "UTF-16";
                        if (encoding == 2) charset = "UTF-16BE";
                        if (encoding == 3) charset = "UTF-8";

                        return new String(buffer, charset).trim();
                    } else {
                        raf.skipBytes(frameSize);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "MP3 parse error: " + e.getMessage());
            } finally {
                if (raf != null) try { raf.close(); } catch (IOException e) {}
            }
            return null;
        }

        private static int readSynchsafeInt(RandomAccessFile raf) throws IOException {
            int b1 = raf.readByte();
            int b2 = raf.readByte();
            int b3 = raf.readByte();
            int b4 = raf.readByte();
            return (b1 << 21) | (b2 << 14) | (b3 << 7) | b4;
        }
    }

    private Bitmap smartCropBitmap(Bitmap original) {
        if (original == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();
        int[] pixels = new int[width * height];
        original.getPixels(pixels, 0, width, 0, 0, width, height);

        int top = 0;
        int bottom = height;
        int left = 0;
        int right = width;

        final int THRESHOLD = 30;

        for (int y = 0; y < height; y++) {
            boolean rowHasData = false;
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                if (Color.red(pixel) > THRESHOLD ||
                    Color.green(pixel) > THRESHOLD ||
                    Color.blue(pixel) > THRESHOLD) {
                    rowHasData = true;
                    break;
                }
            }
            if (rowHasData) {
                top = y;
                break;
            }
        }

        for (int y = height - 1; y >= 0; y--) {
            boolean rowHasData = false;
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                if (Color.red(pixel) > THRESHOLD ||
                    Color.green(pixel) > THRESHOLD ||
                    Color.blue(pixel) > THRESHOLD) {
                    rowHasData = true;
                    break;
                }
            }
            if (rowHasData) {
                bottom = y + 1;
                break;
            }
        }

        for (int x = 0; x < width; x++) {
            boolean colHasData = false;
            for (int y = top; y < bottom; y++) {
                int pixel = pixels[y * width + x];
                if (Color.red(pixel) > THRESHOLD ||
                    Color.green(pixel) > THRESHOLD ||
                    Color.blue(pixel) > THRESHOLD) {
                    colHasData = true;
                    break;
                }
            }
            if (colHasData) {
                left = x;
                break;
            }
        }

        for (int x = width - 1; x >= 0; x--) {
            boolean colHasData = false;
            for (int y = top; y < bottom; y++) {
                int pixel = pixels[y * width + x];
                if (Color.red(pixel) > THRESHOLD ||
                    Color.green(pixel) > THRESHOLD ||
                    Color.blue(pixel) > THRESHOLD) {
                    colHasData = true;
                    break;
                }
            }
            if (colHasData) {
                right = x + 1;
                break;
            }
        }

        int newWidth = right - left;
        int newHeight = bottom - top;

        if (newWidth <= 0 || newHeight <= 0) return original;

        if (newWidth > width - 5 && newHeight > height - 5) {
            return original;
        }

        try {
            return Bitmap.createBitmap(original, left, top, newWidth, newHeight);
        } catch (Exception e) {
            return original;
        }
    }

    private Bitmap parseLocalMetadata(LocalSong song) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Bitmap coverBitmap = null;
        try {
            retriever.setDataSource(song.path);

            String t = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (!TextUtils.isEmpty(t)) song.title = t;

            String a = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (TextUtils.isEmpty(a)) a = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
            if (TextUtils.isEmpty(a)) a = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR);
            if (!TextUtils.isEmpty(a)) song.artist = a;

            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            song.album = album == null ? "" : album;

            String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (!TextUtils.isEmpty(dur)) song.duration = Long.parseLong(dur);

            byte[] art = retriever.getEmbeddedPicture();
            if (art != null && art.length > 0) {

                Bitmap original = decodeSampledBitmapFromByteArray(art, 1200, 1200);
                coverBitmap = smartCropBitmap(original);
            }
        } catch (Exception e) {
            Log.e(TAG, "ID3 parse error", e);
        } finally {
            try { retriever.release(); } catch (Exception e) {}
        }
        return coverBitmap;
    }

    private Bitmap findCoverInDirectory(String audioPath) {
        try {
            File audioFile = new File(audioPath);
            File parentDir = audioFile.getParentFile();
            if (parentDir == null || !parentDir.isDirectory()) return null;

            String[] coverNames = {"cover.jpg", "cover.png", "folder.jpg", "folder.png",
                                   "album.jpg", "front.jpg"};
            for (String name : coverNames) {
                File coverFile = new File(parentDir, name);
                if (coverFile.exists()) {

                    Bitmap original = decodeSampledBitmapFromFile(coverFile.getAbsolutePath(), 1200, 1200);
                    return smartCropBitmap(original);
                }
            }

            File[] images = parentDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String name = pathname.getName().toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg");
                }
            });

            if (images != null) {
                for (File img : images) {
                    String name = img.getName().toLowerCase();
                    if (name.contains("cover") || name.contains("folder") || name.contains("front")) {

                        Bitmap original = decodeSampledBitmapFromFile(img.getAbsolutePath(), 1200, 1200);
                        return smartCropBitmap(original);
                    }
                }
                if (images.length == 1) {

                    Bitmap original = decodeSampledBitmapFromFile(images[0].getAbsolutePath(), 1200, 1200);
                    return smartCropBitmap(original);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Directory cover search failed", e);
        }
        return null;
    }

    private void fetchMetadataFromiTunes(final LocalSong song, final Bitmap existingCover) {
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (getCurrentSong() != song) return;

                Bitmap apiCover = null;
                String apiArtist = null;
                try {
                    String term = song.title;
                    if (!TextUtils.isEmpty(song.artist) && !"未知艺术家".equals(song.artist)) {
                        term += " " + song.artist;
                    }
                    term = term.replaceAll("\\([^)]*\\)", "");
                    String encodedTerm = URLEncoder.encode(term, "UTF-8");
                    String urlStr = "https://itunes.apple.com/search?term=" + encodedTerm + "&entity=song&limit=1";

                    String jsonResponse = performHttpRequest(urlStr);
                    if (jsonResponse != null) {
                        JSONObject root = new JSONObject(jsonResponse);
                        if (root.optInt("resultCount") > 0) {
                            JSONArray results = root.getJSONArray("results");
                            JSONObject track = results.getJSONObject(0);

                            if (TextUtils.isEmpty(song.artist)) {
                                apiArtist = track.optString("artistName");
                            }

                            if (existingCover == null) {
                                String artworkUrl = track.optString("artworkUrl100");
                                if (!TextUtils.isEmpty(artworkUrl)) {
                                    artworkUrl = artworkUrl.replace("100x100bb", "600x600bb");
                                    Bitmap raw = downloadBitmap(artworkUrl);
                                    apiCover = smartCropBitmap(raw);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "iTunes API error: " + e.getMessage());
                }

                final Bitmap finalCover = (existingCover != null) ? existingCover : apiCover;
                final String finalArtist = apiArtist;

                if (getCurrentSong() == song) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            boolean updated = false;
                            if (!TextUtils.isEmpty(finalArtist)) {
                                song.artist = finalArtist;
                                updated = true;
                            }
                            if (finalCover != null || updated) {
                                if (listener != null) listener.onTrackStart(song, finalCover);
                            }
                        }
                    });
                }
            }
        });
    }

    private String performHttpRequest(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() == 200) {
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private Bitmap downloadBitmap(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                InputStream in = conn.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[4096];
                while ((nRead = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                return decodeSampledBitmapFromByteArray(buffer.toByteArray(), 1200, 1200);
            }
        } catch (Exception e) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private Bitmap decodeSampledBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void playPause() {
        try {
            if (mediaPlayer.getPlayWhenReady()) {
                mediaPlayer.pause();
                isPlaying = false;
            } else {
                if (mediaPlayer.getPlaybackState() == Player.STATE_IDLE) mediaPlayer.prepare();
                mediaPlayer.play();
                isPlaying = true;
            }

            if (listener != null) {
                listener.onPlayingStateChanged(isPlaying);
            }
        } catch (Exception e) {}
    }

    public void playNext() {
        if (currentPlaylist.isEmpty()) return;
        int nextIndex = (currentIndex + 1) % currentPlaylist.size();
        playSong(nextIndex);
    }

    public void playPrev() {
        if (currentPlaylist.isEmpty()) return;
        int prevIndex = (currentIndex - 1 + currentPlaylist.size()) % currentPlaylist.size();
        playSong(prevIndex);
    }

    public void seekTo(long position) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(position);
            } catch (Exception e) {}
        }
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer.getPlayWhenReady()
                    && mediaPlayer.getPlaybackState() == Player.STATE_READY;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPlayWhenReady() {
        try {
            return mediaPlayer.getPlayWhenReady();
        } catch (Exception e) {
            return false;
        }
    }

    public long getCurrentPosition() {
        try {
            return mediaPlayer.getCurrentPosition();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getDuration() {
        try {
            long dur = mediaPlayer.getDuration();
            return dur == C.TIME_UNSET ? 0 : dur;
        } catch (Exception e) {
            return 0;
        }
    }

    public LocalSong getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            return currentPlaylist.get(currentIndex);
        }
        return null;
    }

    public List<LocalSong> getPlaylist() {
        return currentPlaylist;
    }

    public void release() {
        if (networkExecutor != null) networkExecutor.shutdown();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {}
            mediaPlayer = null;
        }
        instance = null;
    }
}
