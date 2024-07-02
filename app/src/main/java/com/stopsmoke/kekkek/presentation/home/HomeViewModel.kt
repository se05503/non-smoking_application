package com.stopsmoke.kekkek.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stopsmoke.kekkek.domain.model.HistoryTime
import com.stopsmoke.kekkek.domain.model.Post
import com.stopsmoke.kekkek.domain.model.User
import com.stopsmoke.kekkek.domain.model.UserConfig
import com.stopsmoke.kekkek.domain.model.getStartTimerState
import com.stopsmoke.kekkek.domain.model.getTotalMinutesTime
import com.stopsmoke.kekkek.domain.repository.PostRepository
import com.stopsmoke.kekkek.domain.repository.UserRepository
import com.stopsmoke.kekkek.presentation.home.rankingList.RankingListItem
import com.stopsmoke.kekkek.presentation.home.rankingList.toRankingListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {
    private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState.NormalUiState.init())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var timeString: String = ""
    private var savedMoneyPerMinute: Double = 0.0
    private var savedLifePerMinute: Double = 0.0

    private var _currentUserState = MutableStateFlow<User>(User.Guest)
    val currentUserState = _currentUserState.asStateFlow()

    val user = userRepository.getUserData().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private val _noticeBanner = MutableStateFlow(Post.emptyPost())
    val noticeBanner: StateFlow<Post> get() = _noticeBanner.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val noticeBannerPost = postRepository.getTopNotice()
                _noticeBanner.emit(noticeBannerPost)
            }catch (e:Exception){
                _uiState.emit(HomeUiState.ErrorExit)
            }
        }
    }

    fun updateUserData() = viewModelScope.launch {
        try {
            val userData = userRepository.getUserData()
            userData.collect { user ->
                _currentUserState.value = user
                when (user) {
                    is User.Registered -> {

                        if (user.history.historyTimeList.isEmpty()) {
                            setEmptyStartUserHistory()
                        }

                        val totalMinutesTime = user.history.getTotalMinutesTime()
                        timeString = formatElapsedTime(totalMinutesTime)
                        calculateSavedValues(user.userConfig)
                        _uiState.emit(
                            HomeUiState.NormalUiState(
                                homeItem = HomeItem(
                                    timeString = timeString,
                                    savedMoney = savedMoneyPerMinute * totalMinutesTime,
                                    savedLife = savedLifePerMinute * totalMinutesTime,
                                    rank = user.ranking,
                                    addictionDegree = user.cigaretteAddictionTestResult ?: "테스트 필요",
                                    history = user.history
                                ),
                                startTimerSate = user.history.getStartTimerState()
                            )
                        )
                    }

                    else -> {}
                }
            }
        } catch (e: Exception){
            _uiState.emit(HomeUiState.ErrorExit)
        }
    }


    fun startTimer() {
        timerJob?.cancel()
        (uiState.value as? HomeUiState.NormalUiState).let{
            timerJob = viewModelScope.launch {
                while (true) {
                    timeString =
                        formatElapsedTime(
                            (currentUserState.value as? User.Registered)?.history?.getTotalMinutesTime()
                                ?: 0
                        )
                    _uiState.update { prev ->
                        (prev as HomeUiState.NormalUiState).copy(
                            homeItem = prev.homeItem.copy(
                                timeString = timeString
                            )
                        )
                    }
                    delay(1000) // 1 second
                }
            }
        }
    }

    fun stopTimer() = viewModelScope.launch {
        timerJob?.cancel()
        timerJob = null
    }

    fun setStopUserHistory() = viewModelScope.launch {
        try {
            if (currentUserState.value is User.Registered) {
                val user = (currentUserState.value as User.Registered)
                val updatedHistoryTimeList = user.history.historyTimeList.toMutableList()
                val lastItem = updatedHistoryTimeList.last().copy(
                    quitSmokingStopDateTime = LocalDateTime.now()
                )

                updatedHistoryTimeList[updatedHistoryTimeList.size - 1] = lastItem

                val updatedUserHistory =
                    user.history.copy(
                        historyTimeList = updatedHistoryTimeList,
                        totalMinutesTime = timeStringToMinutes((uiState.value as HomeUiState.NormalUiState).homeItem.timeString)
                    )
                userRepository.setUserData(user.copy(history = updatedUserHistory))

                updateUserData()
            }
        }catch (e:Exception){
            _uiState.emit(HomeUiState.ErrorExit)
        }
    }

    fun setStartUserHistory() = viewModelScope.launch {
        try {
            if (currentUserState.value is User.Registered) {
                val user = currentUserState.value as User.Registered

                var updatedHistoryTimeList: MutableList<HistoryTime>? = null

                if (user.history.historyTimeList.isNotEmpty()) {
                    updatedHistoryTimeList = user.history.historyTimeList.toMutableList()
                } else updatedHistoryTimeList = mutableListOf()

                val lastItem = HistoryTime(
                    quitSmokingStartDateTime = LocalDateTime.now(),
                    quitSmokingStopDateTime = null
                )
                updatedHistoryTimeList.add(lastItem)

                val updatedUserHistory =
                    user.history.copy(historyTimeList = updatedHistoryTimeList)

                userRepository.setUserData(user.copy(history = updatedUserHistory))

                updateUserData()
            }
        }catch (e:Exception){
            _uiState.emit(HomeUiState.ErrorExit)
        }

    }


    private fun setEmptyStartUserHistory() = viewModelScope.launch {
        try {
            if (currentUserState.value is User.Registered) {
                val user = currentUserState.value as User.Registered

                var updatedHistoryTimeList: MutableList<HistoryTime> = mutableListOf()
                val lastItem = HistoryTime(
                    quitSmokingStartDateTime = LocalDateTime.now(),
                    quitSmokingStopDateTime = null
                )
                updatedHistoryTimeList.add(lastItem)

                val updatedUserHistory =
                    user.history.copy(historyTimeList = updatedHistoryTimeList)

                userRepository.setUserData(user.copy(history = updatedUserHistory))
            }
        }catch (e:Exception){
            _uiState.emit(HomeUiState.ErrorExit)
        }

    }

    private fun formatElapsedTime(elapsedTimeMinutes: Long): String {
        val hours = elapsedTimeMinutes / 60
        val minutes = elapsedTimeMinutes % 60
        return if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
    }

    fun timeStringToMinutes(timeString: String): Long {
        val parts = timeString.split("시간")
        val hours = if (parts.size > 1) parts[0].trim().toLong() else 0L
        val minutes = parts.last().replace("분", "").trim().toLong()
        return hours * 60 + minutes
    }

    private fun calculateSavedValues(userConfig: UserConfig) {
        // 하루에 피는 담배의 총 개수
        val totalCigarettesPerDay = userConfig.dailyCigarettesSmoked.toDouble()

        // 하루에 소비하는 갑의 개수
        val packsPerDay = totalCigarettesPerDay / userConfig.packCigaretteCount.toDouble()

        // 하루에 소비하는 갑의 비용
        val totalPackCostPerDay = packsPerDay * userConfig.packPrice.toDouble()

        // 하루에 소비하는 총 시간 (분 단위)
        val totalMinutesSmokedPerDay = 24 * 60.0 // 하루 전체 시간

        // 하루에 소비하는 담배 시간당 생명 절약량
        savedLifePerMinute = totalCigarettesPerDay / totalMinutesSmokedPerDay

        // 하루에 소비하는 갑의 시간당 비용
        savedMoneyPerMinute = totalPackCostPerDay / totalMinutesSmokedPerDay
    }


    private val _userRankingList = MutableStateFlow<List<RankingListItem>>(emptyList())
    val userRankingList get() = _userRankingList.asStateFlow()

    private val _myRank = MutableStateFlow<Int?>(null)
    val myRank get() = _myRank.asStateFlow()

    fun getAllUserData() = viewModelScope.launch {
        try {
            val list = userRepository.getAllUserData().map { user ->
                (user as User.Registered).toRankingListItem()
            }

            (user as? User.Registered)?.let {
                _myRank.value = list.indexOf(it.toRankingListItem())
            }

            _userRankingList.emit(list)
        }catch (e:Exception){
            _uiState.emit(HomeUiState.ErrorExit)
        }

    }

    fun getMyRank(){
        (user.value as? User.Registered)?.let{
            _myRank.value = userRankingList.value.indexOf(it.toRankingListItem())+1
        }
    }
}