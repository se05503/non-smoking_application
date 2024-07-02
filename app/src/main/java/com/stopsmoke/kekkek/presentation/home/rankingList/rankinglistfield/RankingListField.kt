package com.stopsmoke.kekkek.presentation.home.rankingList.rankinglistfield

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface RankingListField : Parcelable {
    @Parcelize
    data object Time : RankingListField

    @Parcelize
    data object Achievement : RankingListField
}
