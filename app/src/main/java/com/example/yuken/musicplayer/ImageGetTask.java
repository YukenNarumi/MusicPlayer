package com.example.yuken.musicplayer;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

public class ImageGetTask extends AsyncTask<ImageGetTask.Params, Void, Bitmap> {
    public static class Params {
        public enum Type {
            DEFAULT,
            SLIM
        }

        private String path;
        private Type   type;

        /**
         * デフォルトコンストラクタ
         *
         * @param _path 画像パス
         * @param _type 画像タイプ
         */
        Params(String _path, Type _type) {
            path = _path;
            type = _type;
        }

        /**
         * @return キャッシュキーに追加する文字
         */
        private String GetPathAddWord() {
            String _key = "";
            if (type == Type.SLIM) {
                return "_slim";
            }
            return _key;
        }

        /**
         * @return 画像パス
         */
        private String GetPath() {
            return path;
        }

        /**
         * @return キャッシュ用画像パス
         */
        public String GetCachePath() {
            return path + GetPathAddWord();
        }

        /**
         * @return 表示画像が縮小のものかどうか
         */
        private boolean IsSlim() {
            return (type == Type.SLIM);
        }
    }

    // アルバムアート用画像サイズ
    private final static int ALBUM_ART_IMAGE_WIDTH       = 200;
    private final static int ALBUM_ART_IMAGE_HEIGHT      = 200;
    private final static int ALBUM_ART_SLIM_IMAGE_WIDTH  = 72;
    private final static int ALBUM_ART_SLIM_IMAGE_HEIGHT = 72;

    @SuppressLint("StaticFieldLeak")
    private ImageView image;
    private String    tag;

    /**
     * デフォルトコンストラクタ
     *
     * @param _image 表示する画像
     */
    ImageGetTask(ImageView _image) {
        super();
        image = _image;
        tag = image.getTag().toString();
    }

    /**
     * 非同期処理
     * 画像の取得 / キャッシュ済みの場合はキャッシュから、そうでない場合は通常読み込み
     *
     * @param params 画像パス
     * @return 非同期処理で取得した画像
     */
    @Override
    protected Bitmap doInBackground(Params... params) {
        Params _param = params[0];
        Bitmap bitmap = ImageCache.getImage(_param.GetCachePath());
        if (bitmap != null) {
            return bitmap;
        }

        int _width  = ALBUM_ART_IMAGE_WIDTH;
        int _height = ALBUM_ART_IMAGE_HEIGHT;
        if (_param.IsSlim()) {
            _width = ALBUM_ART_SLIM_IMAGE_WIDTH;
            _height = ALBUM_ART_SLIM_IMAGE_HEIGHT;
        }
        bitmap = decodeBitmap(_param.GetPath(), _width, _height);
        ImageCache.setImage(_param.GetCachePath(), bitmap);

        return bitmap;
    }

    /**
     * 非同期処理終了後に実行する処理
     * 非同期処理で取得した画像を設定する
     *
     * @param result 非同期処理で取得した画像
     */
    @Override
    protected void onPostExecute(Bitmap result) {
        if (tag.equals(image.getTag())) {
            image.setImageBitmap(result);
        }
    }

    /**
     * Bitmap形式への画像デコード
     *
     * @param path   画像パス
     * @param width  画像の幅
     * @param height 画像の高さ
     * @return 画像パスから読み込んだ画像 / 画像が存在しない場合 null
     */
    private static Bitmap decodeBitmap(String path, int width, int height) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * 画像をサブサンプル(縮小して読み込む、低解像度で読み込む)するためのサイズ計算
     *
     * @param options   デコーダ用オプション
     * @param reqWidth  画像の幅
     * @param reqHeight 画像の高さ
     * @return 2のべき乗となるよう計算したサイズ
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height       = options.outHeight;
        final int width        = options.outWidth;
        int       inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            }
            else {
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }
        }
        return inSampleSize;
    }
}
