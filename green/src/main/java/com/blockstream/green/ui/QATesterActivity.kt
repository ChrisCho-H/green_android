package com.blockstream.green.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.NetworkEvent
import com.blockstream.gdk.data.Notification
import com.blockstream.green.R
import com.blockstream.green.data.Countries
import com.blockstream.green.data.Country
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.QaTesterActivityBinding
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.ui.items.CountryListItem
import com.blockstream.green.ui.items.NetworkListItem
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.QTNotificationDelay
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.openKeyboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class QATesterActivity : AppCompatActivity(), FilterableDataProvider {

    private lateinit var binding: QaTesterActivityBinding

    @Inject
    lateinit var qaTester: QATester

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var greenWallet: GreenWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent opening from adb
        if(!isDevelopmentFlavor()){
            finish()
        }

        binding = QaTesterActivityBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.also {
            // Prevent replacing title from NavController
            it.setDisplayShowTitleEnabled(false)
        }

        binding.toolbar.title = "QA Tester"

        binding.qaTester = qaTester

        binding.buttonContinue.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.buttonKill.setOnClickListener {
            exitProcess(0)
        }

        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonDisconnectAll.setOnClickListener {
            for(session in sessionManager.getConnectedSessions().values){
                session.disconnectAsync()
            }

            Snackbar.make(binding.coordinator, "All sessions was disconnected", Snackbar.LENGTH_SHORT).show()
        }

        binding.buttonRequireLoginNotification.setOnClickListener {
            qaTester.notificationsEvents.onNext(QTNotificationDelay(Notification(event = "network", network = NetworkEvent(true, loginRequired = true, waiting = 0))))
        }

        binding.buttonDisconnectNotification.setOnClickListener {
            qaTester.notificationsEvents.onNext(QTNotificationDelay(Notification(event = "network", network = NetworkEvent(false, waiting = 7, loginRequired = false))))
            qaTester.notificationsEvents.onNext(QTNotificationDelay(Notification(event = "network", network = NetworkEvent(true, loginRequired = false, waiting = 0)), delay = 10))
        }

        binding.buttonCreateCustomNetwork.setOnClickListener {
            FilterBottomSheetDialogFragment().also {
                it.show(supportFragmentManager, it.toString())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getModelAdapter(): ModelAdapter<*, *> {
        val adapter = ModelAdapter<Network, NetworkListItem>() {
            NetworkListItem(it.id, it.name, "")
        }.set(greenWallet.networks.networks.values.toList())

        adapter.itemFilter.filterPredicate = { item: NetworkListItem, constraint: CharSequence? ->
            item.networkName.lowercase().contains(
                constraint.toString().lowercase()
            )
        }

        return adapter
    }

    override fun filteredItemClicked(item: GenericItem, position: Int) {
        val network = (item as NetworkListItem).network

        val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(this))
        dialogBinding.hint = "Hostname"
        dialogBinding.textInputLayout.helperText = "host:port"

        MaterialAlertDialogBuilder(this)
            .setTitle("Network hostname")
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                greenWallet.registerCustomNetwork(network, dialogBinding.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

    }
}