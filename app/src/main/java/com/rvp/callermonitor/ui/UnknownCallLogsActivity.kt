package com.rvp.callermonitor.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rvp.callermonitor.viewmodel.CallViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.ItemTouchHelper
import com.rvp.callermonitor.model.UnknownCallLog
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.View
import android.widget.TextView
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.provider.BlockedNumberContract
import android.telecom.TelecomManager
import android.os.Build
import com.rvp.callermonitor.R
import com.google.android.material.appbar.MaterialToolbar
import android.util.Log
import kotlinx.coroutines.delay
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

/**
 * Activity to display unknown call logs.
 */
@AndroidEntryPoint
class UnknownCallLogsActivity : AppCompatActivity() {
    private val callViewModel: CallViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UnknownCallLogAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var allLogs: List<com.rvp.callermonitor.model.UnknownCallLog> = emptyList()

    private val restoreFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val input = contentResolver.openInputStream(uri)
                val json = input?.bufferedReader()?.use { it.readText() } ?: ""
                val type = object : TypeToken<List<com.rvp.callermonitor.model.UnknownCallLog>>() {}.type
                val logs: List<com.rvp.callermonitor.model.UnknownCallLog> = Gson().fromJson(json, type)
                lifecycleScope.launch {
                    logs.forEach { log ->
                        callViewModel.insertUnknownCallLog(log.phoneNumber, log.timestamp)
                    }
                }
                Snackbar.make(recyclerView, getString(R.string.toast_restore_complete), Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(recyclerView, getString(R.string.toast_restore_failed, e.message), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unknown_call_logs)

        Log.d("UnknownCallLogsActivity", "Activity created")

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        Log.d("UnknownCallLogsActivity", "Toolbar found: ${toolbar != null}")
        
        setSupportActionBar(toolbar)
        Log.d("UnknownCallLogsActivity", "SupportActionBar set: ${supportActionBar != null}")
        
        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        Log.d("UnknownCallLogsActivity", "Back button enabled: ${supportActionBar?.isShowing}")

        // Setup SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        Log.d("UnknownCallLogsActivity", "SwipeRefreshLayout found: ${swipeRefreshLayout != null}")
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("UnknownCallLogsActivity", "Pull to refresh triggered")
            importCallLogsAndRefresh()
        }

