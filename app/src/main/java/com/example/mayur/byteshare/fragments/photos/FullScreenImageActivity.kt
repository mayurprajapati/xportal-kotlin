package com.example.mayur.xportal.fragments.photos

import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.mayur.xportal.R
import io.multimoon.colorful.CAppCompatActivity
import java.io.File

class FullScreenImageActivity : CAppCompatActivity() {


    internal lateinit var imageView: ImageView
    private lateinit var currentPhoto: String
    //    FullScreenPagerAdapter adapter;
    //    ViewPager viewPager;

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photos_full_screen_item)

        //        Objects.requireNonNull(getSupportActionBar()).hide();

        currentPhoto = intent.getStringExtra("currentPhoto")
        imageView = findViewById(R.id.imgFullScreenPhoto)
        //        Glide.with(this).load(Uri.parse(currentPhoto)).into(imageView);

        Glide.with(this).load(File(currentPhoto)).into(imageView)

        //        viewPager=findViewById(R.id.photoPager);
        //        adapter=new FullScreenPagerAdapter(this,currentPhoto);
        //        viewPager.setAdapter(adapter);

    }

    //implements LoaderManager.LoaderCallbacks<Cursor>
    //    @NonNull
    //    @Override
    //    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
    //        String[] projection = {
    //                MediaStore.Files.FileColumns._ID,
    //                MediaStore.Files.FileColumns.DATE_ADDED,
    //                MediaStore.Files.FileColumns.DATA,
    //                MediaStore.Files.FileColumns.MEDIA_TYPE
    //        };
    //        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
    //                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
    //        return new CursorLoader(
    //                this,
    //                MediaStore.Files.getContentUri("external"),
    //                projection,
    //                selection,
    //                null,
    //                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
    //        );
    //    }
    //
    //    @Override
    //    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
    //        adapter.changeCursor(cursor);
    //    }
    //
    //    @Override
    //    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    //        adapter.changeCursor(null);
    //
    //    }
}
