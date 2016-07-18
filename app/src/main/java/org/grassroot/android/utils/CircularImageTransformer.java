package org.grassroot.android.utils;

import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;

/**
 * Created by paballo on 2016/07/12.
 */
public class CircularImageTransformer implements Transformation{

    @Override
    public Bitmap transform(Bitmap source) {
        return ImageUtils.getRoundedShape(source);
    }

    @Override
    public String key() {
        return "circular";
    }
    }

