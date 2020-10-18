package com.example.mayur.byteshare

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.annotation.RequiresPermission
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.core.view.GravityCompat
import androidx.viewpager.widget.ViewPager
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.appcompat.widget.Toolbar
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mayur.byteshare.connection.wifi.MyWifiManager
import com.example.mayur.byteshare.connection.wifimanager.TransferWifi
import com.example.mayur.byteshare.R
import com.example.mayur.byteshare.bloc.apps.AppsBloc
import com.example.mayur.byteshare.connection.connection.RootFragment
import com.example.mayur.byteshare.connection.hotspot.HotspotManager
import com.example.mayur.byteshare.connection.hotspot.TransferHotspot
import com.example.mayur.byteshare.connection.logger.Logger
import com.example.mayur.byteshare.fragments.apps.AppsFragment
import com.example.mayur.byteshare.fragments.files.FilesFragment
import com.example.mayur.byteshare.fragments.history.HistoryFragment
import com.example.mayur.byteshare.fragments.music.MusicFragment
import com.example.mayur.byteshare.fragments.photos.PhotosFragment
import com.example.mayur.byteshare.fragments.video.VideosFragment
import com.example.mayur.byteshare.hider.Hider
import com.example.mayur.byteshare.hider.HiderActivity
import com.example.mayur.byteshare.settings.SettingsActivity
import io.multimoon.colorful.CAppCompatActivity
import io.multimoon.colorful.Colorful
import io.multimoon.colorful.ThemeColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

val ioCoroutine = CoroutineScope(Dispatchers.IO)
val uiCoroutine = CoroutineScope(Dispatchers.Main)

class MainActivity : AppCompatActivity() {
    lateinit var cardViewCount: CardView
    lateinit var mViewPager: ViewPager
    lateinit var musicFragment: MusicFragment
    private lateinit var actionBar: ActionBar
    private lateinit var filesFragment: FilesFragment
    private lateinit var appsFragment: AppsFragment
    private lateinit var historyFragment: HistoryFragment
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var hotspotManager: HotspotManager
    private lateinit var myWifiManager: MyWifiManager
    private lateinit var mainPagerAdapter: MainPagerAdapter
    private lateinit var rootFragment: RootFragment
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var isWakeLockStarted = false
    private lateinit var hider: Hider
    private lateinit var photosFragment: PhotosFragment
    private lateinit var navigationView: NavigationView
    private lateinit var videosFragment: VideosFragment

