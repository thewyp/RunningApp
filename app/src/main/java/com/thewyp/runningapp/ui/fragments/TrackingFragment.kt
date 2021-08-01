package com.thewyp.runningapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.model.CameraPosition
import com.amap.api.maps2d.model.PolylineOptions
import com.thewyp.runningapp.R
import com.thewyp.runningapp.databinding.FragmentTrackingBinding
import com.thewyp.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.thewyp.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.thewyp.runningapp.other.Constants.POLYLINE_COLOR
import com.thewyp.runningapp.other.Constants.POLYLINE_WIDTH
import com.thewyp.runningapp.services.Polyline
import com.thewyp.runningapp.services.TrackingService
import com.thewyp.runningapp.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!

    private var mMap: AMap? = null

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTrackingBinding.bind(view)
        binding.mapView.onCreate(savedInstanceState)
        mMap = binding.mapView.map
        addAllPolylines()
        binding.btnToggleRun.setOnClickListener {
            toggleRun()
        }
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, {
            updateTracking(it)
        })
        TrackingService.pathPoints.observe(viewLifecycleOwner, {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })
    }

    private fun addAllPolylines() {
        for (polyline in pathPoints) {
            mMap?.addPolyline(
                PolylineOptions().apply {
                    addAll(polyline)
                    width(POLYLINE_WIDTH)
                    color(POLYLINE_COLOR)
                }
            )
        }
    }

    private fun addLatestPolyline() {
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            mMap?.addPolyline(
                PolylineOptions().apply {
                    add(preLastLatLng)
                    add(lastLatLng)
                    width(POLYLINE_WIDTH)
                    color(POLYLINE_COLOR)
                }
            )
        }
    }

    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            mMap?.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition(pathPoints.last().last(), 100f, 30f, 0f)
                )
            )
        }
    }

    private fun toggleRun() {
        if (isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking) {
            binding.btnToggleRun.text = "Start"
            binding.btnFinishRun.visibility = View.VISIBLE
        } else {
            binding.btnToggleRun.text = "Stop"
            binding.btnFinishRun.visibility = View.GONE
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
    }
}