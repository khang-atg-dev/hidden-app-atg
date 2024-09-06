package moe.shizuku.manager.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.R
import moe.shizuku.manager.app.BaseFragment
import moe.shizuku.manager.databinding.AccountFragmentBinding

class AccountFragment: BaseFragment() {
    private lateinit var binding: AccountFragmentBinding

    override fun getTitle(context: Context): String = context.getString(R.string.account)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AccountFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}