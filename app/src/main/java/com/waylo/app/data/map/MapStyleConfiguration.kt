package com.waylo.app.data.map

object MapStyleConfiguration {

    /**
     * OpenFreeMap public style served over HTTPS. No API key, no registration and no
     * documented request limits; the service is donation funded. Requires OSM and
     * OpenMapTiles attribution (rendered automatically by the MapLibre UI) and the map
     * is online only - tiles are never cached to disk by this app.
     */
    const val OPENFREEMAP_DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"

    fun defaultStyleUri(): String = OPENFREEMAP_DARK_STYLE
}
