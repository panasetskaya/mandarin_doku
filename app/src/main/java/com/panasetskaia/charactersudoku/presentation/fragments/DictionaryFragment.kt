package com.panasetskaia.charactersudoku.presentation.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.panasetskaia.charactersudoku.R
import com.panasetskaia.charactersudoku.databinding.FragmentDictionaryBinding
import com.panasetskaia.charactersudoku.presentation.MainActivity
import com.panasetskaia.charactersudoku.presentation.adapters.DictionaryListAdapter
import com.panasetskaia.charactersudoku.presentation.adapters.MyItemTouchCallback
import com.panasetskaia.charactersudoku.presentation.fragments.dialogFragments.ConfirmStartGameFragment
import com.panasetskaia.charactersudoku.presentation.viewmodels.ChineseCharacterViewModel
import com.panasetskaia.charactersudoku.presentation.viewmodels.GameViewModel

class DictionaryFragment : Fragment() {

    private lateinit var characterViewModel: ChineseCharacterViewModel
    private lateinit var gameViewModel: GameViewModel
    private lateinit var listAdapter: DictionaryListAdapter
    private lateinit var itemTouchCallback: MyItemTouchCallback

    private val linearInterpolator = LinearInterpolator()

    private var _binding: FragmentDictionaryBinding? = null
    private val binding: FragmentDictionaryBinding
        get() = _binding ?: throw RuntimeException("FragmentDictionaryBinding is null")

    private var isStartDialogHidden = true

    private var shouldGameStart = false



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDictionaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        characterViewModel = (activity as MainActivity).characterViewModel
        gameViewModel = (activity as MainActivity).gameViewModel
        setupMenu()
        setupFab()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        gameViewModel.shouldGameStartLiveData.observe(viewLifecycleOwner) { should ->
            shouldGameStart = should
        }
        if (shouldGameStart) {
            parentFragmentManager.popBackStack()
            val fragment = GameFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fcvMain, fragment)
                .addToBackStack(null)
                .commit()

        }
    }

    private fun setupRecyclerView() {
        listAdapter = DictionaryListAdapter()
        itemTouchCallback = object : MyItemTouchCallback(this, listAdapter, characterViewModel) {}
        listAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        with(binding.recyclerViewList) {
            adapter = listAdapter
            recycledViewPool.setMaxRecycledViews(
                DictionaryListAdapter.CHOSEN,
                DictionaryListAdapter.MAX_POOL_SIZE
            )
            recycledViewPool.setMaxRecycledViews(
                DictionaryListAdapter.NOT_CHOSEN,
                DictionaryListAdapter.MAX_POOL_SIZE
            )
            listAdapter.onCharacterItemLongClickListener = {
                characterViewModel.changeIsChosenState(it)
            }
            listAdapter.onCharacterItemClickListener = {
                val fragment = SingleCharacterFragment.newInstanceEditCharacter(it)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fcvMain, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            setupSwipeListener(binding.recyclerViewList)
        }
        characterViewModel.dictionaryLiveData.observe(viewLifecycleOwner) {
            listAdapter.submitList(it)
        }
        characterViewModel.isDialogHiddenLiveData.observe(viewLifecycleOwner) { isDialogHidden ->
            isStartDialogHidden = isDialogHidden
        }
        characterViewModel.selectedCharactersLiveData.observe(viewLifecycleOwner) { selected ->
            if (selected.size==9 && isStartDialogHidden) {
                characterViewModel.finishDialog(false)
                val fragment = ConfirmStartGameFragment.newInstance()
                parentFragmentManager.beginTransaction()
                    .add(R.id.dictionaryContainerView, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun setupSwipeListener(rv: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(rv)
    }

    private fun setupFab() {
        requireActivity().runOnUiThread {
            AnimatorSet().apply {
                play(shakeAnimator(binding.fabAdd, "rotation"))
                start()
            }
        }
        binding.fabAdd.setOnClickListener {
            val fragment = SingleCharacterFragment.newInstanceAddCharacter()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fcvMain, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.dict_toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.search_icon -> {
                        Toast.makeText(context, "Will search", Toast.LENGTH_SHORT).show() //todo
                        true
                    }
                    R.id.sudoku_icon -> {
                        parentFragmentManager.popBackStack()
                        val fragment = GameFragment.newInstance()
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fcvMain, fragment)
                            .addToBackStack(null)
                            .commit()
                        true
                    }
                    R.id.dict_help_icon -> {
                        Toast.makeText(context, "Will go to Help", Toast.LENGTH_SHORT).show() //todo
                        true
                    }
                    else -> true
                }
            }
        }, viewLifecycleOwner)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun shakeAnimator(shake: View, propertyName: String) =
        ObjectAnimator.ofFloat(shake, propertyName, -180f, 90f).apply {
            repeatMode = ValueAnimator.RESTART
            repeatCount = 1
            duration = 400
            interpolator = linearInterpolator
        }


    companion object {
        fun newInstance() = DictionaryFragment()
    }

}

//todo: почему возникает заново ConfrimStartGameFragment? Почему не успевает обновиться?