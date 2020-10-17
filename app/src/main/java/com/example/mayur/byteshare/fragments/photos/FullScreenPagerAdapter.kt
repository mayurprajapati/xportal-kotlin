package com.example.mayur.byteshare.fragments.photos

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.viewpager.widget.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.mayur.byteshare.R

class FullScreenPagerAdapter(
    internal var context: Context,
    private var currentPhoto: String
)
    : PagerAdapter() {
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var mMediaStoreCursor: Cursor

    override fun getCount(): Int {
        return mMediaStoreCursor.count
    }

    override fun isViewFromObject(view: View, o: Any): Boolean {
        return false
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view_item = layoutInflater.inflate(R.layout.photos_full_screen_item, container, false)
        val imageView = view_item.findViewById<ImageView>(R.id.imgFullScreenPhoto)
        val dataString = currentPhoto
        Uri.parse("file://$dataString")
        Glide.with(context).load(getUriFromMediaStore(position)).into(imageView)
        return view_item
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
    }

    private fun getUriFromMediaStore(position: Int): Uri {
        val dataIndex = mMediaStoreCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

        mMediaStoreCursor.moveToPosition(position)

        val dataString = mMediaStoreCursor.getString(dataIndex)
        return Uri.parse("file://$dataString")
    }
}