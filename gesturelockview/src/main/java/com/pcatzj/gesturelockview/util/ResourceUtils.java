package com.pcatzj.gesturelockview.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;

/**
 * 类  名： ResourceUtils<br/>
 * 包  名： com.htsc.android.mcrm.view.util<br/>
 * 描  述： <br/>
 * 日  期： 2017-05-04 17:06<br/>
 *
 * @author pcatzj <br/>
 */
public class ResourceUtils {

    public static Bitmap getBitmap(Context context, int drawableRes) {
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = context.getResources().getDrawable(drawableRes, context.getTheme());
        } else {
            drawable = context.getResources().getDrawable(drawableRes);
        }
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
