package com.panasetskaia.charactersudoku.presentation.settings_screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.panasetskaia.charactersudoku.R
import com.panasetskaia.charactersudoku.databinding.FragmentExportBinding
import com.panasetskaia.charactersudoku.domain.entities.ChineseCharacter
import com.panasetskaia.charactersudoku.presentation.MainActivity
import com.panasetskaia.charactersudoku.presentation.base.BaseFragment
import com.panasetskaia.charactersudoku.presentation.dict_screen.ChineseCharacterViewModel
import com.panasetskaia.charactersudoku.presentation.viewmodels.ViewModelFactory
import com.panasetskaia.charactersudoku.utils.getAppComponent
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject


class ExportFragment : BaseFragment<FragmentExportBinding, ChineseCharacterViewModel>(FragmentExportBinding::inflate) {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    override val viewModel by viewModels<ChineseCharacterViewModel> { viewModelFactory }

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getAppComponent().inject(this)
    }

    override fun onReady(savedInstanceState: Bundle?) {
        setupMenu()
        setupListeners()
        setupResultLauncher()
    }

    private fun setupResultLauncher() {
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.also {
                    val jsonString = readFileContent(it)
                    jsonString?.let {
                        viewModel.parseExternalDict(jsonString)
                        Toast.makeText(requireContext(), R.string.new_dict_added, Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(requireContext(), R.string.invalid_dict, Toast.LENGTH_SHORT).show()

                }
            }
        }
    }

    private fun setupListeners() {
        binding.toCsvButton.setOnClickListener {
            viewModel.saveDictionaryToCSV()
            observeFilePath()
        }
        binding.toJsonButton.setOnClickListener {
            viewModel.saveDictionaryToJson()
            observeFilePath()

        }
        binding.fromJsonButton.setOnClickListener {
            openFile()
        }
    }

    private fun setupMenu() {
        binding.appBar.setNavigationOnClickListener {
            viewModel.navigateBack()
        }
    }

    private fun observeFilePath() {
        viewModel.pathLiveData.observe(viewLifecycleOwner) {
            if (it!="") {
                startFileShareIntent(it)
            }
        }
    }

    private fun startFileShareIntent(filePath: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Sharing file from Mandarindoku"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Sharing file from Mandarindoku"
            )
            val fileURI = FileProvider.getUriForFile(
                requireContext(), requireActivity().application.packageName + ".provider",
                File(filePath)
            )
            putExtra(Intent.EXTRA_STREAM, fileURI)
        }
        startActivity(shareIntent)
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        resultLauncher.launch(intent)
    }

    private fun readFileContent(uri: Uri): List<ChineseCharacter>? {
        try {
            val contentResolver = requireActivity().contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(
                InputStreamReader(
                    inputStream)
            )
            val typeToken = object : TypeToken<List<ChineseCharacter>>() {}.type
            val gson = GsonBuilder()
                .setLenient()
                .create()
            val result = gson.fromJson<List<ChineseCharacter>>(reader, typeToken)
            inputStream?.close()
            return result
        } catch (e: Exception) {
            return null
        }
    }
}