        recyclerView = findViewById(R.id.recyclerView)
        adapter = UnknownCallLogAdapter { log ->
            showLogDetailsBottomSheet(log)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        Log.d("UnknownCallLogsActivity", "RecyclerView setup complete")

        // Swipe to delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val log = adapter.currentList[viewHolder.adapterPosition]
                callViewModel.deleteUnknownCallLog(log.id)
                Snackbar.make(recyclerView, getString(R.string.toast_log_deleted), Snackbar.LENGTH_LONG).show()
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
        
        // FAB export functionality removed as requested

        lifecycleScope.launch {
            callViewModel.unknownCallLogs.collectLatest {
                Log.d("UnknownCallLogsActivity", "Received unknown call logs: ${it.size} items")
                allLogs = it
                
                // Add debugging for each item
                it.forEachIndexed { index, log ->
                    Log.d("UnknownCallLogsActivity", "Item $index: ${log.phoneNumber} at ${log.timestamp}")
                }
                
                adapter.submitList(it)
                
                if (it.isEmpty()) {
                    Log.d("UnknownCallLogsActivity", "No unknown call logs found")
                    // Show a message when no logs are available
                    Snackbar.make(recyclerView, getString(R.string.toast_no_unknown_logs), Snackbar.LENGTH_LONG).show()
                } else {
                    Log.d("UnknownCallLogsActivity", "Displaying ${it.size} unknown call logs")
                    // Hide any existing snackbar when data is available
                    Snackbar.make(recyclerView, getString(R.string.toast_logs_loaded, it.size), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        
        // Auto-import call logs on activity start
        lifecycleScope.launch {
            delay(1000) // Wait 1 second for activity to fully load
            Log.d("UnknownCallLogsActivity", "Starting auto-import of call logs")
            importCallLogsAndRefresh()
        }
    }
    
    private fun showLogDetailsBottomSheet(log: UnknownCallLog) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_log_details, null)
        val tvNumber = view.findViewById<TextView>(R.id.tvPhoneNumber)
        val tvTime = view.findViewById<TextView>(R.id.tvTimestamp)
        val btnCall = view.findViewById<View>(R.id.btnCall)
        val btnSms = view.findViewById<View>(R.id.btnSms)
        val btnBlock = view.findViewById<View>(R.id.btnBlock)
        tvNumber.text = log.phoneNumber
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        tvTime.text = sdf.format(java.util.Date(log.timestamp))
        btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${log.phoneNumber}"))
            startActivity(intent)
            dialog.dismiss()
        }
        btnSms.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${log.phoneNumber}"))
            startActivity(intent)
            dialog.dismiss()
        }
        btnBlock.setOnClickListener {
            if (isDefaultPhoneApp()) {
                // System-level block
                try {
                    val values = android.content.ContentValues().apply {
                        put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, log.phoneNumber)
                    }
                    contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
                    Snackbar.make(recyclerView, getString(R.string.toast_number_blocked_system), Snackbar.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Snackbar.make(recyclerView, getString(R.string.toast_block_failed, e.message), Snackbar.LENGTH_LONG).show()
                }
            } else {
                // Local block
                callViewModel.blockNumberLocally(log.phoneNumber)
                Snackbar.make(recyclerView, getString(R.string.toast_blocked_locally, getString(R.string.prompt_set_default_phone_app)), Snackbar.LENGTH_LONG).show()
                promptSetDefaultPhoneApp()
            }
            dialog.dismiss()
        }
        val tvSpam = view.findViewById<TextView>(R.id.tvSpamWarning)
        lifecycleScope.launch {
            if (callViewModel.isNumberSpam(log.phoneNumber)) {
                tvSpam.visibility = View.VISIBLE
            } else {
                tvSpam.visibility = View.GONE
            }
        }
        val btnReportSpam = view.findViewById<View>(R.id.btnReportSpam)
        btnReportSpam.setOnClickListener {
            callViewModel.reportNumberAsSpam(log.phoneNumber)
            Snackbar.make(recyclerView, getString(R.string.toast_number_reported_spam), Snackbar.LENGTH_LONG).show()
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun importCallLogsAndRefresh() {
        Log.d("UnknownCallLogsActivity", "Importing call logs and refreshing")
        
        lifecycleScope.launch {
            try {
                // Check permission first
                if (ActivityCompat.checkSelfPermission(this@UnknownCallLogsActivity, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                    Log.d("UnknownCallLogsActivity", "Call log permission granted, importing...")
                    val importedCount = callViewModel.importAllUnknownCallLogs()
                    Log.d("UnknownCallLogsActivity", "Auto-import completed. Imported $importedCount unknown call logs")
                    
                    if (importedCount > 0) {
                        Snackbar.make(recyclerView, getString(R.string.toast_auto_imported, importedCount), Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("UnknownCallLogsActivity", "Call log permission not granted")
                    Snackbar.make(recyclerView, getString(R.string.toast_call_log_permission_required), Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("UnknownCallLogsActivity", "Error during auto-import", e)
                Snackbar.make(recyclerView, getString(R.string.toast_import_failed, e.message), Snackbar.LENGTH_LONG).show()
            } finally {
                // Stop refresh animation
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Menu functionality removed as requested
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Log.d("UnknownCallLogsActivity", "Back button pressed")
                // Handle back button
                onBackPressed()
                true
            }
            else -> {
                Log.d("UnknownCallLogsActivity", "Unknown menu item: ${item.itemId}")
                super.onOptionsItemSelected(item)
            }
        }
    }
    
    private fun isDefaultPhoneApp(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = getSystemService(TelecomManager::class.java)
            val defaultDialer = telecomManager.defaultDialerPackage
            return packageName == defaultDialer
        }
        return false
    }
    private fun promptSetDefaultPhoneApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            startActivity(intent)
        }
    }
} 