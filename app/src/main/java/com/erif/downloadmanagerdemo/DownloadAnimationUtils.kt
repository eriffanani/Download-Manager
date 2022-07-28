package com.erif.downloadmanagerdemo

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.animation.doOnEnd
import com.google.android.material.progressindicator.CircularProgressIndicator

class DownloadAnimationUtils {

    companion object {

        fun hideButton(button: Button?, progressBar: CircularProgressIndicator?, txtPercentage: TextView?) {
            button?.let { getButton ->
                getButton.text = ""
                progressBar?.progress = 0
                txtPercentage?.text = "0%"
                val progressWidth = progressBar?.width ?: 0
                val decrease = (30f / 100f) * progressWidth
                val progressWidthFinal = progressWidth - decrease.toInt()
                val anim = ValueAnimator.ofInt(getButton.width, progressWidthFinal)
                anim.addUpdateListener {
                    val param = getButton.layoutParams
                    param.width = it.animatedValue as Int
                    param.height = getButton.height
                    getButton.layoutParams = param
                }
                anim.doOnEnd {
                    progressBar?.let { getProgress ->
                        val animProgress = ValueAnimator.ofFloat(0f, 1f)
                        animProgress.addUpdateListener {
                            getProgress.alpha = it.animatedValue as Float
                            txtPercentage?.alpha = it.animatedValue as Float
                        }
                        animProgress.duration = 150L
                        getProgress.visibility = View.VISIBLE
                        txtPercentage?.visibility = View.VISIBLE
                        animProgress.start()
                    }
                }
                anim.duration = 200L
                anim.start()

                val animAlpha = ValueAnimator.ofFloat(1f, 0f)
                animAlpha.addUpdateListener {
                    getButton.alpha = it.animatedValue as Float
                }
                animAlpha.doOnEnd {
                    getButton.visibility = View.INVISIBLE
                }
                animAlpha.duration = 150L
                Handler(Looper.getMainLooper()).postDelayed({
                    animAlpha.start()
                }, 50)

            }
        }

        fun showButton(button: Button?, width: Int, progressBar: CircularProgressIndicator?, txtPercentage: TextView?) {
            button?.let { getButton ->
                val animAlpha = ValueAnimator.ofFloat(0f, 1f)
                animAlpha.addUpdateListener {
                    getButton.alpha = it.animatedValue as Float
                }
                animAlpha.duration = 400L
                getButton.visibility = View.VISIBLE
                animAlpha.start()

                val anim = ValueAnimator.ofInt(0, width)
                anim.addUpdateListener {
                    val param = getButton.layoutParams
                    param.width = it.animatedValue as Int
                    param.height = getButton.height
                    getButton.layoutParams = param
                    getButton.requestLayout()
                }
                anim.doOnEnd {
                    getButton.text = "Download"
                }
                anim.duration = 300L
                anim.start()

                progressBar?.let { getProgress ->
                    val animProgress = ValueAnimator.ofFloat(1f, 0f)
                    animProgress.addUpdateListener {
                        getProgress.alpha = it.animatedValue as Float
                        txtPercentage?.alpha = it. animatedValue as Float
                    }
                    animProgress.doOnEnd {
                        getProgress.visibility = View.INVISIBLE
                        txtPercentage?.visibility = View.INVISIBLE
                    }
                    animProgress.duration = 250L
                    animProgress.start()
                }

            }
        }

    }

}