    val activityResultListeners = mutableMapOf<Int, (Int, Int, Intent?) -> Unit>()
    val permissionResultListeners = mutableMapOf<Int, (Boolean) -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nav_layout_navigation_drawer)

        changeTheme()
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            actionBar = supportActionBar as ActionBar
        }
        actionBar.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_xportal)
        }
        initiate()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionResultListeners[requestCode]?.let {
            if (grantResults.find {
                        it != PackageManager.PERMISSION_GRANTED
                    } != null) it(false)
            else
                it(true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activityResultListeners[requestCode]?.invoke(requestCode, resultCode, data)
    }

    fun onClickOfCardButtons(v: View) {
        when (v.id) {
            R.id.cardViewAppsView -> mViewPager.currentItem = 1
            R.id.cardViewMusicView -> mViewPager.currentItem = 2
            R.id.cardViewVideoView -> mViewPager.currentItem = 3
            R.id.cardViewPhotosView -> mViewPager.currentItem = 4
            R.id.cardViewFilesView -> mViewPager.currentItem = 5
            R.id.cardViewHistoryView -> mViewPager.currentItem = 6
        }
    }

    private fun initiate() {
        Logger.log("INITIATING " + System.currentTimeMillis(), this)
        hider = Hider.getHider(this@MainActivity)

        powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, packageName)

        drawerLayout = findViewById(R.id.drawer_layout)
        mainPagerAdapter = MainPagerAdapter(supportFragmentManager)

        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
                override fun onDrawerSlide(view: View, v: Float) {

                }

                override fun onDrawerOpened(view: View) {

                }

                override fun onDrawerClosed(view: View) {
                    when (menuItem.itemId) {
                        R.id.nav_xhider -> startActivity(
                            Intent(
                                this@MainActivity,
                                HiderActivity::class.java
                            )
                        )
                        R.id.nav_settings -> startActivity(
                            Intent(
                                this@MainActivity,
                                SettingsActivity::class.java
                            )
                        )
                    }
                    drawerLayout.removeDrawerListener(this)
                }

                override fun onDrawerStateChanged(i: Int) {

                }
            })
            drawerLayout.closeDrawers()
            true
        }

        Thread {
            myWifiManager = MyWifiManager.getMyWifiManager(this@MainActivity)
            hotspotManager = HotspotManager.getHotspotManager(this@MainActivity)
        }.start()

        cardViewCount = findViewById(R.id.cardViewCount)
        cardViewCount.setOnClickListener { }
        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.fragments_container)
        mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                cardViewCount.visibility = View.GONE
                when (position) {
                    0 -> cardViewCount.visibility = View.GONE
                    1 -> {
                        appsFragment.updateCardView(cardViewCount)
                    }
                    2 -> {
                        musicFragment.updateCardView(cardViewCount)
                    }
                    3 -> {
                        videosFragment.updateCardView(cardViewCount)
                    }
                    4 -> photosFragment.updateCardView(cardViewCount)
                    5 -> {
                        filesFragment.updateCardView(cardViewCount)
                    }
                    6 -> {
                        historyFragment.updateCardView(cardViewCount)
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        val tabLayout = findViewById<TabLayout>(R.id.mainTabLayout)
        tabLayout.setupWithViewPager(mViewPager)
        mViewPager.adapter = mainPagerAdapter
        mViewPager.offscreenPageLimit = 7
        Logger.log("initialize stopped " + System.currentTimeMillis(), this)
    }

    private fun changeTheme() {
        val sharedPreferences = getSharedPreferences(AppStyle.PREFERENCE_NAME, Context.MODE_PRIVATE)
        val currentColor = sharedPreferences.getString(AppStyle.THEME_COLOR_KEY, "")
        if (currentColor == null || currentColor == "") {
            Colorful().edit()
                .setPrimaryColor(ThemeColor.GREY)
                .setAccentColor(ThemeColor.PINK)
                .apply(this) {

                }

            return
        }

        when (currentColor) {
            AppStyle.ColorValues.DEEP_ORANGE -> {
                AppStyle.currentThemeColor = AppStyle.Colors.DEEP_ORANGE
                AppStyle.currentTheme = ThemeColor.DEEP_ORANGE
            }
            AppStyle.ColorValues.GREY -> {
                AppStyle.currentThemeColor = AppStyle.Colors.GREY
                AppStyle.currentTheme = ThemeColor.GREY
            }
            AppStyle.ColorValues.GREEN -> {
                AppStyle.currentThemeColor = AppStyle.Colors.GREEN
                AppStyle.currentTheme = ThemeColor.GREEN
            }
            AppStyle.ColorValues.BLACK -> {
                AppStyle.currentThemeColor = AppStyle.Colors.BLACK
                AppStyle.currentTheme = ThemeColor.BLACK
            }
            AppStyle.ColorValues.LIGHT_BLUE -> {
                AppStyle.currentThemeColor = AppStyle.Colors.LIGHT_BLUE
                AppStyle.currentTheme = ThemeColor.LIGHT_BLUE
            }
            AppStyle.ColorValues.PINK -> {
                AppStyle.currentThemeColor = AppStyle.Colors.PINK
                AppStyle.currentTheme = ThemeColor.PINK
            }
            else -> {
                AppStyle.currentThemeColor = AppStyle.Colors.GREY
                AppStyle.currentTheme = ThemeColor.GREY
            }
        }

        var colorAccent = ThemeColor.PINK
        if (AppStyle.currentTheme == ThemeColor.PINK)
            colorAccent = ThemeColor.LIGHT_BLUE

        Colorful().edit()
            .setPrimaryColor(AppStyle.currentTheme)
            .setAccentColor(colorAccent)
            .apply(this) {

            }
    }

    @SuppressLint("WakelockTimeout")
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    private fun startWakeLock() {
        if (!isWakeLockStarted)
            wakeLock.acquire()
        isWakeLockStarted = true
    }

    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    private fun stopWakeLock() {
        if (isWakeLockStarted)
            wakeLock.release()
        isWakeLockStarted = false
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        drawerLayout.openDrawer(GravityCompat.START)
        return true
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK
        )
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.REQUEST_DELETE_PACKAGES,
                    Manifest.permission.REQUEST_INSTALL_PACKAGES
                ),
                1452
            )
        }
    }

    fun connectionSuccess(deviceName: String) {
        mViewPager.currentItem = 1
        isConnected = true
        rootFragment.connectionSuccess(deviceName)
        startWakeLock()
    }

    fun connectionTerminated() {
        runOnUiThread {
            stopWakeLock()
            isConnected = false
            isSender = false
            mViewPager.currentItem = 0
            rootFragment.connectionTerminated()
        }
    }


    private fun terminateConnection() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Disconnect")
        builder.setMessage("Are you want to disconnect?")
        builder.setPositiveButton("Disconnect") { dialog, _ ->
            if (isSender) {
                connectionTerminated()
                val sender = TransferHotspot.getSender(this@MainActivity, false)
                sender?.terminateConnection()
                hotspotManager.communication.terminateConnection()
                hotspotManager.terminateConnection()
                rootFragment.connectionTerminated()
                isConnected = false
                isSender = false
            } else {
                connectionTerminated()
                TransferWifi.getSender(this@MainActivity).terminateConnection()
                myWifiManager.communication.terminateConnection()
                myWifiManager.terminateConnection()
                rootFragment.connectionTerminated()
                isConnected = false
                isSender = false
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()


    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        when (requestCode) {
//            PERMISSIONS_REQUEST_CODE -> {
//                var allGranted = true
//                for (permission in permissions) {
//                    if (ActivityCompat.checkSelfPermission(
//                            this,
//                            permission
//                        ) != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        allGranted = false
//                        val snackbar = Snackbar.make(
//                            Objects.requireNonNull(window.decorView),
//                            "Application won't work without $permission",
//                            Snackbar.LENGTH_LONG
//                        )
//                        snackbar.setActionTextColor(Color.rgb(255, 165, 0))
//                        snackbar.setAction("GRANT") { requestPermissions() }
//                        snackbar.show()
//                        break
//                    }
//                }
//
//                if (allGranted)
//                    initiate()
//            }
//
//            1452 -> for (permission in permissions) {
//                if (ActivityCompat.checkSelfPermission(
//                        this,
//                        permission
//                    ) != PackageManager.PERMISSION_GRANTED
//                ) {
//                    val snackbar = Snackbar.make(
//                        Objects.requireNonNull<View>(filesFragment.view),
//                        "Application won't work without $permission",
//                        Snackbar.LENGTH_LONG
//                    )
//                    snackbar.setActionTextColor(Color.rgb(255, 165, 0))
//                    snackbar.setAction("GRANT") { requestPermissions() }
//                    snackbar.show()
//                    break
//                }
//            }
//        }
//    }


//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//
//        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
//        if (result != null) {
//            myWifiManager.resultGenerated(result.contents)
//        }
//    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers()
            return
        }
        if (mViewPager.currentItem == 5) {
            if (filesFragment.onBackPressed()) {
                if (mViewPager.currentItem != 0) {
                    mViewPager.currentItem = 0
                    return
                }
                if (isConnected) {
                    terminateConnection()
                } else {
                    stopWakeLock()
                    super.onBackPressed()
                }
            }
            return
        }

        if (mViewPager.currentItem != 0) {
            mViewPager.currentItem = 0
            return
        }
        if (isConnected) {
            terminateConnection()
        } else
            super.onBackPressed()

    }


    fun onButtonUploadClick() {
        hotspotManager.startHotspot()
        isSender = true
    }

    override fun onDestroy() {
        stopWakeLock()
        super.onDestroy()
    }

    fun onButtonDownloadClick() {
        myWifiManager.startWifi()
        isSender = false
        isConnected = false
    }


    fun disconnect() {
        terminateConnection()
    }


    class PlaceholderFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout._fragment_temp, container, false)
        }
    }

    inner class MainPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            println("Request for position $position")
            when (position) {
                0 -> {
                    Logger.log("RootFragment " + System.currentTimeMillis(), this)
                    rootFragment = RootFragment()
                    return rootFragment
                }
                1 -> {
                    appsFragment = AppsFragment()
                    Logger.log("AppsFragment " + System.currentTimeMillis(), this)
                    return appsFragment
                }
                2 -> {
                    Logger.log("MusicFragment " + System.currentTimeMillis(), this)
                    musicFragment = MusicFragment()
                    return musicFragment
                }
                3 -> {
                    Logger.log("VideosFragment " + System.currentTimeMillis(), this)
                    videosFragment = VideosFragment()
                    return videosFragment
                }

                4 -> {
                    Logger.log("PhotosFragment " + System.currentTimeMillis(), this)
                    photosFragment = PhotosFragment()
                    return photosFragment
                }
                5 -> {
                    Logger.log("FilesFragment " + System.currentTimeMillis(), this)
                    filesFragment = FilesFragment()
                    return filesFragment
                }
                6 -> {
                    Logger.log("HistoryFragment " + System.currentTimeMillis(), this)

                    historyFragment = HistoryFragment()
                    Logger.log("HistoryFragment DONE " + System.currentTimeMillis(), this)
                    return historyFragment
                }
                else -> return PlaceholderFragment()
            }
        }


        override fun getCount(): Int {
            return 7
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "Connection"
                1 -> return "Apps"
                2 -> return "Music"
                3 -> return "Videos"
                4 -> return "Photos"
                5 -> return "Files"
                6 -> return "History"
            }
            return ""
        }
    }

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 1234
        const val TAG = "MSUBCA"
        var handler = Handler(Looper.getMainLooper())
        var isSender = false //Means it is a Hotspot device
        var isConnected = false
    }
}