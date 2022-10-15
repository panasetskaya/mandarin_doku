package com.panasetskaia.charactersudoku.presentation.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.panasetskaia.charactersudoku.R
import com.panasetskaia.charactersudoku.databinding.FragmentSingleCharacterBinding
import com.panasetskaia.charactersudoku.domain.entities.ChineseCharacter
import com.panasetskaia.charactersudoku.presentation.MainActivity
import com.panasetskaia.charactersudoku.presentation.viewmodels.ChineseCharacterViewModel

class SingleCharacterFragment : Fragment() {

    private lateinit var viewModel: ChineseCharacterViewModel
    private var screenMode = SCREEN_MODE_DEFAULT
    private lateinit var chineseCharacter: ChineseCharacter

    private var _binding: FragmentSingleCharacterBinding? = null
    private val binding: FragmentSingleCharacterBinding
        get() = _binding ?: throw RuntimeException("FragmentSingleCharacterBinding is null")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseParams()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSingleCharacterBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).setSupportActionBar(binding.appBar)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = (activity as MainActivity).characterViewModel
        setupMenu()
        when (screenMode) {
            MODE_EDIT -> launchEditMode()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun parseParams() {
        val args = requireArguments()
        if (!args.containsKey(EXTRA_SCREEN_MODE)) {
            throw RuntimeException("No screen mode param")
        }
        val mode = args.getString(EXTRA_SCREEN_MODE)
        if (mode != MODE_EDIT && mode != MODE_ADD) {
            throw RuntimeException("Unknown param: screen mode $mode")
        }
        screenMode = mode
        if (screenMode == MODE_EDIT) {
            if (!args.containsKey(EXTRA_CHINESE)) {
                throw RuntimeException("No param: ChineseCharacter")
            }
            chineseCharacter = args.getParcelable(EXTRA_CHINESE)!!
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.single_character_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.add_icon -> {
                        val chineseChar = binding.etCharacter.text.toString()
                        if (chineseChar.length == 1) {
                            val pinyin = binding.etPinyin.text.toString()
                            val translation = binding.etTranslation.text.toString()
                            val usages = binding.etUsages.text.toString()
                            val id = if (screenMode == MODE_ADD) 0 else chineseCharacter.id
                            val newChar =
                                ChineseCharacter(id, chineseChar, pinyin, translation, usages)
                            viewModel.addOrEditCharacter(newChar)
                            Toast.makeText(context, R.string.added, Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                            replaceWithThisFragment(DictionaryFragment::class.java)
                        } else if (chineseChar.length < MIN_LENGTH) {
                            Toast.makeText(context, R.string.no_char, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, R.string.too_many, Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    else -> true
                }
            }
        }, viewLifecycleOwner)
    }

    private fun launchEditMode() {
        with(binding) {
            etCharacter.setText(chineseCharacter.character)
            etPinyin.setText(chineseCharacter.pinyin)
            etTranslation.setText(chineseCharacter.translation)
            etUsages.setText(chineseCharacter.usages)
        }
    }

    private fun replaceWithThisFragment(fragment: Class<out Fragment>) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fcvMain, fragment, null)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        const val EXTRA_SCREEN_MODE = "extra_mode"
        const val EXTRA_CHINESE = "extra_chinese"
        const val MODE_EDIT = "mode_edit"
        const val MODE_ADD = "mode_add"
        private const val SCREEN_MODE_DEFAULT = ""
        private const val MIN_LENGTH = 1
    }
}