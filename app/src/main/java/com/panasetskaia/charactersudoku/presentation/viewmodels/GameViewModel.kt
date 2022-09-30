package com.panasetskaia.charactersudoku.presentation.viewmodels

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.panasetskaia.charactersudoku.R
import com.panasetskaia.charactersudoku.data.repository.CharacterSudokuRepositoryImpl
import com.panasetskaia.charactersudoku.domain.SUCCESS
import com.panasetskaia.charactersudoku.domain.entities.Board
import com.panasetskaia.charactersudoku.domain.usecases.GetNineRandomCharFromDictUseCase
import com.panasetskaia.charactersudoku.domain.usecases.GetResultUseCase
import kotlinx.coroutines.launch


class GameViewModel(application: Application) : AndroidViewModel(application) {

    val repository = CharacterSudokuRepositoryImpl()
    val getGameResult = GetResultUseCase(repository)
    val getNineRandomCharFromDict = GetNineRandomCharFromDictUseCase(repository)

    private var selectedRow = -1
    private var selectedCol = -1

    private val _selectedCellLiveData = MutableLiveData<Pair<Int, Int>>()
    val selectedCellLiveData: LiveData<Pair<Int, Int>>
        get() = _selectedCellLiveData

    private val _boardLiveData = MutableLiveData<Board>()
    val boardLiveData: LiveData<Board>
        get() = _boardLiveData

    private var _nineCharacters: List<String> = listOf()
    val nineCharacters: List<String>
        get() = _nineCharacters


    init {
        getNewGame()
        _selectedCellLiveData.postValue(Pair(selectedRow, selectedCol))
    }

    fun handleInput(number: Int) {
        if (selectedRow == -1 || selectedCol == -1) return
        val board = _boardLiveData.value
        board?.let {
            if (!it.getCell(selectedRow, selectedCol).isFixed) {
                val characterValue = nineCharacters[number]
                it.getCell(selectedRow, selectedCol).value = characterValue
                it.getCell(selectedRow, selectedCol).isDoubtful = false
                _boardLiveData.postValue(it)
            }
        }
        checkForSolution()
    }

    fun updateSelection(row: Int, col: Int) {
        selectedRow = row
        selectedCol = col
        _selectedCellLiveData.postValue(Pair(row, col))
    }

    fun markSelectedAsDoubtful() {
        val board = _boardLiveData.value
        board?.let {
            val isCellDoubtful = it.getCell(selectedRow, selectedCol).isDoubtful
            it.getCell(selectedRow, selectedCol).isDoubtful = !isCellDoubtful
            _boardLiveData.postValue(it)
        }
    }

    private fun checkForSolution() {
        val boardCells = boardLiveData.value?.cells
        var count = 0
        boardCells?.let { cellsList ->
            for (i in cellsList) {
                if (i.value == "0") {
                    count++
                }
            }
        }
        if (count < EMPTY_CELLS_MINIMUM) {
            viewModelScope.launch {
                boardLiveData.value?.let { board ->
                    val gameResult = getGameResult(board)
                    if (gameResult is SUCCESS) {
                        Toast.makeText(
                            getApplication(),
                            getApplication<Application>().getString(R.string.game_succesful),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        _boardLiveData.postValue(gameResult.solution)
                    } else {
                        Toast.makeText(
                            getApplication(),
                            getApplication<Application>().getString(R.string.check_again),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
    }

    private fun getNewGame() {
        viewModelScope.launch {
            _nineCharacters = getNineRandomCharFromDict()
            if (nineCharacters.size < 9) {
                Toast.makeText(
                    getApplication(),
                    getApplication<Application>().getString(R.string.dict_is_empty),
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                val translatedBoard = repository.getNewGameTestFun()
                _boardLiveData.postValue(translatedBoard)
            }
        }
    }

    companion object {
        internal const val EMPTY_CELLS_MINIMUM = 8
    }

    //todo: сохранение текущей Board в базу данных
}

