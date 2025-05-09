package com.example.roznamcha.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager

class LauncherFragment : Fragment() {

    private val TAG = "LauncherFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_launcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            context?.let { safeContext ->
                if (SettingsManager.isPasswordSet(safeContext)) {
                    // User is set up, go to PIN lock
                    findNavController().navigate(R.id.action_launcherFragment_to_passwordLockFragment)
                } else if (!SettingsManager.hasAcceptedDisclaimer(safeContext)) {
                    // User has NOT accepted the disclaimer, this is the first screen they must see
                    findNavController().navigate(R.id.action_launcherFragment_to_disclaimerFragment)
                } else {
                    // User has accepted disclaimer but maybe quit during setup.
                    // Send them to the next step.
                    findNavController().navigate(R.id.action_launcherFragment_to_currencySelectionFragment)
                }
            }
        }, 500)
    }
}