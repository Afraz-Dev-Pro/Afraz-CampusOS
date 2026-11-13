package com.afraz.campusos.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface CampusRoute {
    @Serializable data object Dashboard : CampusRoute
    @Serializable data object Academics : CampusRoute
    @Serializable data object Toolkit : CampusRoute
    @Serializable data object HackHub : CampusRoute
    @Serializable data object Finance : CampusRoute
    @Serializable data object DigitalId : CampusRoute
}
