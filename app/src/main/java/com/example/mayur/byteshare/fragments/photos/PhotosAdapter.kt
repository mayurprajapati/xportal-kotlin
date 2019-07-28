package com.example.mayur.xportal.fragments.photos

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.mayur.xportal.MainActivity
import com.example.mayur.xportal.R
import com.example.mayur.xportal.util.FileUtils
import java.io.File
import java.text.DateFormat
import java.util.*

class PhotosAdapter(
    private val mPhotosFragment: PhotosFragment, //    private Cursor mMediaStoreCursor;
    private val mainActivity: MainActivity
) : RecyclerView.Adapter<PhotosAdapter.PhotosHolder>(), SwipeRefreshLayout.OnRefreshListener {
    var cancelSelectionClickListener: View.OnClickListener
    var selectAllOnClickListener: View.OnClickListener
    private lateinit var imageList: MutableList<File>
    private val selectedImages: MutableList<Int>

    val selectedItemsCount: Int
        get() = selectedImages.size

    //            File f = new File(getUriFromMediaStore(selectedImages.get(i)).toString());
    val selectedFiles: List<File>
        get() {
            val stringBuilder = StringBuilder()
            val temp = ArrayList<File>()
            for (i in selectedImages.indices) {
                val f = imageList[selectedImages[i]]
                if (f.exists())
                    temp.add(f)
                else
                    stringBuilder.append(f.name).append("\n")
            }

            if (stringBuilder.isNotEmpty()) {
                stringBuilder.append("File(s) doesn't exists.")
                MainActivity.handler.post {
                    Toast.makeText(
                        mainActivity,
                        stringBuilder,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            return temp
        }


    init {
        selectedImages = ArrayList()
        cancelSelectionClickListener = View.OnClickListener {
            selectedImages.clear()
            notifyDataSetChanged()
            mPhotosFragment.showCount(0)
            mPhotosFragment.checkBoxSelectAll.isChecked = false
        }

        selectAllOnClickListener = View.OnClickListener {
            if (mPhotosFragment.checkBoxSelectAll.isChecked) {
                selectedImages.clear()
                for (i in imageList.indices)
                    selectedImages.add(i)
                notifyDataSetChanged()
                mPhotosFragment.showCount(selectedImages.size)
            } else {
                selectedImages.clear()
                mPhotosFragment.showCount(selectedImages.size)
                notifyDataSetChanged()
            }
        }
        loadImages()
    }

    private fun getSelectedImages(): List<File> {
        val selectedFiles = ArrayList<File>()
        for (i in selectedImages.indices) {
            //            System.out.println(musicInfoArrayList.get(selectedImages.get(i)).getSongUrl().toString());
            //            selectedFiles.add(new File(String.valueOf(mMediaStoreCursor.get(selectedImages.get(i)).getSongUrl())));
            //            selectedFiles.add(new File(getUriFromMediaStore(selectedImages.get(i)).toString()));
            selectedFiles.add(imageList[selectedImages[i]])
        }
        return selectedFiles
    }

    private fun loadImages() {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)


        val cursor = mainActivity.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection, null,
            MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        )

        Thread(Runnable {
            cursor?.let {
                val total = cursor.count
                imageList = ArrayList(total)
                val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                while (cursor.moveToNext()) {
                    val f = File(Uri.parse(cursor.getString(dataIndex)).toString())
                    if (f.exists()) {
                        imageList.add(f)
                    }
                }

                MainActivity.handler.post { notifyDataSetChanged() }
                cursor.close()
            }

        }).start()
    }

    fun showProperties() {
        val alertDialog = AlertDialog.Builder(mainActivity).create()
        alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_dialogbox)
        val dialogView = LayoutInflater.from(mainActivity).inflate(
            R.layout.files_layout_file_properties,
            mainActivity.findViewById<View>(R.id.filePropertiesRoot) as ViewGroup
        )
        dialogView.findViewById<View>(R.id.textViewPropertiesLastModified).visibility = View.GONE
        dialogView.findViewById<View>(R.id.textViewPropertiesPath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesFilePath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesDateModified).visibility = View.GONE


        Thread(Runnable {
            val fileNames = StringBuilder()
            var len: Long = 0
            for (file in getSelectedImages()) {
                fileNames.append(file.name).append("\n")
                len += file.length()
            }
            val finalLen = len
            MainActivity.handler.post {
                val fileSize = dialogView.findViewById<TextView>(R.id.filePropertiesFileSize)

                fileSize.text = FileUtils.formatFileSize(finalLen)
                alertDialog.setTitle("Properties")
                (dialogView.findViewById<View>(R.id.filePropertiesFileName) as TextView).text =
                        fileNames
                alertDialog.setView(dialogView)

                alertDialog.setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    "OKAY"
                ) { dialog, _ -> dialog.dismiss() }
                alertDialog.show()
            }
        }).start()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): PhotosHolder {
        val view =
            LayoutInflater.from(viewGroup.context).inflate(R.layout.photos_item, viewGroup, false)
        val photosHolder = PhotosHolder(view)

        photosHolder.mImageView.setOnLongClickListener {
            //                File f = new File(String.valueOf(getUriFromMediaStore(photosHolder.getAdapterPosition())));
            val f = imageList[photosHolder.adapterPosition]
            val alertDialog = AlertDialog.Builder(mainActivity).create()
            alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_dialogbox)
            val view1 = LayoutInflater.from(mainActivity).inflate(
                R.layout.files_layout_file_properties,
                mainActivity.findViewById<View>(R.id.filePropertiesRoot) as ViewGroup
            )
            alertDialog.setTitle("Properties")
            val date = Date(f.lastModified())
            (view1.findViewById<View>(R.id.filePropertiesFileName) as TextView).text = f.name
            (view1.findViewById<View>(R.id.filePropertiesFilePath) as TextView).text =
                    f.absolutePath
            var dateFormat = DateFormat.getDateInstance().format(date)
            dateFormat += "\n" + DateFormat.getTimeInstance().format(date)
            (view1.findViewById<View>(R.id.filePropertiesDateModified) as TextView).text =
                    dateFormat
            val fileSize = view1.findViewById<TextView>(R.id.filePropertiesFileSize)
            fileSize.text = FileUtils.formatFileSize(f.length())
            alertDialog.setView(view1)
            alertDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                "OKAY"
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.show()
            true
        }

        photosHolder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!selectedImages.contains(photosHolder.adapterPosition))
                    selectedImages.add(photosHolder.layoutPosition)
                photosHolder.selected.visibility = View.VISIBLE
                photosHolder.cardView.radius = 36f
            } else {
                photosHolder.selected.visibility = View.GONE
                photosHolder.cardView.radius = 0f
                selectedImages.remove(Integer.valueOf(photosHolder.layoutPosition))
            }

            if (selectedItemsCount == itemCount) {
                mPhotosFragment.checkBoxSelectAll.isChecked = true
            }

            if (selectedItemsCount == itemCount - 1)
                mPhotosFragment.checkBoxSelectAll.isChecked = false
            mPhotosFragment.showCount(selectedImages.size)
        }


        return photosHolder
    }

    override fun onBindViewHolder(photosHolder: PhotosHolder, pos: Int) {
        if (selectedImages.contains(pos)) {
            photosHolder.checkBox.isChecked = true
            photosHolder.cardView.radius = 36f
            photosHolder.selected.visibility = View.VISIBLE
        } else {
            photosHolder.selected.visibility = View.GONE
            photosHolder.checkBox.isChecked = false
            photosHolder.cardView.radius = 0f
        }

        val options = DrawableTransitionOptions()
        options.crossFade()

        val requestOptions = RequestOptions().centerCrop().override(150, 150)
        Glide.with(mainActivity)
            //                .load(new File(String.valueOf(getUriFromMediaStore(pos))))
            .load(imageList[pos])
            .transition(options)
            .apply(requestOptions)
            .into(photosHolder.mImageView)
    }

    fun deleteSelectedImages() {
        val alertDialog = AlertDialog.Builder(mainActivity).create()
        alertDialog.setTitle("Are you sure?\nFollowing Photo(s) will be deleted.")
        val stringBuilder = StringBuilder()
        for (file in getSelectedImages()) {
            stringBuilder.append(file.name).append("\n")
        }
        alertDialog.setMessage(stringBuilder)
        alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_dialogbox)

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Delete") { dialog, _ ->
            var deleted = true
            dialog.dismiss()
            val selectedImages = getSelectedImages()
            for (i in selectedImages.indices) {
                val f = selectedImages[i]
                if (!f.delete()) {
                    deleted = false
                }
                if (deleted) {
                    val temp = imageList
                    temp.remove(f)
                    imageList = temp
                    notifyItemRemoved(this@PhotosAdapter.selectedImages[i])
                    Snackbar.make(
                        mainActivity.mViewPager,
                        "Image(s) deleted successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else
                    Snackbar.make(
                        mainActivity.mViewPager,
                        "Couldn't delete song(s).",
                        Toast.LENGTH_SHORT
                    ).show()
            }
            clearCount()
        }

        alertDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "Cancel"
        ) { dialog, _ -> dialog.dismiss() }

        alertDialog.show()
    }

    fun clearCount() {
        selectedImages.clear()
        notifyDataSetChanged()
        mPhotosFragment.showCount(0)
    }

    override fun getItemCount(): Int {
        return imageList.size
        //        return (mMediaStoreCursor == null) ? 0 : mMediaStoreCursor.getCount();
    }

    //    private Cursor swapCursor(Cursor cursor) {
    //        if (mMediaStoreCursor == cursor) {
    //            return null;
    //        }
    //        Cursor oldCursor = mMediaStoreCursor;
    //        this.mMediaStoreCursor = cursor;
    //        if (cursor != null) {
    //            this.notifyDataSetChanged();
    //        }
    //        return oldCursor;
    //    }
    //
    //    public void changeCursor(Cursor cursor) {
    //        Logger.log(cursor == null ? "NULL" : "NOT NULL", this);
    //        Logger.log("changeCursor() called ", this);
    //        Cursor oldCursor = swapCursor(cursor);
    //        if (oldCursor != null) {
    //            oldCursor.close();
    //        }
    //    }

    //    private Uri getUriFromMediaStore(int position) {
    //        int dataIndex = mMediaStoreCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
    //
    //        mMediaStoreCursor.moveToPosition(position);
    //
    //        String dataString = mMediaStoreCursor.getString(dataIndex);
    //        Logger.log("URI :- " + dataString, this);
    //        return Uri.parse(dataString);
    //    }

    //    private void getOnClickUri(int position) {
    //        int mediaTypeIndex = mMediaStoreCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE);
    //        int dataIndex = mMediaStoreCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
    //
    //        mMediaStoreCursor.moveToPosition(position);
    //        String dataString = mMediaStoreCursor.getString(dataIndex);
    //        String authorities = mainActivity.getPackageName() + ".fileprovider";
    //        File f = new File(dataString);
    //        if (f.exists()) {
    ////            Uri mediaUri = FileProvider.getUriForFile(mActivity, authorities, f);
    //            Uri mediaUri = Uri.fromFile(f);
    ////        Uri mediaUri = Uri.parse("file://" + dataString);
    //
    //            switch (mMediaStoreCursor.getInt(mediaTypeIndex)) {
    //                case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
    //                    mOnClickThumbListener.OnClickImage(mediaUri);
    //                    break;
    ////            case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
    ////                mOnClickThumbListener.OnClickVideo(mediaUri);
    ////                break;
    //                default:
    //            }
    //        }
    //    }

    override fun onRefresh() {
        mPhotosFragment.swipeRefreshLayout.isRefreshing = true
        Thread(Runnable {
            MainActivity.handler.postDelayed({
                notifyDataSetChanged()
                mPhotosFragment.refreshCompleted()
            }, 1000)
        }).start()
    }

    inner class PhotosHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        internal var mImageView: ImageView = itemView.findViewById(R.id.imgPhoto)
        internal var cardView: CardView = itemView as CardView
        internal var checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        internal var selected: ImageView = itemView.findViewById(R.id.imageSelectionPlaceholder)

        init {
            mImageView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val intent = Intent(mainActivity, FullScreenImageActivity::class.java)
            intent.putExtra("currentPhoto", imageList[adapterPosition].absolutePath)
            mainActivity.startActivity(intent)
        }
    }
}
