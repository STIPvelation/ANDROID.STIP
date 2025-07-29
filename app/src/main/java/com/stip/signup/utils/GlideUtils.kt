package com.stip.stip.signup.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class GlideUtils {

    companion object {

        fun loadImage(view: ImageView, drawable: Drawable){
            Glide.with(view.context)
                .load(drawable)
                .into(view)
        }

        fun loadImage(view: ImageView, path: Int){
            Glide.with(view.context)
                .load(path)
                .into(view)
        }

        fun loadImage(view: ImageView, path: Bitmap){
            Glide.with(view.context)
                .load(path)
                .into(view)
        }

        fun loadImage(view: ImageView, path: Int, holderImage: Int){
            Glide.with(view.context)
                .load(path)
                .placeholder(holderImage)
                .into(view)
        }

        fun loadImage(view: ImageView, path: Int, holderImage: Int, errorImage: Int){
            Glide.with(view.context)
                .load(path)
                .placeholder(holderImage)
                .into(view)
        }

        fun loadImage(view: ImageView, url: String, errorImage: Int){
            if (url.isNotBlank()) {
                Glide.with(view.context)
                    .load(url)
                    .placeholder(errorImage)
                    .into(view)
            }
        }

        fun loadImage(view: ImageView, path: String, holderImage: Int, errorImage: Int){
            Glide.with(view.context)
                .load(path)
                .placeholder(holderImage)
                .into(view)
        }

        fun loadImage(view: ImageView, url: String) {
            if (url.isNotBlank()) {
                Glide.with(view.context)
                    .load(url)
                    .into(view)
            }
        }

        fun loadTopRoundedImage(view: ImageView, url: String, round: Int){
            if (url.isNotBlank()) {
                Glide.with(view.context)
                    .load(url)
                    .transform(RoundedCornersTransformation(round, 0 , RoundedCornersTransformation.CornerType.TOP))
                    .into(view)
            }
        }

        fun loadImage(view: ImageView, uri: Uri){
            Glide.with(view.context)
                .load(uri)
                .into(view)
        }

        fun loadImageNoCache(view: ImageView, url: String) {
            Glide.with(view.context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(view)
        }

        fun clearImage(view: ImageView){
            Glide.with(view.context)
                .clear(view)
        }

        fun loadNotAnimationImage(view: ImageView, url: String) {
            if (url.isNotBlank()) {
                Glide.with(view.context)
                    .load(url)
                    .dontAnimate()
                    .into(view)
            }
        }

        fun loadNotAnimationImage(view: ImageView, path: Int) {
            Glide.with(view.context)
                .load(path)
                .dontAnimate()
                .into(view)
        }

        fun loadGifImage(view: ImageView, url: String) {
            if (url.isNotBlank()) {
                Glide.with(view.context)
                    .asGif()
                    .load(url)
                    .into(view)
            }
        }

        fun loadGifImage(view: ImageView, path: Int, animEndCallback: (() -> Unit)?){
            Glide.with(view.context)
                .asGif()
                .load(path)
                .listener(object: RequestListener<GifDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: GifDrawable,
                        model: Any,
                        target: Target<GifDrawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.setLoopCount(1)
                        val callback = object : Animatable2Compat.AnimationCallback() {
                            override fun onAnimationEnd(drawable: Drawable?) {
                                animEndCallback?.invoke()
                                super.onAnimationEnd(drawable)
                            }
                        }
                        resource?.registerAnimationCallback(callback)

                        return false
                    }

                })
                .into(view)
        }

        fun loadImageCircleCrop(view: ImageView, url: String?, errorImage: Int) {
            if (url.isNullOrBlank()) {
                Glide.with(view.context)
                    .load(errorImage)
                    .circleCrop()
                    .into(view)
            } else {
                Glide.with(view.context)
                    .load(url)
                    .circleCrop()
                    .into(view)
            }
        }

        fun loadImageCircleCrop(view: ImageView, uri: Uri) {
            Glide.with(view.context)
                .load(uri)
                .circleCrop()
                .into(view)
        }

        fun loadImageCircleCrop(view: ImageView, path: Int) {
            Glide.with(view.context)
                .load(path)
                .circleCrop()
                .into(view)
        }

        fun loadImageCircleCrop(view: ImageView, url: String) {
            Glide.with(view.context)
                .load(url)
                .circleCrop()
                .into(view)
        }

        fun loadImageCircleCrop(view: ImageView, path: Bitmap){
            Glide.with(view.context)
                .load(path)
                .circleCrop()
                .into(view)
        }

        fun loadImageWithCenterCrop(view: ImageView, path: Int, round: Int = 1) {
            Glide.with(view.context)
                .load(path)
                .transform(CenterCrop(), RoundedCorners(round))
                .into(view)
        }

        fun loadImageWithCenterCrop(view: ImageView, url: String, round: Int = 1) {
            if (url.isNotBlank()) {
                Glide.with(view.context)
                    .load(url)
                    .transform(CenterCrop(), RoundedCorners(round))
                    .into(view)
            }
        }

        fun loadImageWithCenterCrop(view: ImageView, uri: Uri, round: Int = 1) {
            Glide.with(view.context)
                .load(uri)
                .transform(CenterCrop(), RoundedCorners(round))
                .into(view)
        }

        fun loadImageWithCenterCropNoCache(view: ImageView, url: String, round: Int = 1) {
            if (url.isNotBlank()) {
                Glide.with(view.context)
                    .load(url)
                    .transform(CenterCrop(), RoundedCorners(round))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(view)
            }
        }

        fun displayImage(
            context: Context?,
            url: String?,
            imageView: ImageView?,
            width: Int,
            height: Int
        ) {
            if (imageView == null || context == null) {
                return
            }
            val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(width / 2, height / 2).centerCrop()
            Glide.with(context)
                .load(Uri.parse(url))
                .apply(requestOptions)
                .into(imageView)
        }

        fun circleCenterCrop(view: ImageView, resource: Int) {
            Glide.with(view.context)
                .load(resource)
                .apply(RequestOptions()
                    .centerCrop()
                    .circleCrop())
                    // .transforms was deprecated
                .into(view)
        }
    }

