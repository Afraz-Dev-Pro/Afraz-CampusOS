package com.afraz.campusos.feature_toolkit.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.afraz.campusos.MainActivity

class QuickScanTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = "Scan Notes"
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val launch = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN_SCANNER, true)
        startActivityAndCollapse(launch)
    }

    companion object { const val EXTRA_OPEN_SCANNER = "com.afraz.campusos.OPEN_SCANNER" }
}