fun RequestManager.loadImage(view: ImageView, drawable: Drawable){
    this.load(drawable)
        .into(view)
}

fun RequestManager.loadImage(view: ImageView, path: Int){
    this.load(path)
        .into(view)
}

fun RequestManager.loadImage(view: ImageView, path: Bitmap){
    this.load(path)
        .into(view)
}

fun RequestManager.loadImage(view: ImageView, path: Int, holderImage: Int){
    this.load(path)
        .placeholder(holderImage)
        .into(view)
}

fun RequestManager.loadImage(view: ImageView, path: Int, holderImage: Int, errorImage: Int){
    this.load(path)
        .placeholder(holderImage)
        .into(view)
}

fun RequestManager.loadImage(view: ImageView, url: String, errorImage: Int){
    if (url.isNotBlank()) {
        this.load(url)
            .placeholder(errorImage)
            .into(view)
    }
}

fun RequestManager.loadImage(view: ImageView, path: String, holderImage: Int, errorImage: Int){
    this.load(path)
        .placeholder(holderImage)
        .into(view)
}

fun RequestManager.loadImage(view: ImageView, url: String) {
    if (url.isNotBlank()) {
        this.load(url)
            .into(view)
    }
}

fun RequestManager.loadImage(view: ImageView, uri: Uri){
    this.load(uri)
        .into(view)
}

fun RequestManager.roundedLoadImageWithCenterCrop(view: ImageView, path: Int, round: Int = 1){
    this.load(path)
        .transform(CenterCrop(), RoundedCorners(round))
        .into(view)
}

fun RequestManager.roundedLoadImageWithCenterCrop(view: ImageView, url: String?, round: Int = 1, errorImage: Int){
    if (url.isNullOrBlank()) {
        this.load(errorImage)
            .transform(CenterCrop(), RoundedCorners(round))
            .into(view)
    } else {
        this.load(url)
            .transform(CenterCrop(), RoundedCorners(round))
            .error(errorImage)
            .into(view)
    }
}

fun RequestManager.roundedLoadImageWithCenterCrop(view: ImageView, bitmap: Bitmap, round: Int = 1){
    this.load(bitmap)
        .transform(CenterCrop(), RoundedCorners(round))
        .into(view)
}

fun RequestManager.roundedLoadImageWithCenterCrop(view: ImageView, uri:Uri, round: Int = 1){
    this.load(uri)
        .transform(CenterCrop(), RoundedCorners(round))
        .into(view)
}

fun RequestManager.topRoundedLoadImageWithCenterCrop(view: ImageView, url: String, round: Int = 1){
    this.load(url)
        .transform(CenterCrop(), RoundedCornersTransformation(round, 0 , RoundedCornersTransformation.CornerType.TOP))
        .into(view)
}
}